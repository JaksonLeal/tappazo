import '../entities/game_entity.dart';
import '../entities/game_mode.dart';
import '../repositories/game_repository.dart';

class UpdateGameSettingsUseCase {
  final GameRepository _repository;

  UpdateGameSettingsUseCase(this._repository);

  Future<GameEntity> execute({
    required String gameId,
    int? drinkPrice,
    GameMode? mode,
  }) {
    if (gameId.trim().isEmpty) {
      throw ArgumentError('El ID de la partida no puede estar vacío');
    }
    if (drinkPrice != null && drinkPrice <= 0) {
      throw ArgumentError('El precio de la bebida debe ser mayor a 0');
    }

    return _repository.updateGameSettings(
      id: gameId.trim(),
      drinkPrice: drinkPrice,
      mode: mode,
    );
  }
}

