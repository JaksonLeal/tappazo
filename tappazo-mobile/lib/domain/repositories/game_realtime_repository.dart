import '../entities/game_decisions.dart';
import '../entities/game_event.dart';

/// Canal en tiempo real de la partida. El backend es la autoridad: este
/// repositorio solo transporta acciones y eventos, nunca resuelve resultados.
abstract class GameRealtimeRepository {
  Stream<RealtimeConnectionStatus> get connectionStatus;
  RealtimeConnectionStatus get currentStatus;
  Stream<String> get errors;

  Future<void> connect();
  Future<void> disconnect();

  /// Reconecta sin esperar el backoff. Con [force] descarta un socket que
  /// aparenta estar vivo (tras un corte de red suele estar muerto).
  Future<void> reconnectNow({bool force = false});

  Stream<GameEvent> watchGame(String gameId);

  void submitAttempt({
    required String gameId,
    required String roundId,
    required int proposedNumber,
    String? photoReference,
    PhotoOmittedReason? photoOmittedReason,
  });

  void submitTieBreakVote({
    required String gameId,
    required String tieBreakId,
    required TieBreakDecision decision,
  });

  /// `false` mientras el backend no exponga la acción de voto de revelación.
  bool get isRevealVotingSupported;

  void submitRevealVote({
    required String gameId,
    required String revealAttemptId,
    required RevealVoteDecision decision,
  });
}
