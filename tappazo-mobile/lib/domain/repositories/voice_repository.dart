import '../entities/voice_entities.dart';

/// Repositorio de tokens de voz. El backend solo genera el token; toda la
/// transmisión ocurre en la infraestructura de LiveKit (Sección 27).
abstract class VoiceRepository {
  Future<VoiceToken> fetchVoiceToken(String gameId);
}
