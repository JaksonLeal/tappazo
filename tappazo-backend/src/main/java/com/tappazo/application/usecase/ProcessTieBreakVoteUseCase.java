package com.tappazo.application.usecase;

import com.tappazo.application.port.out.*;
import com.tappazo.domain.exception.DomainException;
import com.tappazo.domain.model.*;
import com.tappazo.domain.rule.GameModeRule;
import com.tappazo.domain.rule.GameModeRuleRegistry;
import com.tappazo.domain.service.BillDivisionEngine;
import com.tappazo.domain.service.TieBreakResolver;

import java.util.*;

/**
 * Caso de uso: Procesar Voto de Desempate (Sección 33, 34, 35, 37, 61).
 * Gestiona la votación de los participantes empatados, resolución automática en caso todos empatados,
 * o ejecución de NUEVA_RONDA / DIVIDIR_CUENTA.
 */
public class ProcessTieBreakVoteUseCase {

    private final TieBreakRepositoryPort tieBreakRepository;
    private final RoundRepositoryPort roundRepository;
    private final RoundParticipantRepositoryPort roundParticipantRepository;
    private final GameRepositoryPort gameRepository;
    private final GameParticipantRepositoryPort gameParticipantRepository;
    private final DebtMovementRepositoryPort debtMovementRepository;
    private final PlayerStatisticsRepositoryPort statisticsRepository;
    private final GameNotificationPort notificationPort;

    private final TieBreakResolver tieBreakResolver;
    private final BillDivisionEngine billDivisionEngine;
    private final GameModeRuleRegistry ruleRegistry;

    public ProcessTieBreakVoteUseCase(TieBreakRepositoryPort tieBreakRepository,
                                     RoundRepositoryPort roundRepository,
                                     RoundParticipantRepositoryPort roundParticipantRepository,
                                     GameRepositoryPort gameRepository,
                                     GameParticipantRepositoryPort gameParticipantRepository,
                                     DebtMovementRepositoryPort debtMovementRepository,
                                     PlayerStatisticsRepositoryPort statisticsRepository,
                                     GameNotificationPort notificationPort,
                                     TieBreakResolver tieBreakResolver,
                                     BillDivisionEngine billDivisionEngine,
                                     GameModeRuleRegistry ruleRegistry) {
        this.tieBreakRepository = Objects.requireNonNull(tieBreakRepository, "tieBreakRepository must not be null");
        this.roundRepository = Objects.requireNonNull(roundRepository, "roundRepository must not be null");
        this.roundParticipantRepository = Objects.requireNonNull(roundParticipantRepository, "roundParticipantRepository must not be null");
        this.gameRepository = Objects.requireNonNull(gameRepository, "gameRepository must not be null");
        this.gameParticipantRepository = Objects.requireNonNull(gameParticipantRepository, "gameParticipantRepository must not be null");
        this.debtMovementRepository = Objects.requireNonNull(debtMovementRepository, "debtMovementRepository must not be null");
        this.statisticsRepository = Objects.requireNonNull(statisticsRepository, "statisticsRepository must not be null");
        this.notificationPort = Objects.requireNonNull(notificationPort, "notificationPort must not be null");
        this.tieBreakResolver = Objects.requireNonNull(tieBreakResolver, "tieBreakResolver must not be null");
        this.billDivisionEngine = Objects.requireNonNull(billDivisionEngine, "billDivisionEngine must not be null");
        this.ruleRegistry = Objects.requireNonNull(ruleRegistry, "ruleRegistry must not be null");
    }

    public record ProcessTieBreakVoteCommand(
            String tieBreakId,
            String playerId,
            TieBreakResolutionMode decision
    ) {}

    public record ProcessTieBreakVoteResult(
            String tieBreakId,
            TieBreakStatus status,
            TieBreakResolutionMode resolvedMode,
            String selectedDeciderId,
            String tieBreakRoundId,
            List<DebtMovement> debtMovements
    ) {}

