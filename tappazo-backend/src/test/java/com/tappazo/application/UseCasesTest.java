package com.tappazo.application;

import com.tappazo.application.fakes.InMemoryRepositories.*;
import com.tappazo.application.usecase.*;
import com.tappazo.domain.exception.DomainException;
import com.tappazo.domain.exception.InvalidGameStateException;
import com.tappazo.domain.model.*;
import com.tappazo.domain.rule.GameModeRuleRegistry;
import com.tappazo.domain.service.BillDivisionEngine;
import com.tappazo.domain.service.DebtCalculationEngine;
import com.tappazo.domain.service.TieBreakResolver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Pruebas de la Capa de Aplicación - Casos de Uso (Fase 4)")
class UseCasesTest {

    private GameRepo gameRepo;
    private GameParticipantRepo gameParticipantRepo;
    private RoundRepo roundRepo;
    private RoundParticipantRepo roundParticipantRepo;
    private RevealRepo revealRepo;
    private TieBreakRepo tieBreakRepo;
    private DebtMovementRepo debtMovementRepo;
    private StatisticsRepo statisticsRepo;
    private NotificationPort notificationPort;

    private CreateGameUseCase createGameUseCase;
    private JoinGameUseCase joinGameUseCase;
    private SubmitAttemptUseCase submitAttemptUseCase;
    private ResolveRoundUseCase resolveRoundUseCase;
    private ProcessTieBreakVoteUseCase processTieBreakVoteUseCase;

    @BeforeEach
    void setUp() {
        gameRepo = new GameRepo();
        gameParticipantRepo = new GameParticipantRepo();
        roundRepo = new RoundRepo();
        roundParticipantRepo = new RoundParticipantRepo();
        revealRepo = new RevealRepo();
        tieBreakRepo = new TieBreakRepo();
        debtMovementRepo = new DebtMovementRepo();
        statisticsRepo = new StatisticsRepo();
        notificationPort = new NotificationPort();

        createGameUseCase = new CreateGameUseCase(gameRepo, gameParticipantRepo);
        joinGameUseCase = new JoinGameUseCase(gameRepo, gameParticipantRepo, notificationPort);
        submitAttemptUseCase = new SubmitAttemptUseCase(roundRepo, roundParticipantRepo, revealRepo, notificationPort);

        GameModeRuleRegistry ruleRegistry = new GameModeRuleRegistry();
        DebtCalculationEngine debtEngine = new DebtCalculationEngine();
        TieBreakResolver tieBreakResolver = new TieBreakResolver();
        BillDivisionEngine billDivisionEngine = new BillDivisionEngine();

        resolveRoundUseCase = new ResolveRoundUseCase(
                roundRepo,
                roundParticipantRepo,
                gameRepo,
                gameParticipantRepo,
                revealRepo,
                tieBreakRepo,
                debtMovementRepo,
                statisticsRepo,
                notificationPort,
                ruleRegistry,
                debtEngine,
                tieBreakResolver
        );

        processTieBreakVoteUseCase = new ProcessTieBreakVoteUseCase(
                tieBreakRepo,
                roundRepo,
                roundParticipantRepo,
                gameRepo,
                gameParticipantRepo,
                debtMovementRepo,
                statisticsRepo,
                notificationPort,
                tieBreakResolver,
                billDivisionEngine,
                ruleRegistry
        );
    }

    @Test
    @DisplayName("CreateGameUseCase: Crea partida en LOBBY y registra al host como PLAYER activo (Sección 6, 9)")
    void testCreateGameSuccessfully() {
        var command = new CreateGameUseCase.CreateGameCommand("HostUser1", 4000L, GameMode.ULTIMO_PIERDE, 10);
        var result = createGameUseCase.execute(command);

        assertNotNull(result.gameId());
        assertNotNull(result.gameCode());
        assertEquals("HostUser1", result.hostId());
        assertEquals(4000L, result.drinkPrice());
        assertEquals(GameMode.ULTIMO_PIERDE, result.mode());
        assertEquals(10, result.maxPlayers());

        Game savedGame = gameRepo.findById(result.gameId()).orElseThrow();
        assertEquals(GameState.LOBBY, savedGame.getState());

        List<GameParticipant> participants = gameParticipantRepo.findByGameId(result.gameId());
        assertEquals(1, participants.size());
        assertEquals("HostUser1", participants.get(0).getUserId());
        assertEquals(ParticipantRole.PLAYER, participants.get(0).getRole());
        assertEquals(ParticipantState.ACTIVE, participants.get(0).getParticipantState());
    }

