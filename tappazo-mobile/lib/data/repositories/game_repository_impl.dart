import '../../domain/entities/game_details_entity.dart';
import '../../domain/entities/game_entity.dart';
import '../../domain/entities/game_mode.dart';
import '../../domain/entities/game_participant_entity.dart';
import '../../domain/repositories/game_repository.dart';
import '../datasources/game_remote_datasource.dart';

class GameRepositoryImpl implements GameRepository {
  final GameRemoteDataSource _remoteDataSource;

  GameRepositoryImpl(this._remoteDataSource);

  @override
  Future<GameEntity> createGame({
    required int drinkPrice,
    required GameMode initialMode,
    int? maxPlayers,
  }) {
    return _remoteDataSource.createGame(
      drinkPrice: drinkPrice,
      initialMode: initialMode,
      maxPlayers: maxPlayers,
    );
  }

  @override
  Future<GameParticipantEntity> joinGame(String gameCode) {
    return _remoteDataSource.joinGame(gameCode);
  }

  @override
  Future<GameDetailsEntity> getGameDetails(String id) {
    return _remoteDataSource.getGameDetails(id);
  }

  @override
  Future<GameEntity> getGameByCode(String code) {
    return _remoteDataSource.getGameByCode(code);
  }

  @override
  Future<List<GameParticipantEntity>> getGameParticipants(String id) {
    return _remoteDataSource.getGameParticipants(id);
  }

  @override
  Future<GameEntity> updateGameSettings({
    required String id,
    int? drinkPrice,
    GameMode? mode,
  }) {
    return _remoteDataSource.updateGameSettings(
      id: id,
      drinkPrice: drinkPrice,
      mode: mode,
    );
  }
}

