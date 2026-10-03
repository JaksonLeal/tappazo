package com.tappazo.domain.service;

import com.tappazo.domain.model.DebtMovement;
import com.tappazo.domain.model.DebtReason;
import com.tappazo.domain.model.GameMode;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Motor de cálculo de deudas por modalidad (FASE 3 - Secciones 36, 46, 47, 48, 49).
 * Backend es la AUTORIDAD ABSOLUTA de los cálculos económicos.
 * Toda moneda en enteros Long (COP), sin double/float para importes, con truncamiento explícito.
 */
public class DebtCalculationEngine {

    /**
     * Resultado estructurado del cálculo de deuda de una ronda.
     */
    public record RoundDebtSummary(
            long totalAccount,
            long basePerLoser,
            long additionalPerLoser,
            long totalPerLoser,
            long totalPaidByLosers,
            long discardedRemainder,
            List<DebtMovement> debtMovements
    ) {}

    /**
     * Calcula la deuda exacta por perdedor y genera los movimientos según la modalidad.
     *
     * @param gameId identificador de la partida
     * @param roundId identificador de la ronda
     * @param mode modalidad jugada (ULTIMO_PIERDE, PEQUENOS_PAGAN, ULTIMOS_DOS_PIERDEN)
     * @param confirmedLosers lista ordenada de IDs de los perdedores confirmados
     * @param winners lista ordenada de IDs de los ganadores
     * @param drinkPrice precio congelado de la bebida en COP
     * @return RoundDebtSummary con desglose financiero exacto y movimientos generados
     */
    public RoundDebtSummary calculateRoundDebt(String gameId,
                                              String roundId,
                                              GameMode mode,
                                              List<String> confirmedLosers,
                                              List<String> winners,
                                              long drinkPrice) {
        Objects.requireNonNull(gameId, "gameId must not be null");
        Objects.requireNonNull(roundId, "roundId must not be null");
        Objects.requireNonNull(mode, "mode must not be null");
        Objects.requireNonNull(confirmedLosers, "confirmedLosers must not be null");
        Objects.requireNonNull(winners, "winners must not be null");

        if (drinkPrice <= 0) {
            throw new IllegalArgumentException("El precio de la bebida debe ser mayor a 0");
        }

        int l = confirmedLosers.size();
        int w = winners.size();
        int totalPlayers = l + w;

        // Principio Maestro (Sección 36): CUENTA = (N - L) * drinkPrice = W * drinkPrice
        long totalAccount = (long) w * drinkPrice;

        if (l == 0 || w == 0) {
            return new RoundDebtSummary(totalAccount, 0, 0, 0, 0, 0, List.of());
        }

        long basePerLoser = drinkPrice;
        long additionalPerLoser = 0;

        switch (mode) {
            case ULTIMO_PIERDE -> {
                // Sección 47: 1 perdedor cubre a todos los ganadores
                // L=1, additionalPerLoser = (W - 1) * drinkPrice
                if (w > 1) {
                    additionalPerLoser = (long) (w - 1) * drinkPrice;
                }
            }
            case PEQUENOS_PAGAN -> {
                // Sección 48: L = floor(N/2), W = N - L
                // additionalPerLoser = ((W - L) * drinkPrice) / L
                if (w > l) {
                    additionalPerLoser = ((long) (w - l) * drinkPrice) / l;
                }
            }
            case ULTIMOS_DOS_PIERDEN -> {
                // Sección 42, 49: L = 2 fijos
                // additionalPerLoser = ((W - L) * drinkPrice) / L
                if (w > l) {
                    additionalPerLoser = ((long) (w - l) * drinkPrice) / l;
                }
            }
        }

        long totalPerLoser = basePerLoser + additionalPerLoser;
        long totalPaidByLosers = totalPerLoser * l;
        long discardedRemainder = Math.max(0, totalAccount - totalPaidByLosers);

        // Generar movimientos detallados de deuda (Sección 49)
        List<DebtMovement> movements = new ArrayList<>();

        // 1. Cobertura directa de bebida completa (1 perdedor cubre 1 ganador)
        int directCount = Math.min(l, w);
        for (int i = 0; i < directCount; i++) {
            movements.add(new DebtMovement(
                    UUID.randomUUID().toString(),
                    gameId,
                    roundId,
                    confirmedLosers.get(i),
                    winners.get(i),
                    drinkPrice,
                    DebtReason.ROUND_LOSS
            ));
        }

        // 2. Cobertura de ganadores restantes (W - L)
        if (w > l && additionalPerLoser > 0) {
            List<String> remainingWinners = winners.subList(l, w);
            int remWinnersCount = remainingWinners.size();
            long sharePerRemWinner = additionalPerLoser / remWinnersCount;

            if (sharePerRemWinner > 0) {
                for (String loserId : confirmedLosers) {
                    for (String remWinnerId : remainingWinners) {
                        movements.add(new DebtMovement(
                                UUID.randomUUID().toString(),
                                gameId,
                                roundId,
                                loserId,
                                remWinnerId,
                                sharePerRemWinner,
                                DebtReason.ROUND_LOSS
                        ));
                    }
                }
            }
        }

        return new RoundDebtSummary(
                totalAccount,
                basePerLoser,
                additionalPerLoser,
                totalPerLoser,
                totalPaidByLosers,
                discardedRemainder,
                movements
        );
    }
}
