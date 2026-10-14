import 'dart:async';

import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../core/network/error_messages.dart';
import '../../domain/entities/game_decisions.dart';
import '../../domain/entities/game_details_entity.dart';
import '../../domain/entities/game_event.dart';
import '../../domain/entities/game_state.dart';
import '../../domain/entities/reveal_attempt_entity.dart';
import '../../domain/entities/round_entity.dart';
import '../../domain/entities/round_outcome_entities.dart';
import '../../domain/repositories/game_realtime_repository.dart';
import '../../domain/usecases/cast_vote_usecases.dart';
import '../../domain/usecases/submit_reveal_attempt_usecase.dart';
import 'game_session_state.dart';

/// Orquesta la sesión en tiempo real de una partida:
/// conexión STOMP, eventos del backend y acciones del jugador.
///
/// Regla de oro (Sección 69): este notifier solo refleja lo que publica el
/// backend. No decide turnos, votos válidos, perdedores ni desempates.
class GameSessionNotifier extends StateNotifier<GameSessionState> {
  final String gameId;
  final String currentUserId;
  final GameRealtimeRepository _realtime;
  final SubmitRevealAttemptUseCase _submitAttempt;
  final CastTieBreakVoteUseCase _castTieBreakVote;
  final CastRevealVoteUseCase _castRevealVote;
  final Future<GameDetailsEntity?> Function() _resync;
  final Stream<bool>? _networkChanges;

  /// Tiempo máximo esperando el eco ATTEMPT_SUBMITTED. El backend no envía
  /// ERROR por STOMP cuando rechaza un intento, así que se avisa al usuario.
  static const Duration echoTimeout = Duration(seconds: 12);

  StreamSubscription<RealtimeConnectionStatus>? _statusSub;
  StreamSubscription<GameEvent>? _eventsSub;
  StreamSubscription<bool>? _networkSub;
  DateTime? _pausedAt;
  bool _wasOffline = false;

  /// Si la app estuvo en segundo plano más que esto, el socket se recrea.
  static const Duration staleSocketAfter = Duration(seconds: 20);
  Timer? _echoTimer;
  int _noticeSeq = 0;
  bool _hasConnectedOnce = false;
  bool _resyncInFlight = false;

  GameSessionNotifier({
    required this.gameId,
    required this.currentUserId,
    required GameRealtimeRepository realtime,
    required SubmitRevealAttemptUseCase submitAttempt,
    required CastTieBreakVoteUseCase castTieBreakVote,
    required CastRevealVoteUseCase castRevealVote,
    required Future<GameDetailsEntity?> Function() resync,
    Stream<bool>? networkChanges,
  })  : _realtime = realtime,
        _networkChanges = networkChanges,
        _submitAttempt = submitAttempt,
        _castTieBreakVote = castTieBreakVote,
        _castRevealVote = castRevealVote,
        _resync = resync,
        super(GameSessionState(connection: realtime.currentStatus));

  bool get isRevealVotingSupported => _castRevealVote.isSupported;

  void start() {
    _statusSub = _realtime.connectionStatus.listen(_onConnectionStatus);
    _eventsSub = _realtime.watchGame(gameId).listen(_onEvent);
    _networkSub = _networkChanges?.listen(_onNetworkChanged);
    unawaited(_connect());
    unawaited(resync());
  }

  Future<void> _connect() async {
    try {
      await _realtime.connect();
    } catch (_) {
      // El cliente STOMP reintenta solo; el badge de conexión lo refleja.
    }
  }

  /// Reintento manual desde la UI.
  Future<void> reconnect() async {
    if (state.connection == RealtimeConnectionStatus.disconnected) {
      await _connect();
    } else {
      await _realtime.reconnectNow();
    }
  }

  // ─── Red y ciclo de vida de la app ──────────────────────────────────────────

  void _onNetworkChanged(bool online) {
    if (!mounted || online == state.isOnline) return;
    state = state.copyWith(isOnline: online);
    if (!online) {
      _wasOffline = true;
      return;
    }
    if (_wasOffline) {
      _wasOffline = false;
      // El socket previo casi seguro murió con la red: forzar uno nuevo.
      unawaited(_realtime.reconnectNow(force: true));
      unawaited(resync());
    }
  }

  void onAppPaused() => _pausedAt = DateTime.now();

  void onAppResumed() {
    if (!mounted) return;
    final pausedAt = _pausedAt;
    _pausedAt = null;
    final stale = pausedAt != null && DateTime.now().difference(pausedAt) > staleSocketAfter;
    unawaited(_realtime.reconnectNow(force: stale));
    unawaited(resync());
  }

  void _onConnectionStatus(RealtimeConnectionStatus status) {
    if (!mounted) return;
    state = state.copyWith(connection: status);
    if (status == RealtimeConnectionStatus.connected) {
      // Tras reconectar se pudieron perder eventos: refrescar por REST (Sección 68).
      if (_hasConnectedOnce) unawaited(resync());
      _hasConnectedOnce = true;
    }
  }

