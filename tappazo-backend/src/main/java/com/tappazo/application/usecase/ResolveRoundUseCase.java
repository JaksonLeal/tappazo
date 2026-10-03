package com.tappazo.application.usecase;

import com.tappazo.application.port.out.*;
import com.tappazo.domain.exception.DomainException;
import com.tappazo.domain.model.*;
import com.tappazo.domain.rule.GameModeRule;
import com.tappazo.domain.rule.GameModeRuleRegistry;
import com.tappazo.domain.service.DebtCalculationEngine;
import com.tappazo.domain.service.TieBreakResolver;

import java.util.*;

/**
 * Caso de uso: Resolver Ronda (Sección 28, 29, 30, 36, 44, 48, 49, 70).
 * Aplica la modalidad de juego, verifica desempates, calcula obligaciones económicas y
 * promueve espectadores a jugadores al finalizar.
 */
public class ResolveRoundUseCase {

    private final RoundRepositoryPort roundRepository;
    private final RoundParticipantRepositoryPort roundParticipantRepository;
    private final GameRepositoryPort gameRepository;
    private final GameParticipantRepositoryPort gameParticipantRepository;
    private final RevealRepositoryPort revealRepository;
    private final TieBreakRepositoryPort tieBreakRepository;
    private final DebtMovementRepositoryPort debtMovementRepository;
    private final PlayerStatisticsRepositoryPort statisticsRepository;
    private final GameNotificationPort notificationPort;

    private final GameModeRuleRegistry ruleRegistry;
    private final DebtCalculationEngine debtEngine;
    private final TieBreakResolver tieBreakResolver;

    public ResolveRoundUseCase(RoundRepositoryPort roundRepository,
                               RoundParticipantRepositoryPort roundParticipantRepository,
                               GameRepositoryPort gameRepository,
                               GameParticipantRepositoryPort gameParticipantRepository,
                               RevealRepositoryPort revealRepository,
                               TieBreakRepositoryPort tieBreakRepository,
                               DebtMovementRepositoryPort debtMovementRepository,
                               PlayerStatisticsRepositoryPort statisticsRepository,
                               GameNotificationPort notificationPort,
                               GameModeRuleRegistry ruleRegistry,
                               DebtCalculationEngine debtEngine,
                               TieBreakResolver tieBreakResolver) {
        this.roundRepository = Objects.requireNonNull(roundRepository, "roundRepository must not be null");
        this.roundParticipantRepository = Objects.requireNonNull(roundParticipantRepository, "roundParticipantRepository must not be null");
        this.gameRepository = Objects.requireNonNull(gameRepository, "gameRepository must not be null");
        this.gameParticipantRepository = Objects.requireNonNull(gameParticipantRepository, "gameParticipantRepository must not be null");
        this.revealRepository = Objects.requireNonNull(revealRepository, "revealRepository must not be null");
        this.tieBreakRepository = Objects.requireNonNull(tieBreakRepository, "tieBreakRepository must not be null");
        this.debtMovementRepository = Objects.requireNonNull(debtMovementRepository, "debtMovementRepository must not be null");
        this.statisticsRepository = Objects.requireNonNull(statisticsRepository, "statisticsRepository must not be null");
        this.notificationPort = Objects.requireNonNull(notificationPort, "notificationPort must not be null");
        this.ruleRegistry = Objects.requireNonNull(ruleRegistry, "ruleRegistry must not be null");
        this.debtEngine = Objects.requireNonNull(debtEngine, "debtEngine must not be null");
        this.tieBreakResolver = Objects.requireNonNull(tieBreakResolver, "tieBreakResolver must not be null");
    }

    public record ResolveRoundCommand(String roundId) {}

    public record ResolveRoundResult(
            String roundId,
            RoundState roundState,
            boolean hasTie,
            String tieBreakId,
            List<String> confirmedLosers,
            List<String> winners,
            List<DebtMovement> debtMovements
    ) {}

