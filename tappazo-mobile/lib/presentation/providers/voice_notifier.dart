import 'dart:async';

import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../core/network/error_messages.dart';
import '../../domain/entities/voice_entities.dart';
import '../../domain/services/voice_call_service.dart';
import '../../domain/usecases/get_voice_token_usecase.dart';

class VoiceState {
  final VoiceConnectionStatus status;
  final bool micEnabled;
  final bool speakerOn;
  final bool isTogglingMic;
  final List<VoiceParticipant> participants;
  final String? error;

  const VoiceState({
    this.status = VoiceConnectionStatus.idle,
    this.micEnabled = false,
    this.speakerOn = true,
    this.isTogglingMic = false,
    this.participants = const [],
    this.error,
  });

  bool get isInCall => status == VoiceConnectionStatus.connected || status == VoiceConnectionStatus.reconnecting;
  bool get isBusy => status == VoiceConnectionStatus.connecting;
  int get speakingCount => participants.where((p) => p.isSpeaking).length;

  static const _unset = Object();

  VoiceState copyWith({
    VoiceConnectionStatus? status,
    bool? micEnabled,
    bool? speakerOn,
    bool? isTogglingMic,
    List<VoiceParticipant>? participants,
    Object? error = _unset,
  }) {
    return VoiceState(
      status: status ?? this.status,
      micEnabled: micEnabled ?? this.micEnabled,
      speakerOn: speakerOn ?? this.speakerOn,
      isTogglingMic: isTogglingMic ?? this.isTogglingMic,
      participants: participants ?? this.participants,
      error: identical(error, _unset) ? this.error : error as String?,
    );
  }
}

/// Llamada de voz de la partida (Sección 27): pide el token al backend y se
/// une a la sala LiveKit `game-{gameId}`. Entra con el micrófono apagado.
class VoiceNotifier extends StateNotifier<VoiceState> {
  final String gameId;
  final GetVoiceTokenUseCase _getToken;
  final VoiceCallService _service;
  final Stream<bool>? _networkChanges;
  final Duration rejoinDelay;

  /// Reintentos automáticos tras una caída de red de la sala.
  static const int maxAutoRejoins = 3;

  StreamSubscription<VoiceRoomSnapshot>? _snapshotSub;
  StreamSubscription<bool>? _networkSub;
  Timer? _rejoinTimer;
  bool _wantsCall = false;
  bool _online = true;
  int _autoRejoins = 0;

  VoiceNotifier({
    required this.gameId,
    required GetVoiceTokenUseCase getToken,
    required VoiceCallService service,
    Stream<bool>? networkChanges,
    this.rejoinDelay = const Duration(seconds: 2),
  })  : _getToken = getToken,
        _service = service,
        _networkChanges = networkChanges,
        super(const VoiceState()) {
    _snapshotSub = _service.snapshots.listen(_onSnapshot);
    _networkSub = _networkChanges?.listen(_onNetwork);
  }

  /// Se une a la sala de voz de la partida.
  Future<void> join() async {
    if (state.isBusy || state.isInCall) return;
    _wantsCall = true;
    _rejoinTimer?.cancel();
    state = state.copyWith(status: VoiceConnectionStatus.connecting, error: null);
    try {
      final token = await _getToken.execute(gameId);
      if (!mounted || !_wantsCall) return;
      await _service.connect(url: token.serverUrl, token: token.token);
      if (!mounted) return;
      _autoRejoins = 0;
      if (!state.speakerOn) unawaited(_service.setSpeakerOn(false));
    } catch (e) {
      if (!mounted) return;
      state = state.copyWith(
        status: VoiceConnectionStatus.disconnected,
        micEnabled: false,
        participants: const [],
        error: _describeJoinError(e),
      );
    }
  }

  Future<void> leave() async {
    _wantsCall = false;
    _rejoinTimer?.cancel();
    await _service.disconnect();
    if (!mounted) return;
    state = state.copyWith(
      status: VoiceConnectionStatus.idle,
      micEnabled: false,
      participants: const [],
      error: null,
    );
  }

  Future<void> toggleMic() async {
    if (!state.isInCall || state.isTogglingMic) return;
    final target = !state.micEnabled;
    state = state.copyWith(isTogglingMic: true, error: null);
    try {
      await _service.setMicrophoneEnabled(target);
      if (mounted) state = state.copyWith(micEnabled: target, isTogglingMic: false);
    } catch (e) {
      if (!mounted) return;
      final text = e.toString().toLowerCase();
      final denied = text.contains('permission') || text.contains('notallowed') || text.contains('denied');
      state = state.copyWith(
        isTogglingMic: false,
        micEnabled: false,
        error: denied
            ? 'Permiso de micrófono denegado. Actívalo en Ajustes para hablar.'
            : 'No se pudo activar el micrófono.',
      );
    }
  }

  Future<void> toggleSpeaker() async {
    final target = !state.speakerOn;
    state = state.copyWith(speakerOn: target);
    if (state.isInCall) {
      try {
        await _service.setSpeakerOn(target);
      } catch (_) {
        if (mounted) state = state.copyWith(speakerOn: !target);
      }
    }
  }

  void _onSnapshot(VoiceRoomSnapshot snapshot) {
    if (!mounted) return;
    final dropped = snapshot.status == VoiceConnectionStatus.disconnected;
    state = state.copyWith(
      status: snapshot.status == VoiceConnectionStatus.idle && _wantsCall ? state.status : snapshot.status,
      micEnabled: snapshot.micEnabled,
      participants: snapshot.participants,
      error: dropped ? _describeDisconnect(snapshot.disconnectReason) : state.error,
    );
    if (dropped && _wantsCall && snapshot.disconnectReason == VoiceDisconnectReason.network) {
      _scheduleRejoin();
    } else if (dropped && snapshot.disconnectReason != VoiceDisconnectReason.network) {
      _wantsCall = false;
    }
  }

  void _onNetwork(bool online) {
    final cameBack = online && !_online;
    _online = online;
    if (cameBack && _wantsCall && state.status == VoiceConnectionStatus.disconnected) {
      _autoRejoins = 0;
      _scheduleRejoin(immediate: true);
    }
  }

  void _scheduleRejoin({bool immediate = false}) {
    if (!_online || _autoRejoins >= maxAutoRejoins) return;
    _autoRejoins++;
    _rejoinTimer?.cancel();
    _rejoinTimer = Timer(immediate ? Duration.zero : rejoinDelay * _autoRejoins, () {
      if (!mounted || !_wantsCall) return;
      // Nuevo token: el anterior pudo expirar (TTL de 2 h en el backend).
      unawaited(join());
    });
  }

  String _describeJoinError(Object e) {
    final text = e.toString();
    if (text.contains('DioException')) {
      return 'No se pudo obtener el acceso a la voz. ${describeError(e)}';
    }
    return 'No se pudo conectar al servidor de voz. Revisa tu conexión.';
  }

  String? _describeDisconnect(VoiceDisconnectReason? reason) => switch (reason) {
        VoiceDisconnectReason.network => 'Se perdió la conexión de voz. Reintentando…',
        VoiceDisconnectReason.removed => 'Saliste de la voz: tu cuenta se conectó desde otro dispositivo.',
        VoiceDisconnectReason.serverClosed => 'La sala de voz se cerró.',
        _ => null,
      };

  @override
  void dispose() {
    _wantsCall = false;
    _rejoinTimer?.cancel();
    _snapshotSub?.cancel();
    _networkSub?.cancel();
    unawaited(_service.disconnect());
    super.dispose();
  }
}
