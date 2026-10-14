import 'dart:async';

import 'package:livekit_client/livekit_client.dart';

import '../../domain/entities/voice_entities.dart';
import '../../domain/services/voice_call_service.dart';

/// Llamada de voz sobre LiveKit (Sección 27). Solo audio: sin video.
///
/// El SDK gestiona por sí mismo la reconexión de la sala ante cortes breves
/// (RoomReconnecting → RoomReconnected); aquí solo se traducen sus eventos a
/// [VoiceRoomSnapshot] para la UI.
class LiveKitVoiceCallService implements VoiceCallService {
  final _controller = StreamController<VoiceRoomSnapshot>.broadcast();

  Room? _room;
  EventsListener<RoomEvent>? _listener;
  VoiceRoomSnapshot _current = const VoiceRoomSnapshot(status: VoiceConnectionStatus.idle);
  bool _leaving = false;

  @override
  Stream<VoiceRoomSnapshot> get snapshots => _controller.stream;

  @override
  VoiceRoomSnapshot get current => _current;

  @override
  Future<void> connect({required String url, required String token}) async {
    await _teardown();
    _leaving = false;
    _emit(status: VoiceConnectionStatus.connecting);

    final room = Room(
      roomOptions: const RoomOptions(
        adaptiveStream: false,
        dynacast: false,
        // DTX/RED reducen datos cuando nadie habla y mejoran la resiliencia.
        defaultAudioPublishOptions: AudioPublishOptions(dtx: true, red: true),
        defaultAudioOutputOptions: AudioOutputOptions(speakerOn: true),
        defaultAudioCaptureOptions: AudioCaptureOptions(
          echoCancellation: true,
          noiseSuppression: true,
          autoGainControl: true,
        ),
      ),
    );
    _room = room;
    _listener = room.createListener()
      ..on<RoomReconnectingEvent>((_) => _emit(status: VoiceConnectionStatus.reconnecting))
      ..on<RoomAttemptReconnectEvent>((_) => _emit(status: VoiceConnectionStatus.reconnecting))
      ..on<RoomReconnectedEvent>((_) => _emit(status: VoiceConnectionStatus.connected))
      ..on<RoomDisconnectedEvent>((e) => _onDisconnected(e.reason))
      ..on<ParticipantConnectedEvent>((_) => _emit())
      ..on<ParticipantDisconnectedEvent>((_) => _emit())
      ..on<ActiveSpeakersChangedEvent>((_) => _emit())
      ..on<TrackMutedEvent>((_) => _emit())
      ..on<TrackUnmutedEvent>((_) => _emit())
      ..on<TrackSubscribedEvent>((_) => _emit())
      ..on<LocalTrackPublishedEvent>((_) => _emit());

    try {
      await room.connect(url, token, connectOptions: const ConnectOptions(autoSubscribe: true));
      _emit(status: VoiceConnectionStatus.connected);
    } catch (e) {
      await _teardown();
      _emit(status: VoiceConnectionStatus.disconnected, reason: VoiceDisconnectReason.network);
      rethrow;
    }
  }

  void _onDisconnected(DisconnectReason? reason) {
    final mapped = _leaving
        ? VoiceDisconnectReason.userLeft
        : switch (reason) {
            DisconnectReason.clientInitiated => VoiceDisconnectReason.userLeft,
            DisconnectReason.participantRemoved || DisconnectReason.duplicateIdentity => VoiceDisconnectReason.removed,
            DisconnectReason.serverShutdown || DisconnectReason.roomDeleted => VoiceDisconnectReason.serverClosed,
            DisconnectReason.signalingConnectionFailure ||
            DisconnectReason.reconnectAttemptsExceeded ||
            DisconnectReason.disconnected ||
            DisconnectReason.joinFailure => VoiceDisconnectReason.network,
            _ => VoiceDisconnectReason.unknown,
          };
    _emit(status: VoiceConnectionStatus.disconnected, reason: mapped, micEnabled: false);
    unawaited(_teardown());
  }

  @override
  Future<void> setMicrophoneEnabled(bool enabled) async {
    final local = _room?.localParticipant;
    if (local == null) return;
    // La primera activación solicita el permiso RECORD_AUDIO en Android.
    await local.setMicrophoneEnabled(enabled);
    _emit();
  }

  @override
  Future<void> setSpeakerOn(bool on) async {
    await _room?.setSpeakerOn(on);
  }

  @override
  Future<void> disconnect() async {
    _leaving = true;
    final room = _room;
    if (room != null) {
      try {
        await room.disconnect();
      } catch (_) {}
    }
    await _teardown();
    _emit(status: VoiceConnectionStatus.idle, reason: VoiceDisconnectReason.userLeft, micEnabled: false);
  }

  Future<void> _teardown() async {
    final listener = _listener;
    final room = _room;
    _listener = null;
    _room = null;
    if (listener != null) await listener.dispose();
    if (room != null) await room.dispose();
  }

  List<VoiceParticipant> _participants() {
    final room = _room;
    if (room == null) return const [];
    final local = room.localParticipant;
    return [
      if (local != null)
        VoiceParticipant(
          identity: local.identity,
          isLocal: true,
          isSpeaking: local.isSpeaking,
          isMuted: !local.isMicrophoneEnabled(),
        ),
      for (final p in room.remoteParticipants.values)
        VoiceParticipant(
          identity: p.identity,
          isLocal: false,
          isSpeaking: p.isSpeaking,
          isMuted: p.isMuted,
        ),
    ];
  }

  void _emit({VoiceConnectionStatus? status, VoiceDisconnectReason? reason, bool? micEnabled}) {
    if (_controller.isClosed) return;
    final local = _room?.localParticipant;
    _current = VoiceRoomSnapshot(
      status: status ?? _current.status,
      micEnabled: micEnabled ?? (local?.isMicrophoneEnabled() ?? false),
      participants: _participants(),
      disconnectReason: reason ?? (status == null ? _current.disconnectReason : null),
    );
    _controller.add(_current);
  }
}
