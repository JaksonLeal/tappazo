import '../entities/game_details_entity.dart';
import '../entities/game_entity.dart';
import '../entities/game_mode.dart';
import '../entities/game_participant_entity.dart';

abstract class GameRepository {
  Future<GameEntity> createGame({
    required int drinkPrice,
    required GameMode initialMode,
    int? maxPlayers,
  });

  Future<GameParticipantEntity> joinGame(String gameCode);

  Future<GameDetailsEntity> getGameDetails(String id);

  Future<GameEntity> getGameByCode(String code);

  Future<List<GameParticipantEntity>> getGameParticipants(String id);

  Future<GameEntity> updateGameSettings({
    required String id,
    int? drinkPrice,
    GameMode? mode,
  });
}

