package com.tappazo.domain.model;

import java.util.Objects;

/**
 * Resultado individual de un jugador para alimentar el motor de resolución (Sección 28, 44, 45).
 */
public class PlayerResult {
    private final String playerId;
    private final Integer officialNumber;
    private final boolean automaticLoser;
    private final DebtReason automaticLoserReason;

    public static PlayerResult normal(String playerId, int officialNumber) {
        return new PlayerResult(playerId, officialNumber, false, null);
    }

    public static PlayerResult automatic(String playerId, DebtReason reason) {
        return new PlayerResult(playerId, null, true, reason);
    }

    public PlayerResult(String playerId, Integer officialNumber, boolean automaticLoser, DebtReason automaticLoserReason) {
        this.playerId = Objects.requireNonNull(playerId, "playerId must not be null");
        this.officialNumber = officialNumber;
        this.automaticLoser = automaticLoser;
        this.automaticLoserReason = automaticLoserReason;
    }

    public String getPlayerId() {
        return playerId;
    }

    public Integer getOfficialNumber() {
        return officialNumber;
    }

    public boolean isAutomaticLoser() {
        return automaticLoser;
    }

    public DebtReason getAutomaticLoserReason() {
        return automaticLoserReason;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PlayerResult that = (PlayerResult) o;
        return Objects.equals(playerId, that.playerId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(playerId);
    }
}
