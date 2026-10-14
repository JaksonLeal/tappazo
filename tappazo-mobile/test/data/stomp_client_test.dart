import 'dart:async';
import 'dart:io';

import 'package:flutter_test/flutter_test.dart';
import 'package:tappazo/data/datasources/stomp/stomp_client.dart';
import 'package:tappazo/data/datasources/stomp/stomp_frame.dart';

/// Mini broker STOMP en proceso para probar el cliente sobre un socket real.
class _FakeBroker {
  late final HttpServer server;
  final frames = <StompFrame>[];
  final _sockets = <WebSocket>[];
  String? requiredToken;

  Future<void> start() async {
    server = await HttpServer.bind(InternetAddress.loopbackIPv4, 0);
    server.transform(WebSocketTransformer()).listen((ws) {
      _sockets.add(ws);
      final decoder = StompFrameDecoder();
      ws.listen((data) {
        for (final frame in decoder.add(data as String)) {
          frames.add(frame);
          _onFrame(ws, frame);
        }
      });
    });
  }

  void _onFrame(WebSocket ws, StompFrame frame) {
    switch (frame.command) {
      case 'CONNECT':
        if (requiredToken != null && frame.headers['Authorization'] != 'Bearer $requiredToken') {
          ws.add(const StompFrame('ERROR', headers: {'message': 'unauthorized'}).encode());
          return;
        }
        ws.add(const StompFrame('CONNECTED', headers: {'version': '1.2', 'heart-beat': '0,0'}).encode());
      case 'SUBSCRIBE':
        // Heart-beat suelto + mensaje dividido en dos fragmentos.
        final msg = StompFrame('MESSAGE', headers: {
          'subscription': frame.headers['id']!,
          'destination': frame.headers['destination']!,
          'message-id': '1',
        }, body: '{"type":"PING"}').encode();
        ws.add('\n');
        ws.add(msg.substring(0, 10));
        ws.add(msg.substring(10));
    }
  }

  String get url => 'ws://127.0.0.1:${server.port}/ws';

  Future<void> dropConnections() async {
    for (final s in _sockets) {
      await s.close();
    }
    _sockets.clear();
  }

  Future<void> stop() async {
    await dropConnections();
    await server.close(force: true);
  }
}

void main() {
  late _FakeBroker broker;

  setUp(() async {
    broker = _FakeBroker()..requiredToken = 'jwt-123';
    await broker.start();
  });

  tearDown(() => broker.stop());

  test('CONNECT envía el JWT y recibe mensajes de la suscripción', () async {
    final client = StompClient(url: broker.url, tokenProvider: () async => 'jwt-123');
    final received = Completer<String>();
    client.subscribe('/topic/game/g1', (f) => received.complete(f.body));

    await client.connect();
    expect(client.status, StompConnectionStatus.connected);
    expect(await received.future.timeout(const Duration(seconds: 3)), '{"type":"PING"}');

    final connect = broker.frames.firstWhere((f) => f.command == 'CONNECT');
    expect(connect.headers['Authorization'], 'Bearer jwt-123');
    expect(connect.headers['accept-version'], '1.2');

    client.send('/app/game/g1/attempt', {'proposedNumber': 42});
    await Future<void>.delayed(const Duration(milliseconds: 100));
    final send = broker.frames.firstWhere((f) => f.command == 'SEND');
    expect(send.headers['destination'], '/app/game/g1/attempt');
    expect(send.body, '{"proposedNumber":42}');

    await client.dispose();
  });

  test('sin JWT no conecta', () async {
    final client = StompClient(url: broker.url, tokenProvider: () async => null);
    await expectLater(client.connect(), throwsA(isA<StompException>()));
    await client.dispose();
  });

  test('send sin conexión lanza StompException', () async {
    final client = StompClient(url: broker.url, tokenProvider: () async => 'jwt-123');
    expect(() => client.send('/app/x', {}), throwsA(isA<StompException>()));
    await client.dispose();
  });

  test('reconecta y vuelve a suscribirse tras caída del socket', () async {
    final client = StompClient(url: broker.url, tokenProvider: () async => 'jwt-123');
    var messages = 0;
    client.subscribe('/topic/game/g1', (_) => messages++);
    await client.connect();
    await Future<void>.delayed(const Duration(milliseconds: 100));
    expect(messages, 1);

    await broker.dropConnections();
    await client.statusStream
        .firstWhere((s) => s == StompConnectionStatus.connected)
        .timeout(const Duration(seconds: 5));
    await Future<void>.delayed(const Duration(milliseconds: 100));

    expect(messages, 2, reason: 'la suscripción debe repetirse tras reconectar');
    expect(broker.frames.where((f) => f.command == 'SUBSCRIBE').length, 2);
    await client.dispose();
  });

  test('reconnectNow(force) recrea el socket y re-suscribe aunque parezca conectado', () async {
    final client = StompClient(url: broker.url, tokenProvider: () async => 'jwt-123');
    var messages = 0;
    client.subscribe('/topic/game/g1', (_) => messages++);
    await client.connect();
    await Future<void>.delayed(const Duration(milliseconds: 100));
    expect(messages, 1);

    await client.reconnectNow(); // sin force y conectado: no hace nada
    expect(broker.frames.where((f) => f.command == 'CONNECT').length, 1);

    await client.reconnectNow(force: true);
    await Future<void>.delayed(const Duration(milliseconds: 100));
    expect(client.status, StompConnectionStatus.connected);
    expect(broker.frames.where((f) => f.command == 'CONNECT').length, 2);
    expect(messages, 2);
    await client.dispose();
  });

  test('reconnectNow tras cierre manual no reabre la conexión', () async {
    final client = StompClient(url: broker.url, tokenProvider: () async => 'jwt-123');
    await client.connect();
    await client.disconnect();
    await client.reconnectNow(force: true);
    expect(client.status, StompConnectionStatus.disconnected);
    await client.dispose();
  });
}