    @Test
    @DisplayName("JoinGameUseCase: Unirse en LOBBY asigna rol PLAYER; unirse en IN_PROGRESS asigna SPECTATOR (Sección 8)")
    void testJoinGameRolesDependingOnGameState() {
        // Crear juego
        var createResult = createGameUseCase.execute(
                new CreateGameUseCase.CreateGameCommand("Host1", 4000L, GameMode.ULTIMO_PIERDE, 10)
        );

        // Jugador 2 se une en LOBBY -> entra como PLAYER
        var joinLobby = joinGameUseCase.execute(new JoinGameUseCase.JoinGameCommand(createResult.gameCode(), "User2"));
        assertEquals(ParticipantRole.PLAYER, joinLobby.role());

        // Iniciar ronda -> Game pasa a IN_PROGRESS
        Game game = gameRepo.findById(createResult.gameId()).orElseThrow();
        game.startRound();
        gameRepo.save(game);

        // Jugador 3 se une en IN_PROGRESS -> entra como SPECTATOR (Sección 8)
        var joinInProgress = joinGameUseCase.execute(new JoinGameUseCase.JoinGameCommand(createResult.gameCode(), "User3"));
        assertEquals(ParticipantRole.SPECTATOR, joinInProgress.role());

        // Intentar unirse a una partida FINISHED debe ser rechazado (Sección 11)
        game.roundFinished();
        game.finishGame();
        gameRepo.save(game);

        assertThrows(InvalidGameStateException.class, () ->
                joinGameUseCase.execute(new JoinGameUseCase.JoinGameCommand(createResult.gameCode(), "User4"))
        );
    }

    @Test
    @DisplayName("SubmitAttemptUseCase: Registra intento de revelación y avanza ronda a VALIDATING (Sección 19, 24)")
    void testSubmitAttemptSuccessfully() {
        // Configurar juego y ronda
        Round round = new Round("R1", "G1", GameMode.ULTIMO_PIERDE, 4000L, RoundType.NORMAL);
        round.startRevealing();
        roundRepo.save(round);

        RoundParticipant p1 = new RoundParticipant("RP1", "R1", "Player1", "Player1", 1);
        roundParticipantRepo.save(p1);

        var command = new SubmitAttemptUseCase.SubmitAttemptCommand("R1", "Player1", 25, "s3://photo.jpg", null);
        var result = submitAttemptUseCase.execute(command);

        assertNotNull(result.revealId());
        assertNotNull(result.attemptId());
        assertEquals(1, result.attemptNumber());
        assertEquals(25, result.proposedNumber());
        assertEquals(AttemptStatus.PENDING, result.attemptStatus());

        Round updatedRound = roundRepo.findById("R1").orElseThrow();
        assertEquals(RoundState.VALIDATING, updatedRound.getState());
    }

