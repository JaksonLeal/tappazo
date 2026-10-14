import 'dart:async';

import 'package:flutter_test/flutter_test.dart';
import 'package:tappazo/domain/entities/game_decisions.dart';
import 'package:tappazo/domain/entities/game_details_entity.dart';
import 'package:tappazo/domain/entities/game_entity.dart';
import 'package:tappazo/domain/entities/game_event.dart';
import 'package:tappazo/domain/entities/game_mode.dart';
import 'package:tappazo/domain/entities/game_state.dart';
import 'package:tappazo/domain/entities/reveal_attempt_entity.dart';
import 'package:tappazo/domain/entities/round_entity.dart';
import 'package:tappazo/domain/entities/round_outcome_entities.dart';
import 'package:tappazo/domain/repositories/game_realtime_repository.dart';
import 'package:tappazo/domain/repositories/media_repository.dart';
import 'package:tappazo/domain/usecases/cast_vote_usecases.dart';
import 'package:tappazo/domain/usecases/submit_reveal_attempt_usecase.dart';
import 'package:tappazo/presentation/providers/game_session_notifier.dart';
import 'package:tappazo/presentation/providers/game_session_state.dart';

class FakeRealtime implements GameRealtimeRepository {
  final statusCtrl = StreamController<RealtimeConnectionStatus>.broadcast();
  final eventsCtrl = StreamController<GameEvent>.broadcast();
  final attempts = <Map<String, Object?>>[];
  final tieVotes = <TieBreakDecision>[];
  RealtimeConnectionStatus status = RealtimeConnectionStatus.disconnected;
  bool votingSupported = false;

  void setStatus(RealtimeConnectionStatus s) {
    status = s;
    statusCtrl.add(s);
  }

  void emit(GameEvent e) => eventsCtrl.add(e);

  @override
  Stream<RealtimeConnectionStatus> get connectionStatus => statusCtrl.stream;
  @override
  RealtimeConnectionStatus get currentStatus => status;
  @override
  Stream<String> get errors => const Stream.empty();
  @override
  Future<void> connect() async => setStatus(RealtimeConnectionStatus.connected);
  @override
  Future<void> disconnect() async {}

  final reconnects = <bool>[];
  @override
  Future<void> reconnectNow({bool force = false}) async {
    reconnects.add(force);
    setStatus(RealtimeConnectionStatus.connecting);
    setStatus(RealtimeConnectionStatus.connected);
  }
  @override
  Stream<GameEvent> watchGame(String gameId) => eventsCtrl.stream;
  @override
  bool get isRevealVotingSupported => votingSupported;

  @override
  void submitAttempt({
    required String gameId,
    required String roundId,
    required int proposedNumber,
    String? photoReference,
    PhotoOmittedReason? photoOmittedReason,
  }) {
    attempts.add({
      'roundId': roundId,
      'proposedNumber': proposedNumber,
      'photoReference': photoReference,
      'photoOmittedReason': photoOmittedReason,
    });
  }

  @override
  void submitTieBreakVote({required String gameId, required String tieBreakId, required TieBreakDecision decision}) =>
      tieVotes.add(decision);

  @override
  void submitRevealVote({required String gameId, required String revealAttemptId, required RevealVoteDecision decision}) {}
}

class FakeMedia implements MediaRepository {
  int uploads = 0;
  @override
  Future<String> uploadRevealPhoto(List<int> jpegBytes) async {
    uploads++;
    return 'reveals/photo-$uploads.jpg';
  }

  @override
  Future<String> resolvePhotoUrl(String fileKey) async => 'http://x/$fileKey';
}

const round1 = RoundEntity(
  id: 'r1',
  gameId: 'g1',
  mode: 'ULTIMO_PIERDE',
  state: RoundState.revealing,
  drinkPrice: 5000,
  roundType: 'NORMAL',
);

GameDetailsEntity details({RoundEntity? round}) => GameDetailsEntity(
      game: const GameEntity(
        id: 'g1',
        code: 'ABCD',
        hostId: 'host',
        state: GameState.inProgress,
        maxPlayers: 4,
        currentDrinkPrice: 5000,
        currentMode: GameMode.ultimoPierde,
      ),
      participants: const [],
      currentRound: round,
    );

Future<void> pump() => Future<void>.delayed(Duration.zero);

