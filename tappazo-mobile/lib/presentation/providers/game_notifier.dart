import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../../domain/entities/game_details_entity.dart';
import '../../domain/entities/game_entity.dart';
import '../../domain/entities/game_mode.dart';
import '../../domain/usecases/create_game_usecase.dart';
import '../../domain/usecases/get_game_details_usecase.dart';
import '../../domain/usecases/join_game_usecase.dart';
import '../../domain/usecases/update_game_settings_usecase.dart';
import 'game_state.dart';

class GameNotifier extends StateNotifier<GameStateData> {
  final CreateGameUseCase _createGameUseCase;
  final JoinGameUseCase _joinGameUseCase;
  final GetGameDetailsUseCase _getGameDetailsUseCase;
  final UpdateGameSettingsUseCase _updateGameSettingsUseCase;

  GameNotifier({
    required CreateGameUseCase createGameUseCase,
    required JoinGameUseCase joinGameUseCase,
    required GetGameDetailsUseCase getGameDetailsUseCase,
    required UpdateGameSettingsUseCase updateGameSettingsUseCase,
  })  : _createGameUseCase = createGameUseCase,
        _joinGameUseCase = joinGameUseCase,
        _getGameDetailsUseCase = getGameDetailsUseCase,
        _updateGameSettingsUseCase = updateGameSettingsUseCase,
        super(const GameInitial());

  /// Crea una nueva partida y carga sus detalles
  Future<GameEntity?> createGame({
    required int drinkPrice,
    required GameMode initialMode,
    int? maxPlayers,
  }) async {
    state = const GameLoading(message: 'Creando partida...');
    try {
      final game = await _createGameUseCase.execute(
        drinkPrice: drinkPrice,
        initialMode: initialMode,
        maxPlayers: maxPlayers,
      );

      // Cargar detalles de la partida recién creada
      final details = await _getGameDetailsUseCase.execute(game.id);
      state = GameLoaded(details: details);
      return game;
    } catch (e) {
      state = GameError('Error al crear partida: $e');
      return null;
    }
  }

  /// Se une a una partida existente mediante su código
  Future<bool> joinGame(String gameCode) async {
    state = const GameLoading(message: 'Uniéndose a la partida...');
    try {
      final participant = await _joinGameUseCase.execute(gameCode);
      final details = await _getGameDetailsUseCase.execute(participant.gameId);
      state = GameLoaded(details: details);
      return true;
    } catch (e) {
      state = GameError('Error al unirse a la partida: $e');
      return false;
    }
  }

  /// Recarga el estado y los participantes de la partida actual
  Future<void> refreshGameDetails(String gameId) async {
    try {
      final details = await _getGameDetailsUseCase.execute(gameId);
      state = GameLoaded(details: details);
    } catch (e) {
      // Si ya teníamos datos, no destruimos la pantalla pero notificamos error
      if (state is! GameLoaded) {
        state = GameError('Error al cargar detalles de la partida: $e');
      }
    }
  }

  /// Actualiza configuración (solo permitido por el host en LOBBY)
  Future<bool> updateSettings({
    required String gameId,
    int? drinkPrice,
    GameMode? mode,
  }) async {
    final current = state;
    if (current is GameLoaded) {
      state = current.copyWith(isUpdatingSettings: true);
    }

    try {
      final updatedGame = await _updateGameSettingsUseCase.execute(
        gameId: gameId,
        drinkPrice: drinkPrice,
        mode: mode,
      );

      if (current is GameLoaded) {
        final newDetails = GameDetailsEntity(
          game: updatedGame,
          participants: current.details.participants,
          currentRound: current.details.currentRound,
        );
        state = GameLoaded(details: newDetails, isUpdatingSettings: false);
      }
      return true;
    } catch (e) {
      if (current is GameLoaded) {
        state = current.copyWith(isUpdatingSettings: false);
      }
      state = GameError('Error al actualizar ajustes: $e');
      return false;
    }
  }

  void reset() {
    state = const GameInitial();
  }
}

