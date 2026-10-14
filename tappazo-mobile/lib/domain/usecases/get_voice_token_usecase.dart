import '../entities/voice_entities.dart';
import '../repositories/voice_repository.dart';

/// Obtiene el token temporal de voz de la partida (Secciones 27 y 67).
class GetVoiceTokenUseCase {
  final VoiceRepository _repository;
  GetVoiceTokenUseCase(this._repository);

  Future<VoiceToken> execute(String gameId) {
    if (gameId.trim().isEmpty) {
      throw ArgumentError('gameId vacío');
    }
    return _repository.fetchVoiceToken(gameId);
  }
}
