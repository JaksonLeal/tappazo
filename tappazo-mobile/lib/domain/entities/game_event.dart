import 'game_participant_entity.dart';
import 'reveal_attempt_entity.dart';
import 'round_entity.dart';
import 'round_outcome_entities.dart';

enum RealtimeConnectionStatus { disconnected, connecting, connected, reconnecting }

/// Eventos publicados por el backend en `/topic/game/{gameId}` (Sección 68).
sealed class GameEvent {
  final String gameId;
  final DateTime? timestamp;
  const GameEvent({required this.gameId, this.timestamp});
}

class PlayerJoinedEvent extends GameEvent {
  final GameParticipantEntity participant;
  const PlayerJoinedEvent({required super.gameId, super.timestamp, required this.participant});
}

class RoundStartedEvent extends GameEvent {
  final RoundEntity round;
  const RoundStartedEvent({required super.gameId, super.timestamp, required this.round});
}

class AttemptSubmittedEvent extends GameEvent {
  final RevealAttemptEntity attempt;
  const AttemptSubmittedEvent({required super.gameId, super.timestamp, required this.attempt});
}

class RoundResolvedEvent extends GameEvent {
  final RoundResolutionEntity resolution;
  const RoundResolvedEvent({required super.gameId, super.timestamp, required this.resolution});
}

class TieBreakStartedEvent extends GameEvent {
  final TieBreakEntity tieBreak;
  const TieBreakStartedEvent({required super.gameId, super.timestamp, required this.tieBreak});
}

class TieBreakDeciderSelectedEvent extends GameEvent {
  final String tieBreakId;
  final String selectedDeciderId;
  const TieBreakDeciderSelectedEvent({
    required super.gameId,
    super.timestamp,
    required this.tieBreakId,
    required this.selectedDeciderId,
  });
}

class TieBreakResolvedEvent extends GameEvent {
  final TieBreakEntity tieBreak;
  const TieBreakResolvedEvent({required super.gameId, super.timestamp, required this.tieBreak});
}

class GameFinishedEvent extends GameEvent {
  const GameFinishedEvent({required super.gameId, super.timestamp});
}

/// Evento no reconocido: se tolera y dispara un refresco REST (Sección 68).
class UnknownGameEvent extends GameEvent {
  final String type;
  const UnknownGameEvent({required super.gameId, super.timestamp, required this.type});
}