  /// Recupera el estado autoritativo por REST.
  Future<void> resync() async {
    if (_resyncInFlight || !mounted) return;
    _resyncInFlight = true;
    try {
      final details = await _resync();
      if (!mounted || details == null) return;
      final round = details.currentRound;
      if (round != null && round.id != state.round?.id) {
        _startRound(round, announce: false);
      } else if (round != null) {
        state = state.copyWith(round: round);
      }
      if (details.game.state == GameState.finished) {
        state = state.copyWith(gameFinished: true);
      }
      if (!state.hasSynced) state = state.copyWith(hasSynced: true);
    } catch (_) {
      // Fallo REST puntual: el siguiente evento o reconexión vuelve a intentarlo.
    } finally {
      _resyncInFlight = false;
    }
  }

  // ─── Eventos del backend ────────────────────────────────────────────────────

  void _onEvent(GameEvent event) {
    if (!mounted) return;
    switch (event) {
      case PlayerJoinedEvent():
        unawaited(resync());
      case RoundStartedEvent(:final round):
        _startRound(round, announce: true);
      case AttemptSubmittedEvent(:final attempt):
        _onAttemptSubmitted(attempt);
      case RoundResolvedEvent(:final resolution):
        _echoTimer?.cancel();
        state = state.copyWith(
          resolution: resolution,
          round: state.round?.copyWith(state: RoundState.finished),
          submission: const RevealSubmission(),
        );
        _notify('La ronda se resolvió.');
        unawaited(resync());
      case TieBreakStartedEvent(:final tieBreak):
        _echoTimer?.cancel();
        state = state.copyWith(
          tieBreak: tieBreak,
          myTieBreakVote: null,
          myArbiterVote: null,
          submission: const RevealSubmission(),
        );
        _notify('¡Hay empate! Los jugadores empatados deben votar.');
      case TieBreakDeciderSelectedEvent(:final tieBreakId, :final selectedDeciderId):
        final current = state.tieBreak;
        final updated = (current != null && current.id == tieBreakId)
            ? current.copyWith(selectedDeciderId: selectedDeciderId)
            : TieBreakEntity(id: tieBreakId, selectedDeciderId: selectedDeciderId);
        state = state.copyWith(tieBreak: updated, myArbiterVote: null);
        _notify(selectedDeciderId == currentUserId
            ? 'Los empatados no coincidieron: tú decides el desempate.'
            : 'Los empatados no coincidieron. Esperando al árbitro.');
      case TieBreakResolvedEvent(:final tieBreak):
        final current = state.tieBreak;
        final merged = (current != null && current.id == tieBreak.id)
            ? current.copyWith(
                status: tieBreak.status,
                resolutionMode: tieBreak.resolutionMode,
                selectedDeciderId: tieBreak.selectedDeciderId,
                tieBreakRoundId: tieBreak.tieBreakRoundId,
              )
            : tieBreak;
        state = state.copyWith(tieBreak: merged);
        _notify('Desempate resuelto: ${merged.resolutionMode?.label ?? 'sin modo'}.');
        unawaited(resync());
      case GameFinishedEvent():
        state = state.copyWith(gameFinished: true);
        _notify('La partida terminó.');
      case UnknownGameEvent():
        unawaited(resync());
    }
  }

  void _startRound(RoundEntity round, {required bool announce}) {
    _echoTimer?.cancel();
    final previousTie = state.tieBreak;
    // Una ronda de desempate (NUEVA_RONDA) conserva el contexto del desempate resuelto.
    final keepTie = round.isTieBreakRound && previousTie != null && previousTie.isResolved;
    state = GameSessionState(
      connection: state.connection,
      isOnline: state.isOnline,
      hasSynced: state.hasSynced,
      round: round,
      tieBreak: keepTie ? previousTie : null,
      notice: state.notice,
    );
    if (announce) {
      _notify(round.isTieBreakRound ? '¡Ronda de desempate iniciada!' : '¡La ronda comenzó! Prepara tu tapa.');
    }
  }

  void _onAttemptSubmitted(RevealAttemptEntity attempt) {
    final round = state.round;
    if (round == null || attempt.roundId != round.id) {
      // Intento de una ronda que no conocemos todavía: resincronizar.
      unawaited(resync());
      return;
    }

    final attempts = [
      ...state.attempts.where((a) => a.id != attempt.id),
      attempt,
    ];

    final sub = state.submission;
    final matchesPending = sub.isInFlight || sub.stage == SubmissionStage.unconfirmed;
    final isMine = attempt.playerId == currentUserId ||
        (matchesPending &&
            ((sub.photoReference != null && sub.photoReference == attempt.photoReference) ||
                (sub.photoOmitted && !attempt.hasPhoto && sub.proposedNumber == attempt.proposedNumber)));

    if (isMine) {
      _echoTimer?.cancel();
      state = state.copyWith(
        attempts: attempts,
        ownAttemptIds: {...state.ownAttemptIds, attempt.id},
        submission: const RevealSubmission(),
      );
      _notify('Intento #${attempt.attemptNumber} registrado. Esperando la votación.');
    } else {
      state = state.copyWith(attempts: attempts);
    }
  }

  // ─── Acciones del jugador ───────────────────────────────────────────────────