    public ResolveRoundResult execute(ResolveRoundCommand command) {
        Objects.requireNonNull(command, "command must not be null");

        Round round = roundRepository.findById(command.roundId())
                .orElseThrow(() -> new DomainException("Ronda no encontrada: " + command.roundId()));

        Game game = gameRepository.findById(round.getGameId())
                .orElseThrow(() -> new DomainException("Partida no encontrada: " + round.getGameId()));

        List<RoundParticipant> participants = roundParticipantRepository.findByRoundId(round.getId());
        List<Reveal> reveals = revealRepository.findByRoundId(round.getId());

        Map<String, Reveal> revealMap = new HashMap<>();
        for (Reveal r : reveals) {
            revealMap.put(r.getPlayerId(), r);
        }

        // Construir resultados individuales de los jugadores (Sección 30)
        List<PlayerResult> playerResults = new ArrayList<>();
        for (RoundParticipant rp : participants) {
            if (rp.isRemovedByDisconnection()) {
                playerResults.add(PlayerResult.automatic(rp.getUserId(), DebtReason.STAND_BY_REMOVAL));
            } else if (rp.isRevealInvalid()) {
                playerResults.add(PlayerResult.automatic(rp.getUserId(), DebtReason.REVEAL_INVALID));
            } else {
                Reveal rev = revealMap.get(rp.getUserId());
                if (rev != null && rev.getStatus() == RevealStatus.APPROVED && rev.getOfficialNumber() != null) {
                    playerResults.add(PlayerResult.normal(rp.getUserId(), rev.getOfficialNumber()));
                } else if (rev != null && rev.getStatus() == RevealStatus.REVEAL_INVALID) {
                    playerResults.add(PlayerResult.automatic(rp.getUserId(), DebtReason.REVEAL_INVALID));
                } else {
                    throw new DomainException("El jugador " + rp.getUserId() + " aún no tiene resultado oficial validado");
                }
            }
        }

        // Aplicar la regla de la modalidad correspondiente (Sección 28)
        GameModeRule modeRule = ruleRegistry.getRule(round.getMode());
        GameResult gameResult = modeRule.resolve(playerResults);

        // Caso con Empate (Sección 32, 33)
        if (gameResult.hasTie()) {
            if (round.getState() != RoundState.RESOLVING) {
                round.startResolving();
            }
            round.startTieBreak();
            roundRepository.save(round);

            String tieBreakId = UUID.randomUUID().toString();
            TieBreak tieBreak = tieBreakResolver.initiateTieBreak(tieBreakId, round.getId(), gameResult);
            tieBreakRepository.save(tieBreak);

            notificationPort.notifyTieBreakStarted(game.getId(), tieBreak);

            return new ResolveRoundResult(
                    round.getId(),
                    round.getState(),
                    true,
                    tieBreak.getId(),
                    gameResult.getConfirmedLosers(),
                    gameResult.getWinners(),
                    List.of()
            );
        }

        // Caso sin Empate: Resolución definitiva
        if (round.getState() != RoundState.RESOLVING) {
            round.startResolving();
        }

        // Cálculo económico de la ronda (Sección 36, 46-49)
        var debtSummary = debtEngine.calculateRoundDebt(
                game.getId(),
                round.getId(),
                round.getMode(),
                gameResult.getConfirmedLosers(),
                gameResult.getWinners(),
                round.getDrinkPrice()
        );

        debtMovementRepository.saveAll(debtSummary.debtMovements());

        // Actualizar estados en RoundParticipant
        Set<String> loserSet = new HashSet<>(gameResult.getConfirmedLosers());
        Set<String> winnerSet = new HashSet<>(gameResult.getWinners());

        for (RoundParticipant rp : participants) {
            if (loserSet.contains(rp.getUserId())) {
                rp.markConfirmedLoser();
                updateStats(rp.getUserId(), false, debtSummary.totalPerLoser(), 0);
            } else if (winnerSet.contains(rp.getUserId())) {
                rp.markWinner();
                updateStats(rp.getUserId(), true, 0, round.getDrinkPrice());
            }
        }
        roundParticipantRepository.saveAll(participants);

        // Finalizar ronda y retornar Game a LOBBY (Sección 11, 70)
        round.finish();
        roundRepository.save(round);

        game.roundFinished();
        gameRepository.save(game);

        // Promoción automática de espectadores a jugadores (Sección 8, 70)
        List<GameParticipant> gameParticipants = gameParticipantRepository.findByGameId(game.getId());
        for (GameParticipant gp : gameParticipants) {
            if (gp.isSpectator()) {
                gp.promoteToPlayer();
            }
        }
        gameParticipantRepository.saveAll(gameParticipants);

        notificationPort.notifyRoundResolved(game.getId(), round, gameResult, debtSummary.debtMovements());

        return new ResolveRoundResult(
                round.getId(),
                round.getState(),
                false,
                null,
                gameResult.getConfirmedLosers(),
                gameResult.getWinners(),
                debtSummary.debtMovements()
        );
    }

    private void updateStats(String userId, boolean won, long spent, long received) {
        PlayerStatistics stats = statisticsRepository.findByUserId(userId)
                .orElseGet(() -> new PlayerStatistics(userId));
        stats.recordRoundResult(won, spent, received);
        statisticsRepository.save(stats);
    }
}
