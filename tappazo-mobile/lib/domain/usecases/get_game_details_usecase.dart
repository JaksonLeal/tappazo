import '../entities/game_details_entity.dart';
import '../repositories/game_repository.dart';

class GetGameDetailsUseCase {
  final GameRepository _repository;

  GetGameDetailsUseCase(this._repository);

  Future<GameDetailsEntity> execute(String gameId) {
    if (gameId.trim().isEmpty) {
      throw ArgumentError('El ID de la partida no puede estar vacío');
    }
    return _repository.getGameDetails(gameId.trim());
  }
}

