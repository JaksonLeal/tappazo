package com.tappazo.domain.service;

import com.tappazo.domain.model.PlayerDebtPortion;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Calculador de porciones económicas para la modalidad DIVIDIR_CUENTA (Sección 37, 64).
 * Aplica la fórmula definitiva de porcentajes sobre la CUENTA = (N - TOTAL_LOSER_SLOTS) * drinkPrice.
 */
public class LoserPortionCalculator {

    /**
     * Calcula la CUENTA de la ronda (Sección 36).
     * CUENTA = (N_jugadores - TOTAL_LOSER_SLOTS) * drinkPrice
     */
    public long calculateAccount(int totalPlayers, int totalLoserSlots, long drinkPrice) {
        if (totalPlayers < totalLoserSlots) {
            throw new IllegalArgumentException("totalPlayers no puede ser menor a totalLoserSlots");
        }
        int winnersCount = totalPlayers - totalLoserSlots;
        return winnersCount * drinkPrice;
    }

    /**
     * Calcula los porcentajes y montos para cada perdedor confirmado y cada candidato empatado.
     *
     * @param totalPlayers cantidad total de participantes en la ronda (N)
     * @param totalLoserSlots total de puestos de perdedor
     * @param confirmedNonTiedLosers IDs de los perdedores confirmados que no están empatados
     * @param tiedCandidates IDs de los jugadores empatados compitiendo por los puestos restantes
     * @param remainingLoserSlots cantidad de puestos restantes por los que compiten
     * @param drinkPrice precio congelado de la bebida
     * @return lista de PlayerDebtPortion con porcentaje y monto en COP
     */
    public List<PlayerDebtPortion> calculatePortions(int totalPlayers,
                                                     int totalLoserSlots,
                                                     List<String> confirmedNonTiedLosers,
                                                     List<String> tiedCandidates,
                                                     int remainingLoserSlots,
                                                     long drinkPrice) {
        Objects.requireNonNull(confirmedNonTiedLosers, "confirmedNonTiedLosers must not be null");
        Objects.requireNonNull(tiedCandidates, "tiedCandidates must not be null");
        if (totalLoserSlots <= 0) {
            throw new IllegalArgumentException("totalLoserSlots must be greater than 0");
        }

        long totalAccount = calculateAccount(totalPlayers, totalLoserSlots, drinkPrice);
        List<PlayerDebtPortion> portions = new ArrayList<>();

        // 1. Perdedor confirmado NO empatado: porcentaje = 1 / TOTAL_LOSER_SLOTS
        double confirmedPercentage = 1.0 / totalLoserSlots;
        for (String loserId : confirmedNonTiedLosers) {
            long amount = (long) (confirmedPercentage * totalAccount);
            portions.add(new PlayerDebtPortion(loserId, confirmedPercentage, amount, true));
        }

        // 2. Candidato empatado que compite:
        // porcentaje = REMAINING_LOSER_SLOTS / (TOTAL_LOSER_SLOTS * cantidadEmpatados)
        if (!tiedCandidates.isEmpty() && remainingLoserSlots > 0) {
            double tiedPercentage = (double) remainingLoserSlots / (totalLoserSlots * tiedCandidates.size());
            for (String tiedId : tiedCandidates) {
                long amount = (long) (tiedPercentage * totalAccount);
                portions.add(new PlayerDebtPortion(tiedId, tiedPercentage, amount, false));
            }
        }

        return portions;
    }
}
