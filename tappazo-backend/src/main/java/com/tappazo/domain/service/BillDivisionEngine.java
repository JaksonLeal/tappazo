package com.tappazo.domain.service;

import com.tappazo.domain.model.DebtMovement;
import com.tappazo.domain.model.DebtReason;
import com.tappazo.domain.model.PlayerDebtPortion;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Motor de cálculo para cuenta dividida (FASE 3 - Secciones 36, 37, 46).
 * Aplica la fórmula definitiva de porcentajes sobre la CUENTA = (N - TOTAL_LOSER_SLOTS) * drinkPrice,
 * con división entera en pesos COP.
 */
public class BillDivisionEngine {

    private final LoserPortionCalculator portionCalculator;

    public BillDivisionEngine() {
        this.portionCalculator = new LoserPortionCalculator();
    }

    public BillDivisionEngine(LoserPortionCalculator portionCalculator) {
        this.portionCalculator = Objects.requireNonNull(portionCalculator, "portionCalculator must not be null");
    }

    public record DividedBillSummary(
            long totalAccount,
            List<PlayerDebtPortion> portions,
            long totalDistributedAmount,
            long discardedRemainder,
            List<DebtMovement> debtMovements
    ) {}

    /**
     * Calcula la división de la cuenta y genera los movimientos de deuda correspondientes.
     *
     * @param gameId identificador de la partida
     * @param roundId identificador de la ronda
     * @param totalPlayers cantidad total de participantes (N)
     * @param totalLoserSlots número total de puestos de perdedor
     * @param confirmedNonTiedLosers IDs de perdedores confirmados sin empate
     * @param tiedCandidates IDs de los empatados que compiten por los slots restantes
     * @param remainingLoserSlots cantidad de slots por los que compiten
     * @param winners lista de ganadores que deben recibir el pago
     * @param drinkPrice precio de la bebida en COP
     * @return DividedBillSummary con porcentajes, montos en COP y movimientos generados
     */
    public DividedBillSummary calculateDividedBill(String gameId,
                                                  String roundId,
                                                  int totalPlayers,
                                                  int totalLoserSlots,
                                                  List<String> confirmedNonTiedLosers,
                                                  List<String> tiedCandidates,
                                                  int remainingLoserSlots,
                                                  List<String> winners,
                                                  long drinkPrice) {
        Objects.requireNonNull(gameId, "gameId must not be null");
        Objects.requireNonNull(roundId, "roundId must not be null");
        Objects.requireNonNull(confirmedNonTiedLosers, "confirmedNonTiedLosers must not be null");
        Objects.requireNonNull(tiedCandidates, "tiedCandidates must not be null");
        Objects.requireNonNull(winners, "winners must not be null");

        long totalAccount = portionCalculator.calculateAccount(totalPlayers, totalLoserSlots, drinkPrice);

        List<PlayerDebtPortion> portions = portionCalculator.calculatePortions(
                totalPlayers, totalLoserSlots, confirmedNonTiedLosers, tiedCandidates, remainingLoserSlots, drinkPrice
        );

        long totalDistributed = 0L;
        List<DebtMovement> movements = new ArrayList<>();
        int winnersCount = winners.size();

        for (PlayerDebtPortion portion : portions) {
            long playerAmount = portion.getAmount();
            totalDistributed += playerAmount;

            if (winnersCount > 0 && playerAmount > 0) {
                long perWinnerAmount = playerAmount / winnersCount;
                if (perWinnerAmount > 0) {
                    for (String winnerId : winners) {
                        movements.add(new DebtMovement(
                                UUID.randomUUID().toString(),
                                gameId,
                                roundId,
                                portion.getPlayerId(),
                                winnerId,
                                perWinnerAmount,
                                DebtReason.TIE_BREAK
                        ));
                    }
                }
            }
        }

        long discardedRemainder = Math.max(0, totalAccount - totalDistributed);

        return new DividedBillSummary(
                totalAccount,
                portions,
                totalDistributed,
                discardedRemainder,
                movements
        );
    }
}
