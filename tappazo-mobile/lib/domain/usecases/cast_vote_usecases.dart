import '../entities/game_decisions.dart';
import '../repositories/game_realtime_repository.dart';

/// Voto de desempate NUEVA_RONDA / DIVIDIR_CUENTA (Sección 33).
/// La identidad del votante la toma el backend del JWT.
class CastTieBreakVoteUseCase {
  final GameRealtimeRepository _realtime;
  CastTieBreakVoteUseCase(this._realtime);

  void execute({
    required String gameId,
    required String tieBreakId,
    required TieBreakDecision decision,
  }) {
    _realtime.submitTieBreakVote(gameId: gameId, tieBreakId: tieBreakId, decision: decision);
  }
}

/// Voto SÍ / NO sobre un RevealAttempt (Secciones 22-23).
class CastRevealVoteUseCase {
  final GameRealtimeRepository _realtime;
  CastRevealVoteUseCase(this._realtime);

  bool get isSupported => _realtime.isRevealVotingSupported;

  void execute({
    required String gameId,
    required String revealAttemptId,
    required RevealVoteDecision decision,
  }) {
    _realtime.submitRevealVote(gameId: gameId, revealAttemptId: revealAttemptId, decision: decision);
  }
}
