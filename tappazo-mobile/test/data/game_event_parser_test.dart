import 'package:flutter_test/flutter_test.dart';
import 'package:tappazo/data/models/game_event_parser.dart';
import 'package:tappazo/domain/entities/game_decisions.dart';
import 'package:tappazo/domain/entities/game_event.dart';
import 'package:tappazo/domain/entities/round_entity.dart';

Map<String, dynamic> envelope(String type, Map<String, dynamic> payload) => {
      'type': type,
      'gameId': 'g1',
      'timestamp': '2026-10-13T20:00:00Z',
      'payload': payload,
    };

void main() {
  test('ROUND_STARTED → RoundEntity (RoundResponse)', () {
    final e = GameEventParser.parse(envelope('ROUND_STARTED', {
      'id': 'r1',
      'gameId': 'g1',
      'mode': 'ULTIMO_PIERDE',
      'state': 'REVEALING',
      'drinkPrice': 5000,
      'roundType': 'NORMAL',
      'startedAt': '2026-10-13T20:00:00Z',
      'finishedAt': null,
    }));
    expect(e, isA<RoundStartedEvent>());
    final round = (e as RoundStartedEvent).round;
    expect(round.id, 'r1');
    expect(round.state, RoundState.revealing);
    expect(round.state.acceptsReveals, isTrue);
    expect(round.drinkPrice, 5000);
    expect(round.hasTurnInfo, isFalse);
  });

  test('ATTEMPT_SUBMITTED trata photoReference "" como sin foto', () {
    final e = GameEventParser.parse(envelope('ATTEMPT_SUBMITTED', {
      'roundId': 'r1',
      'attemptId': 'a1',
      'revealId': 'rv1',
      'attemptNumber': 2,
      'proposedNumber': 17,
      'photoReference': '',
      'status': 'PENDING',
    })) as AttemptSubmittedEvent;
    expect(e.attempt.attemptNumber, 2);
    expect(e.attempt.proposedNumber, 17);
    expect(e.attempt.hasPhoto, isFalse);
    expect(e.attempt.playerId, isNull);
  });

  test('ROUND_RESOLVED con perdedores, ganadores y movimientos', () {
    final e = GameEventParser.parse(envelope('ROUND_RESOLVED', {
      'roundId': 'r1',
      'confirmedLosers': ['u1'],
      'winners': ['u2', 'u3'],
      'hasTie': false,
      'debtMovements': [
        {'id': 'm1', 'gameId': 'g1', 'fromPlayerId': 'u1', 'toPlayerId': 'u2', 'amount': 5000, 'reason': 'ROUND_LOSS'},
      ],
    })) as RoundResolvedEvent;
    expect(e.resolution.confirmedLoserIds, ['u1']);
    expect(e.resolution.winnerIds, ['u2', 'u3']);
    expect(e.resolution.debtMovements.single.amount, 5000);
  });

  test('flujo de desempate', () {
    final started = GameEventParser.parse(envelope('TIE_BREAK_STARTED', {
      'tieBreakId': 't1',
      'sourceRoundId': 'r1',
      'tieType': 'PARTIAL',
      'affectedLoserSlots': 1,
      'status': 'PENDING',
      'allTiedCase': false,
    })) as TieBreakStartedEvent;
    expect(started.tieBreak.id, 't1');
    expect(started.tieBreak.isResolved, isFalse);

    final decider = GameEventParser.parse(envelope('TIE_BREAK_DECIDER_SELECTED', {
      'tieBreakId': 't1',
      'status': 'PENDING',
      'selectedDeciderId': 'u9',
    })) as TieBreakDeciderSelectedEvent;
    expect(decider.selectedDeciderId, 'u9');

    final resolved = GameEventParser.parse(envelope('TIE_BREAK_RESOLVED', {
      'tieBreakId': 't1',
      'status': 'RESOLVED',
      'resolutionMode': 'NUEVA_RONDA',
      'selectedDeciderId': '',
      'tieBreakRoundId': 'r2',
    })) as TieBreakResolvedEvent;
    expect(resolved.tieBreak.isResolved, isTrue);
    expect(resolved.tieBreak.resolutionMode, TieBreakDecision.nuevaRonda);
    expect(resolved.tieBreak.selectedDeciderId, isNull);
    expect(resolved.tieBreak.tieBreakRoundId, 'r2');
  });

  test('eventos desconocidos o mal formados se toleran', () {
    expect(GameEventParser.parse(envelope('HOST_CHANGED', {})), isA<UnknownGameEvent>());
    expect(
      GameEventParser.parse({'type': 'ROUND_RESOLVED', 'gameId': 'g1', 'payload': {'hasTie': 'nope'}}),
      isA<UnknownGameEvent>(),
    );
    expect(GameEventParser.parseBody('not json'), isNull);
  });

  test('timestamp numérico (epoch segundos) también se acepta', () {
    final e = GameEventParser.parse({'type': 'GAME_FINISHED', 'gameId': 'g1', 'timestamp': 1760400000.5, 'payload': {}});
    expect(e.timestamp, isNotNull);
    expect(e.timestamp!.isUtc, isTrue);
  });
}
