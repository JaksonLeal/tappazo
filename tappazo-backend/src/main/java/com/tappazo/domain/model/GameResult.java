package com.tappazo.domain.model;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Resultado global de la resolución de una ronda (Sección 28, 44, 45).
 */
public class GameResult {
    private final int totalLoserSlots;
    private final List<String> confirmedLosers;
    private final List<String> winners;
    private final boolean hasTie;
    private final List<String> tiedPlayers;
    private final int remainingLoserSlots;
    private final boolean allTied;
    private final List<String> unassignedAutomaticCandidates;

    public GameResult(int totalLoserSlots, List<String> confirmedLosers, List<String> winners,
                      boolean hasTie, List<String> tiedPlayers, int remainingLoserSlots,
                      boolean allTied, List<String> unassignedAutomaticCandidates) {
        this.totalLoserSlots = totalLoserSlots;
        this.confirmedLosers = confirmedLosers != null ? Collections.unmodifiableList(confirmedLosers) : Collections.emptyList();
        this.winners = winners != null ? Collections.unmodifiableList(winners) : Collections.emptyList();
        this.hasTie = hasTie;
        this.tiedPlayers = tiedPlayers != null ? Collections.unmodifiableList(tiedPlayers) : Collections.emptyList();
        this.remainingLoserSlots = remainingLoserSlots;
        this.allTied = allTied;
        this.unassignedAutomaticCandidates = unassignedAutomaticCandidates != null ?
                Collections.unmodifiableList(unassignedAutomaticCandidates) : Collections.emptyList();
    }

    public int getTotalLoserSlots() {
        return totalLoserSlots;
    }

    public List<String> getConfirmedLosers() {
        return confirmedLosers;
    }

    public List<String> getWinners() {
        return winners;
    }

    public boolean hasTie() {
        return hasTie;
    }

    public List<String> getTiedPlayers() {
        return tiedPlayers;
    }

    public int getRemainingLoserSlots() {
        return remainingLoserSlots;
    }

    public boolean isAllTied() {
        return allTied;
    }

    public List<String> getUnassignedAutomaticCandidates() {
        return unassignedAutomaticCandidates;
    }
}