  /// Sube la foto y envía el intento. Devuelve `true` si se envió al backend.
  Future<bool> submitAttempt({
    required int proposedNumber,
    List<int>? photoBytes,
    PhotoOmittedReason? photoOmittedReason,
  }) async {
    final round = state.round;
    if (round == null) {
      _notify('No hay una ronda activa.', isError: true);
      return false;
    }
    if (!state.isOnline) {
      _notify('Sin internet. Tu intento se podrá enviar cuando vuelva la conexión.', isError: true);
      return false;
    }
    if (!state.isConnected) {
      _notify('Reconectando con la mesa. Inténtalo en unos segundos.', isError: true);
      return false;
    }

    final hasPhoto = photoBytes != null && photoBytes.isNotEmpty;
    state = state.copyWith(
      submission: RevealSubmission(
        stage: hasPhoto ? SubmissionStage.uploading : SubmissionStage.sending,
        proposedNumber: proposedNumber,
        photoOmitted: !hasPhoto,
      ),
    );

    try {
      final photoReference = await _submitAttempt.execute(
        gameId: gameId,
        roundId: round.id,
        proposedNumber: proposedNumber,
        photoBytes: photoBytes,
        photoOmittedReason: photoOmittedReason,
        onUploaded: () {
          if (!mounted) return;
          state = state.copyWith(
            submission: RevealSubmission(
              stage: SubmissionStage.sending,
              proposedNumber: proposedNumber,
              photoOmitted: !hasPhoto,
            ),
          );
        },
      );
      if (!mounted) return true;
      // Si el eco llegó antes de este punto, la submission ya volvió a idle.
      if (state.submission.stage == SubmissionStage.idle) return true;
      state = state.copyWith(
        submission: RevealSubmission(
          stage: SubmissionStage.awaitingEcho,
          proposedNumber: proposedNumber,
          photoReference: photoReference,
          photoOmitted: !hasPhoto,
        ),
      );
      _echoTimer?.cancel();
      _echoTimer = Timer(echoTimeout, _onEchoTimeout);
      return true;
    } catch (e) {
      if (!mounted) return false;
      final message = e is RevealAttemptValidationException
          ? e.message
          : 'No se pudo enviar el intento. ${describeError(e, fallback: 'Inténtalo de nuevo.')}';
      state = state.copyWith(
        submission: RevealSubmission(
          stage: SubmissionStage.failed,
          proposedNumber: proposedNumber,
          photoOmitted: !hasPhoto,
          error: message,
        ),
      );
      _notify(message, isError: true);
      return false;
    }
  }

  void _onEchoTimeout() {
    if (!mounted || state.submission.stage != SubmissionStage.awaitingEcho) return;
    state = state.copyWith(
      submission: RevealSubmission(
        stage: SubmissionStage.unconfirmed,
        proposedNumber: state.submission.proposedNumber,
        photoReference: state.submission.photoReference,
        photoOmitted: state.submission.photoOmitted,
        error: 'El servidor no confirmó tu intento. Puede que no sea tu turno o que '
            'la ronda ya no acepte revelaciones.',
      ),
    );
    unawaited(resync());
  }

  void dismissSubmissionError() {
    state = state.copyWith(submission: const RevealSubmission());
  }

  void castRevealVote(RevealAttemptEntity attempt, RevealVoteDecision decision) {
    if (!_castRevealVote.isSupported) {
      _notify('La votación de revelaciones aún no está disponible en el servidor.', isError: true);
      return;
    }
    try {
      _castRevealVote.execute(gameId: gameId, revealAttemptId: attempt.id, decision: decision);
      state = state.copyWith(myVotes: {...state.myVotes, attempt.id: decision});
    } catch (e) {
      _notify('No se pudo enviar tu voto. ${describeError(e, fallback: 'Revisa tu conexión e inténtalo de nuevo.')}', isError: true);
    }
  }

  /// Voto de empatado o, si [asArbiter], decisión final del árbitro.
  void castTieBreakVote(TieBreakDecision decision, {bool asArbiter = false}) {
    final tieBreak = state.tieBreak;
    if (tieBreak == null || tieBreak.isResolved) return;
    try {
      _castTieBreakVote.execute(gameId: gameId, tieBreakId: tieBreak.id, decision: decision);
      state = asArbiter ? state.copyWith(myArbiterVote: decision) : state.copyWith(myTieBreakVote: decision);
      _notify(asArbiter ? 'Decisión enviada.' : 'Voto enviado: ${decision.label}.');
    } catch (e) {
      _notify('No se pudo enviar tu voto. ${describeError(e, fallback: 'Revisa tu conexión e inténtalo de nuevo.')}', isError: true);
    }
  }

  void _notify(String message, {bool isError = false}) {
    if (!mounted) return;
    state = state.copyWith(notice: GameNotice(++_noticeSeq, message, isError: isError));
  }

  @override
  void dispose() {
    _echoTimer?.cancel();
    _statusSub?.cancel();
    _eventsSub?.cancel();
    _networkSub?.cancel();
    unawaited(_realtime.disconnect());
    super.dispose();
  }
}
