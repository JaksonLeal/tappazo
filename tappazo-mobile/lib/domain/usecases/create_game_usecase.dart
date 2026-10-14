import '../entities/game_entity.dart';
import '../entities/game_mode.dart';
import '../repositories/game_repository.dart';

class CreateGameUseCase {
  final GameRepository _repository;

  CreateGameUseCase(this._repository);

  Future<GameEntity> execute({
    required int drinkPrice,
    required GameMode initialMode,
    int? maxPlayers,
  }) {
    if (drinkPrice <= 0) {
      throw ArgumentError('El precio de la bebida debe ser mayor a 0');
    }
    if (maxPlayers != null && (maxPlayers < 2 || maxPlayers > 10)) {
      throw ArgumentError('El número de jugadores debe estar entre 2 y 10');
    }
    if (maxPlayers != null && maxPlayers < initialMode.minPlayers) {
      throw ArgumentError(
        'La modalidad ${initialMode.displayName} requiere al menos ${initialMode.minPlayers} jugadores',
      );
    }

    return _repository.createGame(
      drinkPrice: drinkPrice,
      initialMode: initialMode,
      maxPlayers: maxPlayers,
    );
  }
}

