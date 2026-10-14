import 'dart:convert';

import '../../domain/entities/game_decisions.dart';
import '../../domain/entities/game_event.dart';
import '../../domain/entities/reveal_attempt_entity.dart';
import '../../domain/entities/round_outcome_entities.dart';
import 'debt_movement_model.dart';
import 'game_participant_model.dart';
import 'json_utils.dart';
import 'round_model.dart';

/// Convierte el sobre `GameWebSocketEvent {type, gameId, timestamp, payload}`
/// publicado por `WebSocketGameNotificationAdapter` en eventos de dominio.
class GameEventParser {
  GameEventParser._();

  /// Devuelve `null` si el cuerpo no es un JSON de evento válido.
  static GameEvent? parseBody(String body) {
    try {
      final decoded = jsonDecode(body);
      if (decoded is! Map<String, dynamic>) return null;
      return parse(decoded);
    } on FormatException {
      return null;
    }
  }

  static GameEvent parse(Map<String, dynamic> json) {
    final type = json['type'] as String? ?? '';
    final gameId = json['gameId'] as String? ?? '';
    final timestamp = parseInstant(json['timestamp']);
    final rawPayload = json['payload'];
    final payload = rawPayload is Map<String, dynamic> ? rawPayload : const <String, dynamic>{};

    try {
      switch (type) {
        case 'PLAYER_JOINED':
          return PlayerJoinedEvent(
            gameId: gameId,
            timestamp: timestamp,
            participant: GameParticipantModel.fromJson(payload),
          );
        case 'ROUND_STARTED':
          return RoundStartedEvent(
            gameId: gameId,
            timestamp: timestamp,
            round: RoundModel.fromJson(payload),
          );
        case 'ATTEMPT_SUBMITTED':
          return AttemptSubmittedEvent(
            gameId: gameId,
            timestamp: timestamp,
            attempt: RevealAttemptEntity(
              id: payload['attemptId'] as String? ?? '',
              roundId: payload['roundId'] as String? ?? '',
              revealId: payload['revealId'] as String? ?? '',
              attemptNumber: (payload['attemptNumber'] as num?)?.toInt() ?? 1,
              proposedNumber: (payload['proposedNumber'] as num?)?.toInt() ?? 0,
              photoReference: nonEmptyString(payload['photoReference']),
              status: AttemptStatus.fromString(payload['status'] as String?),
              playerId: nonEmptyString(payload['playerId']),
              receivedAt: timestamp ?? DateTime.now(),
            ),
          );
        case 'ROUND_RESOLVED':
          final movements = payload['debtMovements'];
          return RoundResolvedEvent(
            gameId: gameId,
            timestamp: timestamp,
            resolution: RoundResolutionEntity(
              roundId: payload['roundId'] as String? ?? '',
              confirmedLoserIds: stringList(payload['confirmedLosers']),
              winnerIds: stringList(payload['winners']),
              hasTie: payload['hasTie'] as bool? ?? false,
              debtMovements: movements is List
                  ? movements
                      .whereType<Map<String, dynamic>>()
                      .map(DebtMovementModel.fromJson)
                      .toList(growable: false)
                  : const [],
            ),
          );
        case 'TIE_BREAK_STARTED':
          return TieBreakStartedEvent(
            gameId: gameId,
            timestamp: timestamp,
            tieBreak: TieBreakEntity(
              id: payload['tieBreakId'] as String? ?? '',
              sourceRoundId: nonEmptyString(payload['sourceRoundId']),
              tieType: payload['tieType']?.toString(),
              affectedLoserSlots: (payload['affectedLoserSlots'] as num?)?.toInt() ?? 0,
              status: TieBreakStatus.fromString(payload['status'] as String?),
              allTiedCase: payload['allTiedCase'] as bool? ?? false,
              tiedPlayerIds: stringList(payload['tiedPlayerIds']),
            ),
          );
        case 'TIE_BREAK_DECIDER_SELECTED':
          return TieBreakDeciderSelectedEvent(
            gameId: gameId,
            timestamp: timestamp,
            tieBreakId: payload['tieBreakId'] as String? ?? '',
            selectedDeciderId: payload['selectedDeciderId'] as String? ?? '',
          );
        case 'TIE_BREAK_RESOLVED':
          return TieBreakResolvedEvent(
            gameId: gameId,
            timestamp: timestamp,
            tieBreak: TieBreakEntity(
              id: payload['tieBreakId'] as String? ?? '',
              status: TieBreakStatus.fromString(payload['status'] as String?),
              resolutionMode: TieBreakDecision.fromString(payload['resolutionMode'] as String?),
              selectedDeciderId: nonEmptyString(payload['selectedDeciderId']),
              tieBreakRoundId: nonEmptyString(payload['tieBreakRoundId']),
            ),
          );
        case 'GAME_FINISHED':
          return GameFinishedEvent(gameId: gameId, timestamp: timestamp);
        default:
          return UnknownGameEvent(gameId: gameId, timestamp: timestamp, type: type);
      }
    } catch (_) {
      // Payload con forma inesperada: se trata como desconocido → refresco REST.
      return UnknownGameEvent(gameId: gameId, timestamp: timestamp, type: type);
    }
  }
}
