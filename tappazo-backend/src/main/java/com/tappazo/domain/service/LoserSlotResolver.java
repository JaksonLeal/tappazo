package com.tappazo.domain.service;

import com.tappazo.domain.model.GameResult;
import com.tappazo.domain.model.PlayerResult;

import java.util.*;

/**
 * Motor de resolución de Loser Slots (Sección 29, 30, 31, 32, 43, 44).
 * Aplica la fórmula de loser slots con remociones y garantiza que el total de perdedores
 * NUNCA supere TOTAL_LOSER_SLOTS.
 */
public class LoserSlotResolver {

    /**
     * Resuelve los resultados de la ronda asignando loser slots y detectando empates en el límite.
     *
     * @param results lista de resultados individuales de los participantes
     * @param totalLoserSlots número total de puestos de perdedor definidos por la modalidad
     * @return GameResult con perdedores confirmados, ganadores, y detalles del desempate si aplica
     */
    public GameResult resolve(List<PlayerResult> results, int totalLoserSlots) {
        Objects.requireNonNull(results, "results must not be null");
        if (results.isEmpty()) {
            throw new IllegalArgumentException("results must not be empty");
        }
        if (totalLoserSlots <= 0) {
            throw new IllegalArgumentException("totalLoserSlots must be greater than 0");
        }

        // 1. Separar candidatos automáticos (desconexión o reveal invalid) y jugadores normales
        List<PlayerResult> automaticCandidates = new ArrayList<>();
        List<PlayerResult> normalPlayers = new ArrayList<>();

        for (PlayerResult pr : results) {
            if (pr.isAutomaticLoser()) {
                automaticCandidates.add(pr);
            } else if (pr.getOfficialNumber() != null) {
                normalPlayers.add(pr);
            }
        }

        // 2. Asignar loser slots a los candidatos automáticos (Sección 30)
        List<String> confirmedLosers = new ArrayList<>();
        List<String> unassignedAutomaticCandidates = new ArrayList<>();

        int automaticAssignedCount = Math.min(automaticCandidates.size(), totalLoserSlots);
        for (int i = 0; i < automaticCandidates.size(); i++) {
            PlayerResult cand = automaticCandidates.get(i);
            if (i < automaticAssignedCount) {
                confirmedLosers.add(cand.getPlayerId());
            } else {
                // Excedente de candidatos automáticos: quedan removidos pero NO ocupan loser slot ni generan deuda
                unassignedAutomaticCandidates.add(cand.getPlayerId());
            }
        }

        int remainingLoserSlotsForNormal = totalLoserSlots - automaticAssignedCount;

        // Si ya no quedan loser slots disponibles, todos los jugadores normales son ganadores
        if (remainingLoserSlotsForNormal <= 0) {
            List<String> winners = new ArrayList<>();
            for (PlayerResult np : normalPlayers) {
                winners.add(np.getPlayerId());
            }
            return new GameResult(totalLoserSlots, confirmedLosers, winners, false, Collections.emptyList(), 0, false, unassignedAutomaticCandidates);
        }

        // Si no hay jugadores normales
        if (normalPlayers.isEmpty()) {
            return new GameResult(totalLoserSlots, confirmedLosers, Collections.emptyList(), false, Collections.emptyList(), 0, false, unassignedAutomaticCandidates);
        }

        // 3. Ordenar jugadores normales de menor a mayor número oficial (el menor pierde)
        List<PlayerResult> sortedNormal = new ArrayList<>(normalPlayers);
        sortedNormal.sort(Comparator.comparingInt(PlayerResult::getOfficialNumber));

        int n = sortedNormal.size();

        // Si la cantidad de jugadores normales es menor o igual a los puestos disponibles,
        // todos los jugadores normales ocupan un loser slot (Sección 43: puestos ya llenos sin desempate)
        if (n <= remainingLoserSlotsForNormal) {
            for (PlayerResult pr : sortedNormal) {
                confirmedLosers.add(pr.getPlayerId());
            }
            return new GameResult(totalLoserSlots, confirmedLosers, Collections.emptyList(), false, Collections.emptyList(), 0, false, unassignedAutomaticCandidates);
        }

        // Hay más jugadores normales que puestos disponibles: revisar el límite (border) en k-1 vs k
        int k = remainingLoserSlotsForNormal;
        int boundaryNumber = sortedNormal.get(k - 1).getOfficialNumber();
        int firstWinnerNumber = sortedNormal.get(k).getOfficialNumber();

        // Caso A: No hay empate en el límite (boundaryNumber < firstWinnerNumber)
        if (boundaryNumber < firstWinnerNumber) {
            List<String> winners = new ArrayList<>();
            for (int i = 0; i < n; i++) {
                if (i < k) {
                    confirmedLosers.add(sortedNormal.get(i).getPlayerId());
                } else {
                    winners.add(sortedNormal.get(i).getPlayerId());
                }
            }
            return new GameResult(totalLoserSlots, confirmedLosers, winners, false, Collections.emptyList(), 0, false, unassignedAutomaticCandidates);
        }

        // Caso B: Empate en el límite (boundaryNumber == firstWinnerNumber) (Sección 31, 32)
        int tiedValue = boundaryNumber;
        List<String> strictlySmaller = new ArrayList<>();
        List<String> tiedCandidates = new ArrayList<>();
        List<String> strictlyGreater = new ArrayList<>();

        for (PlayerResult pr : sortedNormal) {
            if (pr.getOfficialNumber() < tiedValue) {
                strictlySmaller.add(pr.getPlayerId());
            } else if (pr.getOfficialNumber() == tiedValue) {
                tiedCandidates.add(pr.getPlayerId());
            } else {
                strictlyGreater.add(pr.getPlayerId());
            }
        }

        // Los estrictamente menores tienen su puesto asegurado
        confirmedLosers.addAll(strictlySmaller);
        int remainingSlotsForTie = remainingLoserSlotsForNormal - strictlySmaller.size();

        // Los estrictamente mayores son ganadores confirmados
        List<String> winners = new ArrayList<>(strictlyGreater);

        // Caso especial: todos los participantes de la ronda quedaron empatados (Sección 33)
        boolean allTied = strictlySmaller.isEmpty() && strictlyGreater.isEmpty()
                && automaticCandidates.isEmpty() && unassignedAutomaticCandidates.isEmpty();

        return new GameResult(
                totalLoserSlots,
                confirmedLosers,
                winners,
                true,
                tiedCandidates,
                remainingSlotsForTie,
                allTied,
                unassignedAutomaticCandidates
        );
    }
}
