import 'package:flutter_test/flutter_test.dart';
import 'package:tappazo/domain/entities/game_details_entity.dart';
import 'package:tappazo/domain/entities/game_entity.dart';
import 'package:tappazo/domain/entities/game_mode.dart';
import 'package:tappazo/domain/entities/game_participant_entity.dart';
import 'package:tappazo/domain/entities/game_state.dart';
import 'package:tappazo/domain/repositories/game_repository.dart';
import 'package:tappazo/domain/usecases/create_game_usecase.dart';
import 'package:tappazo/domain/usecases/join_game_usecase.dart';

class MockGameRepository implements GameRepository {
  @override
  Future<GameEntity> createGame({
    required int drinkPrice,
    required GameMode initialMode,
    int? maxPlayers,
  }) async {
    return GameEntity(
      id: 'mock-game-1',
      code: 'TEST01',
      hostId: 'mock-user-1',
      state: GameState.lobby,
      maxPlayers: maxPlayers ?? 8,
      currentDrinkPrice: drinkPrice,
      currentMode: initialMode,
    );
  }

  @override
  Future<GameParticipantEntity> joinGame(String gameCode) async {
    return const GameParticipantEntity(
      id: 'mock-part-1',
      gameId: 'mock-game-1',
      userId: 'mock-user-2',
      role: 'PLAYER',
      participantState: 'ACTIVE',
    );
  }

  @override
  Future<GameDetailsEntity> getGameDetails(String id) async => throw UnimplementedError();

  @override
  Future<GameEntity> getGameByCode(String code) async => throw UnimplementedError();

  @override
  Future<List<GameParticipantEntity>> getGameParticipants(String id) async => throw UnimplementedError();

  @override
  Future<GameEntity> updateGameSettings({required String id, int? drinkPrice, GameMode? mode}) async =>
      throw UnimplementedError();
}

void main() {
  late MockGameRepository mockRepo;
  late CreateGameUseCase createGameUseCase;
  late JoinGameUseCase joinGameUseCase;

  setUp(() {
    mockRepo = MockGameRepository();
    createGameUseCase = CreateGameUseCase(mockRepo);
    joinGameUseCase = JoinGameUseCase(mockRepo);
  });

  group('CreateGameUseCase Validation Tests', () {
    test('rejects drinkPrice <= 0', () async {
      expect(
        () => createGameUseCase.execute(
          drinkPrice: 0,
          initialMode: GameMode.ultimoPierde,
        ),
        throwsA(isA<ArgumentError>()),
      );
    });

    test('rejects maxPlayers outside 2..10 range', () async {
      expect(
        () => createGameUseCase.execute(
          drinkPrice: 4000,
          initialMode: GameMode.ultimoPierde,
          maxPlayers: 1,
        ),
        throwsA(isA<ArgumentError>()),
      );

      expect(
        () => createGameUseCase.execute(
          drinkPrice: 4000,
          initialMode: GameMode.ultimoPierde,
          maxPlayers: 11,
        ),
        throwsA(isA<ArgumentError>()),
      );
    });

    test('rejects maxPlayers lower than mode minimum', () async {
      // Pequenos pagan requiere mínimo 3 jugadores
      expect(
        () => createGameUseCase.execute(
          drinkPrice: 4000,
          initialMode: GameMode.pequenosPagan,
          maxPlayers: 2,
        ),
        throwsA(isA<ArgumentError>()),
      );

      // Ultimos dos pierden requiere mínimo 4 jugadores
      expect(
        () => createGameUseCase.execute(
          drinkPrice: 4000,
          initialMode: GameMode.ultimosDosPierden,
          maxPlayers: 3,
        ),
        throwsA(isA<ArgumentError>()),
      );
    });

    test('accepts valid game parameters', () async {
      final game = await createGameUseCase.execute(
        drinkPrice: 5000,
        initialMode: GameMode.pequenosPagan,
        maxPlayers: 6,
      );

      expect(game.id, equals('mock-game-1'));
      expect(game.currentDrinkPrice, equals(5000));
      expect(game.currentMode, equals(GameMode.pequenosPagan));
    });
  });

  group('JoinGameUseCase Validation Tests', () {
    test('rejects code shorter than 4 characters', () {
      expect(
        () => joinGameUseCase.execute('ABC'),
        throwsA(isA<ArgumentError>()),
      );
    });

    test('rejects code longer than 10 characters', () {
      expect(
        () => joinGameUseCase.execute('ABCDEFGHIJK'),
        throwsA(isA<ArgumentError>()),
      );
    });

    test('accepts valid code and converts to uppercase', () async {
      final participant = await joinGameUseCase.execute('x7k29p');
      expect(participant.gameId, equals('mock-game-1'));
    });
  });
}
