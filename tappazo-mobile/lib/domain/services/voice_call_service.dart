import '../entities/voice_entities.dart';

/// Cliente de llamada de voz, independiente del proveedor.
/// Implementación MVP: LiveKit (`LiveKitVoiceCallService`).
abstract class VoiceCallService {
  /// Cambios de estado de la sala (conexión, micrófono, participantes).
  Stream<VoiceRoomSnapshot> get snapshots;

  VoiceRoomSnapshot get current;

  /// Se une a la sala con el micrófono apagado (el jugador decide cuándo hablar).
  Future<void> connect({required String url, required String token});

  /// Activa/desactiva el micrófono. Puede lanzar si se niega el permiso.
  Future<void> setMicrophoneEnabled(bool enabled);

  Future<void> setSpeakerOn(bool on);

  Future<void> disconnect();
}
