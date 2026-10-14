import 'dart:async';
import 'dart:convert';
import 'dart:math' as math;

import 'package:web_socket_channel/web_socket_channel.dart';

import 'stomp_frame.dart';

enum StompConnectionStatus { disconnected, connecting, connected, reconnecting }

typedef StompMessageHandler = void Function(StompFrame frame);
typedef StompTokenProvider = Future<String?> Function();
typedef WebSocketChannelFactory = WebSocketChannel Function(Uri uri);

class StompException implements Exception {
  final String message;
  const StompException(this.message);
  @override
  String toString() => 'StompException: $message';
}

class _Subscription {
  final String id;
  final String destination;
  final StompMessageHandler handler;
  _Subscription(this.id, this.destination, this.handler);
}

/// Cliente STOMP 1.2 sobre WebSocket con:
/// - JWT de Tappazo en el CONNECT (`Authorization: Bearer <jwt>`, Sección 68).
/// - Re-suscripción automática tras reconectar.
/// - Reconexión con backoff exponencial (1s → 30s).
/// - Heart-beats negociados con el servidor.
class StompClient {
  final String url;
  final StompTokenProvider tokenProvider;
  final Duration heartbeat;
  final Duration connectTimeout;
  final Duration maxReconnectDelay;
  final WebSocketChannelFactory _channelFactory;

  StompClient({
    required this.url,
    required this.tokenProvider,
    this.heartbeat = const Duration(seconds: 10),
    this.connectTimeout = const Duration(seconds: 10),
    this.maxReconnectDelay = const Duration(seconds: 30),
    WebSocketChannelFactory? channelFactory,
  }) : _channelFactory = channelFactory ?? WebSocketChannel.connect;

  final _statusController = StreamController<StompConnectionStatus>.broadcast();
  final _errorController = StreamController<String>.broadcast();
  final Map<String, _Subscription> _subscriptions = {};
  final StompFrameDecoder _decoder = StompFrameDecoder();

  StompConnectionStatus _status = StompConnectionStatus.disconnected;
  WebSocketChannel? _channel;
  StreamSubscription<dynamic>? _socketSub;
  Completer<void>? _connectedCompleter;
  Timer? _outgoingHeartbeat;
  Timer? _incomingWatchdog;
  Timer? _reconnectTimer;
  Duration _incomingInterval = Duration.zero;
  int _reconnectAttempt = 0;
  int _subscriptionSeq = 0;
  int _generation = 0;
  bool _manuallyClosed = true;
  bool _disposed = false;

  Stream<StompConnectionStatus> get statusStream => _statusController.stream;
  StompConnectionStatus get status => _status;
  bool get isConnected => _status == StompConnectionStatus.connected;

  /// Errores de protocolo (frames ERROR del broker o fallos de conexión).
  Stream<String> get errors => _errorController.stream;

  /// Abre la conexión. Si falla, programa reintentos automáticos y relanza.
  Future<void> connect() async {
    if (_disposed) throw const StompException('Cliente STOMP liberado');
    if (_status == StompConnectionStatus.connected) return;
    if (_connectedCompleter != null && !_connectedCompleter!.isCompleted) {
      return _connectedCompleter!.future;
    }
    _manuallyClosed = false;
    _reconnectTimer?.cancel();
    await _open();
  }

  /// Reintenta ya mismo, sin esperar el backoff pendiente. Pensado para cuando
  /// vuelve la red o la app regresa a primer plano. No hace nada si se cerró
  /// manualmente o si ya hay un handshake en curso.
  ///
  /// Con [force] se descarta también un socket que aparenta estar conectado:
  /// tras un corte de red el TCP anterior suele quedar muerto sin aviso.
  Future<void> reconnectNow({bool force = false}) async {
    if (_disposed || _manuallyClosed) return;
    if (_connectedCompleter != null && !_connectedCompleter!.isCompleted) return;
    if (_status == StompConnectionStatus.connected) {
      if (!force) return;
      _generation++; // invalida callbacks del socket viejo
      _teardownSocket();
    }
    _reconnectTimer?.cancel();
    _reconnectAttempt = 0;
    try {
      await _open();
    } catch (_) {
      // _open() ya notificó el error y programó el siguiente intento.
    }
  }

