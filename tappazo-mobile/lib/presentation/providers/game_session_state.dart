import '../../domain/entities/game_decisions.dart';
import '../../domain/entities/game_event.dart';
import '../../domain/entities/reveal_attempt_entity.dart';
import '../../domain/entities/round_entity.dart';
import '../../domain/entities/round_outcome_entities.dart';

/// Fase visual derivada exclusivamente de lo que informa el backend.
enum GamePhase { waitingForRound, revealing, tieBreak, resolved, finished }

enum SubmissionStage { idle, uploading, sending, awaitingEcho, unconfirmed, failed }

/// Estado del envío del RevealAttempt propio.
class RevealSubmission {
  final SubmissionStage stage;
  final int? proposedNumber;
  final String? photoReference;
  final bool photoOmitted;
  final String? error;

  const RevealSubmission({
    this.stage = SubmissionStage.idle,
    this.proposedNumber,
    this.photoReference,
    this.photoOmitted = false,
    this.error,
  });

  bool get isInFlight =>
      stage == SubmissionStage.uploading ||
      stage == SubmissionStage.sending ||
      stage == SubmissionStage.awaitingEcho;
}

class GameNotice {
  final int id;
  final String message;
  final bool isError;
  const GameNotice(this.id, this.message, {this.isError = false});
}

class GameSessionState {
  final RealtimeConnectionStatus connection;
  final RoundEntity? round;

  /// Intentos de la ronda actual en orden de llegada.
  final List<RevealAttemptEntity> attempts;

  /// Intentos identificados como propios (el eco ATTEMPT_SUBMITTED aún no trae playerId).
  final Set<String> ownAttemptIds;
  final Map<String, RevealVoteDecision> myVotes;
  final RevealSubmission submission;
  final RoundResolutionEntity? resolution;
  final TieBreakEntity? tieBreak;
  final TieBreakDecision? myTieBreakVote;
  final TieBreakDecision? myArbiterVote;
  final bool gameFinished;
  final GameNotice? notice;

  const GameSessionState({
    this.connection = RealtimeConnectionStatus.disconnected,
    this.round,
    this.attempts = const [],
    this.ownAttemptIds = const {},
    this.myVotes = const {},
    this.submission = const RevealSubmission(),
    this.resolution,
    this.tieBreak,
    this.myTieBreakVote,
    this.myArbiterVote,
    this.gameFinished = false,
    this.notice,
  });

  bool get isConnected => connection == RealtimeConnectionStatus.connected;

  GamePhase get phase {
    if (gameFinished) return GamePhase.finished;
    if (tieBreak != null && !tieBreak!.isResolved) return GamePhase.tieBreak;
    if (round?.state == RoundState.tieBreak) return GamePhase.tieBreak;
    if (resolution != null || tieBreak?.isResolved == true) return GamePhase.resolved;
    final r = round;
    if (r == null || r.isFinished) return GamePhase.waitingForRound;
    return GamePhase.revealing;
  }

  bool isOwnAttempt(RevealAttemptEntity attempt, String userId) =>
      ownAttemptIds.contains(attempt.id) || (attempt.playerId != null && attempt.playerId == userId);

  /// Último intento por revealId (los anteriores quedaron superados).
  List<RevealAttemptEntity> get latestAttemptsPerReveal {
    final byReveal = <String, RevealAttemptEntity>{};
    for (final a in attempts) {
      final key = a.revealId.isNotEmpty ? a.revealId : a.id;
      final current = byReveal[key];
      if (current == null || a.attemptNumber >= current.attemptNumber) {
        byReveal[key] = a;
      }
    }
    return byReveal.values.toList().reversed.toList();
  }

  static const _unset = Object();

  GameSessionState copyWith({
    RealtimeConnectionStatus? connection,
    Object? round = _unset,
    List<RevealAttemptEntity>? attempts,
    Set<String>? ownAttemptIds,
    Map<String, RevealVoteDecision>? myVotes,
    RevealSubmission? submission,
    Object? resolution = _unset,
    Object? tieBreak = _unset,
    Object? myTieBreakVote = _unset,
    Object? myArbiterVote = _unset,
    bool? gameFinished,
    GameNotice? notice,
  }) {
    return GameSessionState(
      connection: connection ?? this.connection,
      round: identical(round, _unset) ? this.round : round as RoundEntity?,
      attempts: attempts ?? this.attempts,
      ownAttemptIds: ownAttemptIds ?? this.ownAttemptIds,
      myVotes: myVotes ?? this.myVotes,
      submission: submission ?? this.submission,
      resolution: identical(resolution, _unset) ? this.resolution : resolution as RoundResolutionEntity?,
      tieBreak: identical(tieBreak, _unset) ? this.tieBreak : tieBreak as TieBreakEntity?,
      myTieBreakVote:
          identical(myTieBreakVote, _unset) ? this.myTieBreakVote : myTieBreakVote as TieBreakDecision?,
      myArbiterVote: identical(myArbiterVote, _unset) ? this.myArbiterVote : myArbiterVote as TieBreakDecision?,
      gameFinished: gameFinished ?? this.gameFinished,
      notice: notice ?? this.notice,
    );
  }
}