    @Test
    @DisplayName("ResolveRoundUseCase: Resuelve ronda sin empate, calcula deudas, finaliza y promueve espectadores (Sección 8, 70)")
    void testResolveRoundNormalWithSpectatorPromotion() {
        // 1. Crear juego
        Game game = new Game("G1", "CODE12", "Host1", 10, 4000L, GameMode.ULTIMO_PIERDE);
        game.startRound();
        gameRepo.save(game);

        // 2. Participantes del juego: 4 jugadores y 1 espectador que se unió durante la ronda
        gameParticipantRepo.save(GameParticipant.createPlayer("GP1", "G1", "P1"));
        gameParticipantRepo.save(GameParticipant.createPlayer("GP2", "G1", "P2"));
        gameParticipantRepo.save(GameParticipant.createPlayer("GP3", "G1", "P3"));
        gameParticipantRepo.save(GameParticipant.createPlayer("GP4", "G1", "P4"));
        GameParticipant spectator = GameParticipant.createSpectator("GP5", "G1", "P5");
        gameParticipantRepo.save(spectator);

        // 3. Ronda activa
        Round round = new Round("R1", "G1", GameMode.ULTIMO_PIERDE, 4000L, RoundType.NORMAL);
        round.startRevealing();
        round.startValidating();
        roundRepo.save(round);

        // 4. Participantes de la ronda
        roundParticipantRepo.save(new RoundParticipant("RP1", "R1", "P1", "P1", 1));
        roundParticipantRepo.save(new RoundParticipant("RP2", "R1", "P2", "P2", 2));
        roundParticipantRepo.save(new RoundParticipant("RP3", "R1", "P3", "P3", 3));
        roundParticipantRepo.save(new RoundParticipant("RP4", "R1", "P4", "P4", 4));

        // 5. Revelaciones aprobadas: P1=17 (pierde), P2=25, P3=30, P4=40
        Reveal rev1 = new Reveal("REV1", "R1", "P1"); rev1.approve(17); revealRepo.save(rev1);
        Reveal rev2 = new Reveal("REV2", "R1", "P2"); rev2.approve(25); revealRepo.save(rev2);
        Reveal rev3 = new Reveal("REV3", "R1", "P3"); rev3.approve(30); revealRepo.save(rev3);
        Reveal rev4 = new Reveal("REV4", "R1", "P4"); rev4.approve(40); revealRepo.save(rev4);

        // Ejecutar resolución
        var resolveResult = resolveRoundUseCase.execute(new ResolveRoundUseCase.ResolveRoundCommand("R1"));

        assertFalse(resolveResult.hasTie());
        assertEquals(RoundState.FINISHED, resolveResult.roundState());
        assertEquals(List.of("P1"), resolveResult.confirmedLosers());
        assertEquals(3, resolveResult.winners().size());

        // Deudas: P1 paga a P2, P3, P4 4000 cada uno (total 12.000 COP)
        assertEquals(3, resolveResult.debtMovements().size());
        long totalDebt = resolveResult.debtMovements().stream().mapToLong(DebtMovement::getAmount).sum();
        assertEquals(12_000L, totalDebt);

        // Game volvió a LOBBY
        Game updatedGame = gameRepo.findById("G1").orElseThrow();
        assertEquals(GameState.LOBBY, updatedGame.getState());

        // Espectador fue promovido automáticamente a PLAYER (Sección 8)
        GameParticipant updatedSpectator = gameParticipantRepo.findByGameIdAndUserId("G1", "P5").orElseThrow();
        assertTrue(updatedSpectator.isPlayer());
        assertFalse(updatedSpectator.isSpectator());
    }

    @Test
    @DisplayName("ResolveRoundUseCase: Detecta empate en el límite y genera entidad TieBreak (Sección 32, 33)")
    void testResolveRoundWithTieInitiatesTieBreak() {
        Game game = new Game("G1", "CODE12", "Host1", 10, 4000L, GameMode.ULTIMO_PIERDE);
        game.startRound();
        gameRepo.save(game);

        Round round = new Round("R1", "G1", GameMode.ULTIMO_PIERDE, 4000L, RoundType.NORMAL);
        round.startRevealing();
        round.startValidating();
        roundRepo.save(round);

        roundParticipantRepo.save(new RoundParticipant("RP1", "R1", "P1", "P1", 1));
        roundParticipantRepo.save(new RoundParticipant("RP2", "R1", "P2", "P2", 2));
        roundParticipantRepo.save(new RoundParticipant("RP3", "R1", "P3", "P3", 3));

        // Empate en 20 entre P1 y P2 para 1 loser slot
        Reveal rev1 = new Reveal("REV1", "R1", "P1"); rev1.approve(20); revealRepo.save(rev1);
        Reveal rev2 = new Reveal("REV2", "R1", "P2"); rev2.approve(20); revealRepo.save(rev2);
        Reveal rev3 = new Reveal("REV3", "R1", "P3"); rev3.approve(50); revealRepo.save(rev3);

        var result = resolveRoundUseCase.execute(new ResolveRoundUseCase.ResolveRoundCommand("R1"));

        assertTrue(result.hasTie());
        assertEquals(RoundState.TIE_BREAK, result.roundState());
        assertNotNull(result.tieBreakId());

        TieBreak tieBreak = tieBreakRepo.findById(result.tieBreakId()).orElseThrow();
        assertEquals(TieBreakStatus.PENDING, tieBreak.getStatus());
        assertEquals(2, tieBreak.getParticipants().size());
    }