    public ProcessTieBreakVoteResult execute(ProcessTieBreakVoteCommand command) {
        Objects.requireNonNull(command, "command must not be null");

        TieBreak tieBreak = tieBreakRepository.findById(command.tieBreakId())
                .orElseThrow(() -> new DomainException("Desempate no encontrado: " + command.tieBreakId()));

        if (tieBreak.getStatus() != TieBreakStatus.PENDING) {
            throw new DomainException("El desempate ya fue resuelto previamente");
        }

        Round sourceRound = roundRepository.findById(tieBreak.getSourceRoundId())
                .orElseThrow(() -> new DomainException("Ronda origen no encontrada: " + tieBreak.getSourceRoundId()));

        Game game = gameRepository.findById(sourceRound.getGameId())
                .orElseThrow(() -> new DomainException("Partida no encontrada: " + sourceRound.getGameId()));

        // Registrar o actualizar voto del participante
        TieBreakParticipant participant = tieBreak.getParticipants().stream()
                .filter(p -> p.getPlayerId().equals(command.playerId()))
                .findFirst()
                .orElseThrow(() -> new DomainException("El jugador " + command.playerId() + " no es participante de este desempate"));

        participant.castDecision(command.decision());

        // Verificar si todos los empatados han votado
        Map<String, TieBreakResolutionMode> votes = new HashMap<>();
        boolean allVoted = true;
        for (TieBreakParticipant p : tieBreak.getParticipants()) {
            if (p.getDecision() == null) {
                allVoted = false;
            } else {
                votes.put(p.getPlayerId(), p.getDecision());
            }
        }

        if (!allVoted && !tieBreak.isAllTiedCase()) {
            tieBreakRepository.save(tieBreak);
            return new ProcessTieBreakVoteResult(tieBreak.getId(), tieBreak.getStatus(), null, null, null, List.of());
        }

        // Obtener jugadores no empatados de la ronda (potenciales árbitros)
        List<RoundParticipant> roundParticipants = roundParticipantRepository.findByRoundId(sourceRound.getId());
        Set<String> tiedPlayerIds = new HashSet<>(votes.keySet());
        List<String> nonTiedPlayerIds = new ArrayList<>();
        List<String> confirmedLoserIds = new ArrayList<>();
        List<String> confirmedWinnerIds = new ArrayList<>();

        for (RoundParticipant rp : roundParticipants) {
            if (!tiedPlayerIds.contains(rp.getUserId())) {
                nonTiedPlayerIds.add(rp.getUserId());
                if (rp.isConfirmedLoser()) {
                    confirmedLoserIds.add(rp.getUserId());
                } else {
                    confirmedWinnerIds.add(rp.getUserId());
                }
            }
        }

        boolean isSecondAllTied = sourceRound.getRoundType() == RoundType.TIE_BREAK_ROUND;
        TieBreakResolutionMode resolvedMode = tieBreakResolver.resolveTieBreakMode(
                tieBreak,
                votes,
                nonTiedPlayerIds,
                isSecondAllTied
        );

        List<DebtMovement> generatedMovements = new ArrayList<>();

        if (resolvedMode == TieBreakResolutionMode.DIVIDIR_CUENTA) {
            GameModeRule rule = ruleRegistry.getRule(sourceRound.getMode());
            int totalLoserSlots = rule.calculateLoserSlots(roundParticipants.size());

            var dividedBill = billDivisionEngine.calculateDividedBill(
                    game.getId(),
                    sourceRound.getId(),
                    roundParticipants.size(),
                    totalLoserSlots,
                    confirmedLoserIds,
                    new ArrayList<>(tiedPlayerIds),
                    tieBreak.getAffectedLoserSlots(),
                    confirmedWinnerIds,
                    sourceRound.getDrinkPrice()
            );

            debtMovementRepository.saveAll(dividedBill.debtMovements());
            generatedMovements.addAll(dividedBill.debtMovements());

            // Actualizar estadísticas de los jugadores
            for (var portion : dividedBill.portions()) {
                updateStats(portion.getPlayerId(), false, portion.getAmount(), 0);
            }
            long drinkPrice = sourceRound.getDrinkPrice();
            for (String winnerId : confirmedWinnerIds) {
                updateStats(winnerId, true, 0, drinkPrice);
            }

            sourceRound.finish();
            roundRepository.save(sourceRound);

            game.roundFinished();
            gameRepository.save(game);

            // Auto-promover espectadores
            List<GameParticipant> gps = gameParticipantRepository.findByGameId(game.getId());
            for (GameParticipant gp : gps) {
                if (gp.isSpectator()) gp.promoteToPlayer();
            }
            gameParticipantRepository.saveAll(gps);

        } else if (resolvedMode == TieBreakResolutionMode.NUEVA_RONDA) {
            // Crear nueva ronda de tipo TIE_BREAK_ROUND (Sección 34, 35)
            String newRoundId = UUID.randomUUID().toString();
            Round tieBreakRound = new Round(
                    newRoundId,
                    game.getId(),
                    sourceRound.getMode(),
                    sourceRound.getDrinkPrice(),
                    RoundType.TIE_BREAK_ROUND
            );
            roundRepository.save(tieBreakRound);

            // Registrar participantes físicos de la nueva ronda (todos los jugadores activos del juego)
            List<GameParticipant> players = gameParticipantRepository.findByGameId(game.getId());
            int order = 1;
            List<RoundParticipant> newParticipants = new ArrayList<>();
            for (GameParticipant p : players) {
                if (p.isPlayer() && p.isActive()) {
                    RoundParticipant rp = new RoundParticipant(
                            UUID.randomUUID().toString(),
                            newRoundId,
                            p.getUserId(),
                            p.getUserId(), // Nickname snapshot
                            order++
                    );
                    newParticipants.add(rp);
                }
            }
            roundParticipantRepository.saveAll(newParticipants);

            tieBreak.resolve(resolvedMode, tieBreak.getSelectedDeciderId(), newRoundId);
        }

        tieBreakRepository.save(tieBreak);
        notificationPort.notifyTieBreakResolved(game.getId(), tieBreak);

        return new ProcessTieBreakVoteResult(
                tieBreak.getId(),
                tieBreak.getStatus(),
                resolvedMode,
                tieBreak.getSelectedDeciderId(),
                tieBreak.getTieBreakRoundId(),
                generatedMovements
        );
    }

    private void updateStats(String userId, boolean won, long spent, long received) {
        PlayerStatistics stats = statisticsRepository.findByUserId(userId)
                .orElseGet(() -> new PlayerStatistics(userId));
        stats.recordRoundResult(won, spent, received);
        statisticsRepository.save(stats);
    }
}