  Future<void> _open() async {
    final generation = ++_generation;
    final completer = Completer<void>();
    _connectedCompleter = completer;
    _setStatus(_reconnectAttempt == 0
        ? StompConnectionStatus.connecting
        : StompConnectionStatus.reconnecting);

    try {
      final token = await tokenProvider();
      if (token == null || token.isEmpty) {
        throw const StompException('No hay sesión activa (JWT ausente)');
      }

      final uri = Uri.parse(url);
      final channel = _channelFactory(uri);
      _channel = channel;
      await channel.ready.timeout(connectTimeout);
      if (generation != _generation) return;

      _decoder.reset();
      _socketSub = channel.stream.listen(
        (data) => _onData(data, generation),
        onError: (Object e) => _onSocketClosed(generation, 'Error de socket: $e'),
        onDone: () => _onSocketClosed(generation, 'Socket cerrado'),
        cancelOnError: true,
      );

      final ms = heartbeat.inMilliseconds;
      _write(StompFrame('CONNECT', headers: {
        'accept-version': '1.2',
        'host': uri.host,
        'heart-beat': '$ms,$ms',
        'Authorization': 'Bearer $token',
      }));

      await completer.future.timeout(connectTimeout);
    } catch (e) {
      if (generation != _generation) return;
      if (!completer.isCompleted) completer.completeError(e);
      // Evita "unhandled future error" si nadie espera este completer.
      completer.future.ignore();
      _errorController.add(e.toString());
      _teardownSocket();
      _scheduleReconnect();
      rethrow;
    }
  }

  void _onData(dynamic data, int generation) {
    if (generation != _generation) return;
    final text = data is String ? data : utf8.decode(data as List<int>);
    _resetWatchdog();
    for (final frame in _decoder.add(text)) {
      _handleFrame(frame);
    }
  }

  void _handleFrame(StompFrame frame) {
    switch (frame.command) {
      case 'CONNECTED':
        _negotiateHeartbeat(frame.headers['heart-beat']);
        _reconnectAttempt = 0;
        _setStatus(StompConnectionStatus.connected);
        for (final sub in _subscriptions.values) {
          _sendSubscribe(sub);
        }
        final completer = _connectedCompleter;
        if (completer != null && !completer.isCompleted) completer.complete();
      case 'MESSAGE':
        final subId = frame.headers['subscription'];
        final sub = subId != null ? _subscriptions[subId] : null;
        if (sub != null) {
          try {
            sub.handler(frame);
          } catch (e) {
            _errorController.add('Error procesando mensaje STOMP: $e');
          }
        }
      case 'ERROR':
        final message = frame.headers['message'] ?? frame.body;
        _errorController.add(message);
        final completer = _connectedCompleter;
        if (completer != null && !completer.isCompleted) {
          completer.completeError(StompException(message));
        }
      default:
        // RECEIPT u otros frames: no se usan.
        break;
    }
  }

  void _negotiateHeartbeat(String? serverHeader) {
    _outgoingHeartbeat?.cancel();
    _incomingWatchdog?.cancel();
    _incomingInterval = Duration.zero;
    if (serverHeader == null) return;
    final parts = serverHeader.split(',');
    if (parts.length != 2) return;
    final sx = int.tryParse(parts[0].trim()) ?? 0; // servidor puede enviar cada sx
    final sy = int.tryParse(parts[1].trim()) ?? 0; // servidor quiere recibir cada sy
    final c = heartbeat.inMilliseconds;

    if (c > 0 && sy > 0) {
      final interval = Duration(milliseconds: math.max(c, sy));
      _outgoingHeartbeat = Timer.periodic(interval, (_) {
        if (isConnected) _channel?.sink.add('\n');
      });
    }
    if (c > 0 && sx > 0) {
      _incomingInterval = Duration(milliseconds: math.max(c, sx));
      _resetWatchdog();
    }
  }

