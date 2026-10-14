/// Token temporal de voz emitido por el backend (Sección 27).
/// `POST /api/v1/games/{id}/voice-token` → `{token, roomName, serverUrl}`.
class VoiceToken {
  final String token;
  final String roomName;
  final String serverUrl;

  const VoiceToken({required this.token, required this.roomName, required this.serverUrl});
}

enum VoiceConnectionStatus { idle, connecting, connected, reconnecting, disconnected }

/// Motivo de desconexión relevante para la UI.
enum VoiceDisconnectReason { userLeft, network, removed, serverClosed, unknown }

class VoiceParticipant {
  /// Identidad LiveKit = userId de Tappazo (la asigna el backend en el token).
  final String identity;
  final bool isLocal;
  final bool isSpeaking;
  final bool isMuted;

  const VoiceParticipant({
    required this.identity,
    required this.isLocal,
    this.isSpeaking = false,
    this.isMuted = true,
  });
}

/// Estado instantáneo de la sala de voz.
class VoiceRoomSnapshot {
  final VoiceConnectionStatus status;
  final bool micEnabled;
  final List<VoiceParticipant> participants;
  final VoiceDisconnectReason? disconnectReason;

  const VoiceRoomSnapshot({
    required this.status,
    this.micEnabled = false,
    this.participants = const [],
    this.disconnectReason,
  });
}
