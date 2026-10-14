import '../entities/game_participant_entity.dart';
import '../repositories/game_repository.dart';

class JoinGameUseCase {
  final GameRepository _repository;

  JoinGameUseCase(this._repository);

  Future<GameParticipantEntity> execute(String gameCode) {
    final cleanCode = gameCode.trim().toUpperCase();
    if (cleanCode.length < 4 || cleanCode.length > 10) {
      throw ArgumentError('El código debe tener entre 4 y 10 caracteres');
    }
    return _repository.joinGame(cleanCode);
  }
}

