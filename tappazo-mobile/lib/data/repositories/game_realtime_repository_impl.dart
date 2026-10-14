import '../../core/constants/api_constants.dart';
import '../../domain/entities/game_decisions.dart';
import '../../domain/entities/game_event.dart';
import '../../domain/repositories/game_realtime_repository.dart';
import '../datasources/game_realtime_datasource.dart';

class GameRealtimeRepositoryImpl implements GameRealtimeRepository {
  final GameRealtimeDataSource _remote;

  GameRealtimeRepositoryImpl(this._remote);

  @override
  Stream<RealtimeConnectionStatus> get connectionStatus => _remote.connectionStatus;

  @override
  RealtimeConnectionStatus get currentStatus => _remote.currentStatus;

  @override
  Stream<String> get errors => _remote.errors;

  @override
  Future<void> connect() => _remote.connect();

  @override
  Future<void> disconnect() => _remote.disconnect();

  @override
  Stream<GameEvent> watchGame(String gameId) => _remote.watchGame(gameId);

  @override
  void submitAttempt({
    required String gameId,
    required String roundId,
    required int proposedNumber,
    String? photoReference,
    PhotoOmittedReason? photoOmittedReason,
  }) {
    _remote.submitAttempt(
      gameId: gameId,
      roundId: roundId,
      proposedNumber: proposedNumber,
      photoReference: photoReference,
      photoOmittedReason: photoOmittedReason?.apiValue,
    );
  }

  @override
  void submitTieBreakVote({
    required String gameId,
    required String tieBreakId,
    required TieBreakDecision decision,
  }) {
    _remote.submitTieBreakVote(gameId: gameId, tieBreakId: tieBreakId, decision: decision);
  }

  @override
  bool get isRevealVotingSupported => ApiConstants.revealVotingEnabled;

  @override
  void submitRevealVote({
    required String gameId,
    required String revealAttemptId,
    required RevealVoteDecision decision,
  }) {
    _remote.submitRevealVote(gameId: gameId, revealAttemptId: revealAttemptId, decision: decision);
  }
}
