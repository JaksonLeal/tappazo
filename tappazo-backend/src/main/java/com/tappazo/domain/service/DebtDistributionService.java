package com.tappazo.domain.service;

import com.tappazo.domain.model.DebtMovement;
import com.tappazo.domain.model.DebtReason;
import com.tappazo.domain.model.PlayerDebtPortion;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Servicio de distribución de deudas (Sección 36, 47, 48, 49, 64).
 * Genera los movimientos de deuda respetando la fórmula de la CUENTA,
 * división entera con truncamiento y descarte de sobrante en COP.
 */
public class DebtDistributionService {

    /**
     * Distribución económica normal (Sección 49).
     * 1. Asignar una bebida completa a cada perdedor para cubrir un ganador.
     * 2. Identificar ganadores restantes (W - L).
     * 3. Dividir las bebidas restantes entre los perdedores (división entera).
     * 4. Ignorar el sobrante.
     */
    public List<DebtMovement> distributeNormal(String gameId,
                                              String roundId,
                                              List<String> confirmedLosers,
                                              List<String> winners,
                                              long drinkPrice,
                                              DebtReason reason) {
        Objects.requireNonNull(gameId, "gameId must not be null");
        Objects.requireNonNull(roundId, "roundId must not be null");
        Objects.requireNonNull(confirmedLosers, "confirmedLosers must not be null");
        Objects.requireNonNull(winners, "winners must not be null");
        DebtReason actualReason = reason != null ? reason : DebtReason.ROUND_LOSS;

        List<DebtMovement> movements = new ArrayList<>();
        int l = confirmedLosers.size();
        int w = winners.size();

        if (l == 0 || w == 0 || drinkPrice <= 0) {
            return movements;
        }

        // Paso 1: Cada perdedor cubre una bebida completa a un ganador directo
        int directCoverage = Math.min(l, w);
        for (int i = 0; i < directCoverage; i++) {
            String loserId = confirmedLosers.get(i);
            String winnerId = winners.get(i);
            movements.add(new DebtMovement(
                    UUID.randomUUID().toString(),
                    gameId,
                    roundId,
                    loserId,
                    winnerId,
                    drinkPrice,
                    actualReason
            ));
        }

        // Paso 2: Ganadores restantes (W - L)
        if (w > l) {
            List<String> remainingWinners = winners.subList(l, w);
            int remainingWinnersCount = remainingWinners.size();
            long totalRemainingDrinksValue = (long) remainingWinnersCount * drinkPrice;

            // Paso 3: Dividir bebidas restantes entre los L perdedores (división entera)
            long additionalPerLoser = totalRemainingDrinksValue / l;

            if (additionalPerLoser > 0) {
                // Cada perdedor reparte su cuota adicional entre los ganadores restantes
                long perWinnerFromEachLoser = additionalPerLoser / remainingWinnersCount;
                if (perWinnerFromEachLoser > 0) {
                    for (String loserId : confirmedLosers) {
                        for (String remainingWinnerId : remainingWinners) {
                            movements.add(new DebtMovement(
                                    UUID.randomUUID().toString(),
                                    gameId,
                                    roundId,
                                    loserId,
                                    remainingWinnerId,
                                    perWinnerFromEachLoser,
                                    actualReason
                            ));
                        }
                    }
                }
            }
        }

        return movements;
    }

    /**
     * Distribución económica bajo modalidad DIVIDIR_CUENTA (Sección 37, 64).
     * Distribuye el monto de cada participante entre los ganadores de la ronda.
     */
    public List<DebtMovement> distributeDividedAccount(String gameId,
                                                      String roundId,
                                                      List<PlayerDebtPortion> portions,
                                                      List<String> winners,
                                                      DebtReason reason) {
        Objects.requireNonNull(gameId, "gameId must not be null");
        Objects.requireNonNull(roundId, "roundId must not be null");
        Objects.requireNonNull(portions, "portions must not be null");
        Objects.requireNonNull(winners, "winners must not be null");
        DebtReason actualReason = reason != null ? reason : DebtReason.TIE_BREAK;

        List<DebtMovement> movements = new ArrayList<>();
        if (winners.isEmpty() || portions.isEmpty()) {
            return movements;
        }

        int winnersCount = winners.size();

        for (PlayerDebtPortion portion : portions) {
            long totalAmount = portion.getAmount();
            if (totalAmount <= 0) continue;

            long amountPerWinner = totalAmount / winnersCount; // División entera COP
            if (amountPerWinner > 0) {
                for (String winnerId : winners) {
                    movements.add(new DebtMovement(
                            UUID.randomUUID().toString(),
                            gameId,
                            roundId,
                            portion.getPlayerId(),
                            winnerId,
                            amountPerWinner,
                            actualReason
                    ));
                }
            }
        }

        return movements;
    }
}