    @Test
    @DisplayName("ProcessTieBreakVoteUseCase: Voto unánime por DIVIDIR_CUENTA liquida y finaliza la ronda (Sección 33, 37)")
    void testProcessTieBreakVoteDividirCuenta() {
        Game game = new Game("G1", "CODE12", "Host1", 10, 4000L, GameMode.ULTIMO_PIERDE);
        game.startRound();
        gameRepo.save(game);

        Round round = new Round("R1", "G1", GameMode.ULTIMO_PIERDE, 4000L, RoundType.NORMAL);
        round.startRevealing();
        round.startValidating();
        round.startResolving();
        round.startTieBreak();
        roundRepo.save(round);

        roundParticipantRepo.save(new RoundParticipant("RP1", "R1", "P1", "P1", 1));
        roundParticipantRepo.save(new RoundParticipant("RP2", "R1", "P2", "P2", 2));
        roundParticipantRepo.save(new RoundParticipant("RP3", "R1", "P3", "P3", 3));

        TieBreak tieBreak = new TieBreak("TB1", "R1", "BOUNDARY_TIE", 1, false);
        tieBreak.addParticipant(new TieBreakParticipant("TB1", "P1"));
        tieBreak.addParticipant(new TieBreakParticipant("TB1", "P2"));
        tieBreakRepo.save(tieBreak);

        // P1 vota DIVIDIR_CUENTA
        processTieBreakVoteUseCase.execute(
                new ProcessTieBreakVoteUseCase.ProcessTieBreakVoteCommand("TB1", "P1", TieBreakResolutionMode.DIVIDIR_CUENTA)
        );

        // P2 vota DIVIDIR_CUENTA (consenso unánime)
        var result = processTieBreakVoteUseCase.execute(
                new ProcessTieBreakVoteUseCase.ProcessTieBreakVoteCommand("TB1", "P2", TieBreakResolutionMode.DIVIDIR_CUENTA)
        );

        assertEquals(TieBreakStatus.RESOLVED, result.status());
        assertEquals(TieBreakResolutionMode.DIVIDIR_CUENTA, result.resolvedMode());
        assertFalse(result.debtMovements().isEmpty());

        Round updatedRound = roundRepo.findById("R1").orElseThrow();
        assertEquals(RoundState.FINISHED, updatedRound.getState());

        Game updatedGame = gameRepo.findById("G1").orElseThrow();
        assertEquals(GameState.LOBBY, updatedGame.getState());
    }

    @Test
    @DisplayName("ProcessTieBreakVoteUseCase: Voto unánime por NUEVA_RONDA crea TIE_BREAK_ROUND (Sección 33, 34, 35)")
    void testProcessTieBreakVoteNuevaRonda() {
        Game game = new Game("G1", "CODE12", "Host1", 10, 4000L, GameMode.ULTIMO_PIERDE);
        game.startRound();
        gameRepo.save(game);

        gameParticipantRepo.save(GameParticipant.createPlayer("GP1", "G1", "P1"));
        gameParticipantRepo.save(GameParticipant.createPlayer("GP2", "G1", "P2"));
        gameParticipantRepo.save(GameParticipant.createPlayer("GP3", "G1", "P3"));

        Round round = new Round("R1", "G1", GameMode.ULTIMO_PIERDE, 4000L, RoundType.NORMAL);
        round.startRevealing();
        round.startValidating();
        round.startResolving();
        round.startTieBreak();
        roundRepo.save(round);

        roundParticipantRepo.save(new RoundParticipant("RP1", "R1", "P1", "P1", 1));
        roundParticipantRepo.save(new RoundParticipant("RP2", "R1", "P2", "P2", 2));
        roundParticipantRepo.save(new RoundParticipant("RP3", "R1", "P3", "P3", 3));

        TieBreak tieBreak = new TieBreak("TB1", "R1", "BOUNDARY_TIE", 1, false);
        tieBreak.addParticipant(new TieBreakParticipant("TB1", "P1"));
        tieBreak.addParticipant(new TieBreakParticipant("TB1", "P2"));
        tieBreakRepo.save(tieBreak);

        // P1 y P2 votan NUEVA_RONDA
        processTieBreakVoteUseCase.execute(
                new ProcessTieBreakVoteUseCase.ProcessTieBreakVoteCommand("TB1", "P1", TieBreakResolutionMode.NUEVA_RONDA)
        );
        var result = processTieBreakVoteUseCase.execute(
                new ProcessTieBreakVoteUseCase.ProcessTieBreakVoteCommand("TB1", "P2", TieBreakResolutionMode.NUEVA_RONDA)
        );

        assertEquals(TieBreakStatus.RESOLVED, result.status());
        assertEquals(TieBreakResolutionMode.NUEVA_RONDA, result.resolvedMode());
        assertNotNull(result.tieBreakRoundId());

        Round newRound = roundRepo.findById(result.tieBreakRoundId()).orElseThrow();
        assertEquals(RoundType.TIE_BREAK_ROUND, newRound.getRoundType());
        assertEquals(RoundState.STARTING, newRound.getState());

        List<RoundParticipant> newParticipants = roundParticipantRepo.findByRoundId(newRound.getId());
        assertEquals(3, newParticipants.size()); // Todos los jugadores participan físicamente (Sección 34)
    }
}