void main() {
  late FakeRealtime realtime;
  late FakeMedia media;
  late GameSessionNotifier notifier;
  late int resyncCalls;
  late StreamController<bool> network;
  RoundEntity? restRound;

  setUp(() {
    realtime = FakeRealtime();
    media = FakeMedia();
    resyncCalls = 0;
    restRound = null;
    network = StreamController<bool>.broadcast();
    notifier = GameSessionNotifier(
      gameId: 'g1',
      currentUserId: 'me',
      realtime: realtime,
      submitAttempt: SubmitRevealAttemptUseCase(realtime, media),
      castTieBreakVote: CastTieBreakVoteUseCase(realtime),
      castRevealVote: CastRevealVoteUseCase(realtime),
      resync: () async {
        resyncCalls++;
        return details(round: restRound);
      },
      networkChanges: network.stream,
    )..start();
  });

  tearDown(() {
    if (notifier.mounted) notifier.dispose();
  });

  test('carga la ronda actual por REST al iniciar y conecta', () async {
    restRound = round1;
    await pump();
    await notifier.resync();
    expect(notifier.state.round?.id, 'r1');
    expect(notifier.state.phase, GamePhase.revealing);
    expect(notifier.state.isConnected, isTrue);
  });

  test('ROUND_STARTED inicia la fase de revelación', () async {
    realtime.emit(const RoundStartedEvent(gameId: 'g1', round: round1));
    await pump();
    expect(notifier.state.phase, GamePhase.revealing);
    expect(notifier.state.notice?.message, contains('ronda'));
  });

  test('envío de intento: sube foto, manda STOMP y reconoce su eco', () async {
    realtime.emit(const RoundStartedEvent(gameId: 'g1', round: round1));
    await pump();

    final ok = await notifier.submitAttempt(proposedNumber: 42, photoBytes: [1, 2, 3]);
    expect(ok, isTrue);
    expect(media.uploads, 1);
    expect(realtime.attempts.single['photoReference'], 'reveals/photo-1.jpg');
    expect(notifier.state.submission.stage, SubmissionStage.awaitingEcho);

    // Eco de otro jugador: no es propio.
    realtime.emit(const AttemptSubmittedEvent(
      gameId: 'g1',
      attempt: RevealAttemptEntity(
          id: 'a0', roundId: 'r1', revealId: 'rv0', attemptNumber: 1, proposedNumber: 7, photoReference: 'reveals/other.jpg'),
    ));
    await pump();
    expect(notifier.state.ownAttemptIds, isEmpty);

    // Eco propio (coincide el photoReference).
    realtime.emit(const AttemptSubmittedEvent(
      gameId: 'g1',
      attempt: RevealAttemptEntity(
          id: 'a1', roundId: 'r1', revealId: 'rv1', attemptNumber: 1, proposedNumber: 42, photoReference: 'reveals/photo-1.jpg'),
    ));
    await pump();
    expect(notifier.state.ownAttemptIds, {'a1'});
    expect(notifier.state.submission.stage, SubmissionStage.idle);
    expect(notifier.state.latestAttemptsPerReveal.map((a) => a.id), ['a1', 'a0']);
  });

  test('intento sin foto exige motivo de falla de cámara', () async {
    realtime.emit(const RoundStartedEvent(gameId: 'g1', round: round1));
    await pump();

    expect(await notifier.submitAttempt(proposedNumber: 10), isFalse);
    expect(notifier.state.submission.stage, SubmissionStage.failed);
    expect(realtime.attempts, isEmpty);

    expect(
      await notifier.submitAttempt(proposedNumber: 10, photoOmittedReason: PhotoOmittedReason.cameraUnavailable),
      isTrue,
    );
    expect(realtime.attempts.single['photoOmittedReason'], PhotoOmittedReason.cameraUnavailable);
    expect(media.uploads, 0);
  });

  test('número fuera de 0-99 se rechaza antes de enviar', () async {
    realtime.emit(const RoundStartedEvent(gameId: 'g1', round: round1));
    await pump();
    expect(await notifier.submitAttempt(proposedNumber: 100, photoBytes: [1]), isFalse);
    expect(media.uploads, 0);
    expect(realtime.attempts, isEmpty);
  });

  test('desempate: voto de empatado, árbitro seleccionado y resolución', () async {
    realtime.emit(const RoundStartedEvent(gameId: 'g1', round: round1));
    realtime.emit(const TieBreakStartedEvent(gameId: 'g1', tieBreak: TieBreakEntity(id: 't1', affectedLoserSlots: 1)));
    await pump();
    expect(notifier.state.phase, GamePhase.tieBreak);

    notifier.castTieBreakVote(TieBreakDecision.nuevaRonda);
    expect(realtime.tieVotes, [TieBreakDecision.nuevaRonda]);
    expect(notifier.state.myTieBreakVote, TieBreakDecision.nuevaRonda);

    realtime.emit(const TieBreakDeciderSelectedEvent(gameId: 'g1', tieBreakId: 't1', selectedDeciderId: 'me'));
    await pump();
    expect(notifier.state.tieBreak?.selectedDeciderId, 'me');
    expect(notifier.state.myArbiterVote, isNull);

    notifier.castTieBreakVote(TieBreakDecision.dividirCuenta, asArbiter: true);
    expect(notifier.state.myArbiterVote, TieBreakDecision.dividirCuenta);

    realtime.emit(const TieBreakResolvedEvent(
      gameId: 'g1',
      tieBreak: TieBreakEntity(id: 't1', status: TieBreakStatus.resolved, resolutionMode: TieBreakDecision.dividirCuenta),
    ));
    await pump();
    expect(notifier.state.phase, GamePhase.resolved);
    expect(notifier.state.tieBreak?.affectedLoserSlots, 1, reason: 'conserva datos del inicio');
  });

  test('ROUND_RESOLVED muestra el resultado del backend', () async {
    realtime.emit(const RoundStartedEvent(gameId: 'g1', round: round1));
    realtime.emit(const RoundResolvedEvent(
      gameId: 'g1',
      resolution: RoundResolutionEntity(roundId: 'r1', confirmedLoserIds: ['me'], winnerIds: ['u2'], hasTie: false),
    ));
    await pump();
    expect(notifier.state.phase, GamePhase.resolved);
    expect(notifier.state.resolution?.confirmedLoserIds, ['me']);
  });

  test('evento desconocido y reconexión disparan resync REST', () async {
    await pump();
    final before = resyncCalls;
    realtime.emit(const UnknownGameEvent(gameId: 'g1', type: 'HOST_CHANGED'));
    await pump();
    expect(resyncCalls, before + 1);

    realtime.setStatus(RealtimeConnectionStatus.reconnecting);
    realtime.setStatus(RealtimeConnectionStatus.connected);
    await pump();
    expect(resyncCalls, before + 2);
  });

  test('voto de revelación deshabilitado si el backend no lo soporta', () async {
    realtime.emit(const RoundStartedEvent(gameId: 'g1', round: round1));
    await pump();
    const attempt = RevealAttemptEntity(id: 'a9', roundId: 'r1', revealId: 'rv9', attemptNumber: 1, proposedNumber: 3);
    notifier.castRevealVote(attempt, RevealVoteDecision.yes);
    expect(notifier.state.myVotes, isEmpty);
    expect(notifier.state.notice?.isError, isTrue);
  });

  test('sin red: bloquea acciones; al volver fuerza reconexión STOMP y resync', () async {
    realtime.emit(const RoundStartedEvent(gameId: 'g1', round: round1));
    await pump();

    network.add(false);
    await pump();
    expect(notifier.state.isOnline, isFalse);
    expect(notifier.state.canAct, isFalse);
    expect(await notifier.submitAttempt(proposedNumber: 5, photoBytes: [1]), isFalse);
    expect(notifier.state.notice?.message, contains('Sin internet'));
    expect(media.uploads, 0);

    final before = resyncCalls;
    network.add(true);
    await pump();
    expect(notifier.state.isOnline, isTrue);
    expect(realtime.reconnects, [true], reason: 'socket viejo descartado');
    expect(resyncCalls, greaterThan(before));
    expect(notifier.state.canAct, isTrue);
  });

  test('volver a primer plano reconecta (forzado si estuvo mucho tiempo fuera)', () async {
    await pump();
    notifier.onAppResumed();
    await pump();
    expect(realtime.reconnects, [false]);
  });

  test('hasSynced solo se activa cuando responde el REST (estado de carga)', () async {
    final pending = Completer<GameDetailsEntity?>();
    final slow = GameSessionNotifier(
      gameId: 'g1',
      currentUserId: 'me',
      realtime: realtime,
      submitAttempt: SubmitRevealAttemptUseCase(realtime, media),
      castTieBreakVote: CastTieBreakVoteUseCase(realtime),
      castRevealVote: CastRevealVoteUseCase(realtime),
      resync: () => pending.future,
    )..start();
    await pump();
    expect(slow.state.hasSynced, isFalse, reason: 'la UI muestra "Sincronizando…"');
    pending.complete(details(round: round1));
    await pump();
    expect(slow.state.hasSynced, isTrue);
    expect(slow.state.phase, GamePhase.revealing);
    slow.dispose();
  });
}