  void _resetWatchdog() {
    if (_incomingInterval == Duration.zero) return;
    _incomingWatchdog?.cancel();
    final generation = _generation;
    _incomingWatchdog = Timer(_incomingInterval * 3, () {
      _onSocketClosed(generation, 'Sin heart-beat del servidor');
    });
  }

  void _onSocketClosed(int generation, String reason) {
    if (generation != _generation) return;
    final completer = _connectedCompleter;
    if (completer != null && !completer.isCompleted) {
      // Aún en handshake: _open() captura el error, limpia y reprograma.
      completer.completeError(StompException(reason));
      return;
    }
    _teardownSocket();
    if (_manuallyClosed) {
      _setStatus(StompConnectionStatus.disconnected);
    } else {
      _errorController.add(reason);
      _scheduleReconnect();
    }
  }

  void _scheduleReconnect() {
    if (_manuallyClosed || _disposed) {
      _setStatus(StompConnectionStatus.disconnected);
      return;
    }
    _reconnectTimer?.cancel();
    final seconds = math.min(maxReconnectDelay.inSeconds, 1 << math.min(_reconnectAttempt, 5));
    _reconnectAttempt++;
    _setStatus(StompConnectionStatus.reconnecting);
    _reconnectTimer = Timer(Duration(seconds: seconds), () {
      _open().catchError((_) {
        // El error ya se notificó y se programó un nuevo intento.
      });
    });
  }

  void _teardownSocket() {
    _outgoingHeartbeat?.cancel();
    _incomingWatchdog?.cancel();
    _socketSub?.cancel();
    _socketSub = null;
    _channel?.sink.close();
    _channel = null;
  }

  /// Registra una suscripción. Se envía ahora si hay conexión y se repite
  /// automáticamente en cada reconexión. Devuelve el id para desuscribir.
  String subscribe(String destination, StompMessageHandler handler) {
    final id = 'sub-${_subscriptionSeq++}';
    final sub = _Subscription(id, destination, handler);
    _subscriptions[id] = sub;
    if (isConnected) _sendSubscribe(sub);
    return id;
  }

  void unsubscribe(String id) {
    final sub = _subscriptions.remove(id);
    if (sub != null && isConnected) {
      _write(StompFrame('UNSUBSCRIBE', headers: {'id': id}));
    }
  }

  void _sendSubscribe(_Subscription sub) {
    _write(StompFrame('SUBSCRIBE', headers: {
      'id': sub.id,
      'destination': sub.destination,
      'ack': 'auto',
    }));
  }

  /// Envía un frame SEND con cuerpo JSON. Lanza [StompException] si no hay conexión.
  void send(String destination, Object jsonBody) {
    if (!isConnected) {
      throw const StompException('Sin conexión en tiempo real. Reintenta en unos segundos.');
    }
    _write(StompFrame('SEND', headers: {
      'destination': destination,
      'content-type': 'application/json',
    }, body: jsonEncode(jsonBody)));
  }

  void _write(StompFrame frame) {
    _channel?.sink.add(frame.encode());
  }

  Future<void> disconnect() async {
    _manuallyClosed = true;
    _reconnectTimer?.cancel();
    _reconnectAttempt = 0;
    if (isConnected) {
      _write(const StompFrame('DISCONNECT'));
    }
    _generation++;
    _teardownSocket();
    _setStatus(StompConnectionStatus.disconnected);
  }

  void _setStatus(StompConnectionStatus status) {
    if (_status == status || _disposed) return;
    _status = status;
    _statusController.add(status);
  }

  Future<void> dispose() async {
    await disconnect();
    _disposed = true;
    _subscriptions.clear();
    await _statusController.close();
    await _errorController.close();
  }
}
