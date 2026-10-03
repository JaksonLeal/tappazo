package com.tappazo.domain.model;

import java.util.Objects;

/**
 * Estadísticas brutas del jugador (Sección 51, 53, 55).
 * Mantiene montos brutos acumulados independientemente del balance neto por partida.
 */
public class PlayerStatistics {
    private final String userId;
    private int roundsPlayed;
    private int roundsWon;
    private int roundsLost;
    private long moneySpent;
    private long moneyReceived;

    public PlayerStatistics(String userId) {
        this.userId = Objects.requireNonNull(userId, "userId must not be null");
        this.roundsPlayed = 0;
        this.roundsWon = 0;
        this.roundsLost = 0;
        this.moneySpent = 0L;
        this.moneyReceived = 0L;
    }

    public PlayerStatistics(String userId, int roundsPlayed, int roundsWon, int roundsLost, long moneySpent, long moneyReceived) {
        this.userId = Objects.requireNonNull(userId, "userId must not be null");
        this.roundsPlayed = roundsPlayed;
        this.roundsWon = roundsWon;
        this.roundsLost = roundsLost;
        this.moneySpent = moneySpent;
        this.moneyReceived = moneyReceived;
    }

    public void recordRoundResult(boolean won, long spent, long received) {
        this.roundsPlayed++;
        if (won) {
            this.roundsWon++;
        } else {
            this.roundsLost++;
        }
        this.moneySpent += spent;
        this.moneyReceived += received;
    }

    public String getUserId() {
        return userId;
    }

    public int getRoundsPlayed() {
        return roundsPlayed;
    }

    public int getRoundsWon() {
        return roundsWon;
    }

    public int getRoundsLost() {
        return roundsLost;
    }

    public long getMoneySpent() {
        return moneySpent;
    }

    public long getMoneyReceived() {
        return moneyReceived;
    }
}
