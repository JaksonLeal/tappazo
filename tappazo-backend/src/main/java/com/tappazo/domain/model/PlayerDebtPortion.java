package com.tappazo.domain.model;

import java.util.Objects;

/**
 * Representa la porción porcentual y el monto que debe pagar un jugador bajo DIVIDIR_CUENTA (Sección 37).
 */
public class PlayerDebtPortion {
    private final String playerId;
    private final double percentage;
    private final long amount; // Monto truncado en pesos COP
    private final boolean confirmedLoser;

    public PlayerDebtPortion(String playerId, double percentage, long amount, boolean confirmedLoser) {
        this.playerId = Objects.requireNonNull(playerId, "playerId must not be null");
        this.percentage = percentage;
        this.amount = amount;
        this.confirmedLoser = confirmedLoser;
    }

    public String getPlayerId() {
        return playerId;
    }

    public double getPercentage() {
        return percentage;
    }

    public long getAmount() {
        return amount;
    }

    public boolean isConfirmedLoser() {
        return confirmedLoser;
    }
}
