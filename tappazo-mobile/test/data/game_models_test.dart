import 'package:flutter_test/flutter_test.dart';
import 'package:tappazo/data/models/debt_movement_model.dart';
import 'package:tappazo/data/models/game_details_model.dart';
import 'package:tappazo/data/models/game_model.dart';
import 'package:tappazo/data/models/mesa_balance_model.dart';
import 'package:tappazo/data/models/mesa_model.dart';
import 'package:tappazo/domain/entities/game_mode.dart';
import 'package:tappazo/domain/entities/game_state.dart';

void main() {
  group('GameModel Tests', () {
    test('fromJson parses GameResponse correctly', () {
      final json = {
        'id': 'game-123',
        'code': 'X7K29P',
        'hostId': 'user-host-1',
        'state': 'LOBBY',
        'maxPlayers': 8,
        'currentDrinkPrice': 4000,
        'currentMode': 'ULTIMO_PIERDE',
        'createdAt': '2026-10-13T20:00:00.000Z',
      };

      final game = GameModel.fromJson(json);

      expect(game.id, equals('game-123'));
      expect(game.code, equals('X7K29P'));
      expect(game.hostId, equals('user-host-1'));
      expect(game.state, equals(GameState.lobby));
      expect(game.maxPlayers, equals(8));
      expect(game.currentDrinkPrice, equals(4000));
      expect(game.currentMode, equals(GameMode.ultimoPierde));
      expect(game.isHost('user-host-1'), isTrue);
      expect(game.isHost('user-other'), isFalse);
    });
  });

  group('GameDetailsModel and Sorting Tests', () {
    test('participants are sorted alphabetically by nickname', () {
      final json = {
        'game': {
          'id': 'game-123',
          'code': 'X7K29P',
          'hostId': 'user-1',
          'state': 'LOBBY',
          'maxPlayers': 6,
          'currentDrinkPrice': 3500,
          'currentMode': 'PEQUENOS_PAGAN',
        },
        'participants': [
          {
            'id': 'part-1',
            'gameId': 'game-123',
            'userId': 'user-c',
            'role': 'PLAYER',
            'participantState': 'ACTIVE',
            'nickname': 'Zoe',
          },
          {
            'id': 'part-2',
            'gameId': 'game-123',
            'userId': 'user-a',
            'role': 'PLAYER',
            'participantState': 'ACTIVE',
            'nickname': 'Andrés',
          },
          {
            'id': 'part-3',
            'gameId': 'game-123',
            'userId': 'user-b',
            'role': 'PLAYER',
            'participantState': 'ACTIVE',
            'nickname': 'Carlos',
          },
        ],
        'currentRound': null,
      };

      final details = GameDetailsModel.fromJson(json);
      final sorted = details.participantsSortedAlphabetically;

      expect(sorted[0].displayName, equals('Andrés'));
      expect(sorted[1].displayName, equals('Carlos'));
      expect(sorted[2].displayName, equals('Zoe'));
    });
  });

  group('Mesa and Balance Models Tests', () {
    test('fromJson parses MesaResponse and MesaBalanceResponse', () {
      final mesaJson = {
        'gameId': 'game-123',
        'code': 'X7K29P',
        'hostId': 'user-host-1',
        'state': 'LOBBY',
        'playerCount': 2,
        'participants': [
          {
            'id': 'part-1',
            'gameId': 'game-123',
            'userId': 'u-1',
            'role': 'PLAYER',
            'participantState': 'ACTIVE',
            'nickname': 'HostUser',
          }
        ],
      };

      final mesa = MesaModel.fromJson(mesaJson);
      expect(mesa.gameId, equals('game-123'));
      expect(mesa.playerCount, equals(2));

      final balanceJson = {
        'gameId': 'game-123',
        'netDebts': [
          {
            'fromPlayerId': 'user-loser-1',
            'toPlayerId': 'user-winner-1',
            'amount': 4000,
          }
        ],
        'totalActiveDebt': 4000,
      };

      final balance = MesaBalanceModel.fromJson(balanceJson);
      expect(balance.totalActiveDebt, equals(4000));
      expect(balance.netDebts.length, equals(1));
      expect(balance.netDebts[0].amount, equals(4000));
      expect(balance.netDebts[0].fromPlayerId, equals('user-loser-1'));

      final debtJson = {
        'id': 'debt-001',
        'gameId': 'game-123',
        'roundId': 'round-001',
        'fromPlayerId': 'user-loser-1',
        'toPlayerId': 'user-winner-1',
        'amount': 4000,
        'reason': 'ROUND_LOSS',
      };

      final debt = DebtMovementModel.fromJson(debtJson);
      expect(debt.reasonDisplay, equals('Pérdida de ronda'));
    });
  });
}
