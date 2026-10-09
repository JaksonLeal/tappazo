package com.tappazo.domain.model;

import java.time.Instant;
import java.util.Objects;

/**
 * Movimiento de deuda entre jugadores dentro de una partida (Sección 50).
 * Montos en enteros (long) en COP, sin flotantes (Sección 46).
 */
public class DebtMovement {
    private final String id;
    private final String gameId;
    private final String roundId;
    private final String fromPlayerId; // Quien debe pagar
    private final String toPlayerId;   // Quien debe recibir
    private final long amount;         // En pesos COP
    private final DebtReason reason;
    private final Instant createdAt;

    public DebtMovement(String id, String gameId, String roundId, String fromPlayerId, String toPlayerId, long amount, DebtReason reason) {
        this.id = Objects.requireNonNull(id, "id must not be null");
        this.gameId = Objects.requireNonNull(gameId, "gameId must not be null");
        this.roundId = Objects.requireNonNull(roundId, "roundId must not be null");
        this.fromPlayerId = Objects.requireNonNull(fromPlayerId, "fromPlayerId must not be null");
        this.toPlayerId = Objects.requireNonNull(toPlayerId, "toPlayerId must not be null");
        if (amount < 0) {
            throw new IllegalArgumentException("El monto de la deuda no puede ser negativo");
        }
        this.amount = amount;
        this.reason = Objects.requireNonNull(reason, "reason must not be null");
        this.createdAt = Instant.now();
    }

    public DebtMovement(String id, String gameId, String roundId, String fromPlayerId, String toPlayerId, long amount, DebtReason reason, Instant createdAt) {
        this.id = Objects.requireNonNull(id, "id must not be null");
        this.gameId = Objects.requireNonNull(gameId, "gameId must not be null");
        this.roundId = Objects.requireNonNull(roundId, "roundId must not be null");
        this.fromPlayerId = Objects.requireNonNull(fromPlayerId, "fromPlayerId must not be null");
        this.toPlayerId = Objects.requireNonNull(toPlayerId, "toPlayerId must not be null");
        if (amount < 0) {
            throw new IllegalArgumentException("El monto de la deuda no puede ser negativo");
        }
        this.amount = amount;
        this.reason = Objects.requireNonNull(reason, "reason must not be null");
        this.createdAt = createdAt != null ? createdAt : Instant.now();
    }

    public String getId() {
        return id;
    }

    public String getGameId() {
        return gameId;
    }

    public String getRoundId() {
        return roundId;
    }

    public String getFromPlayerId() {
        return fromPlayerId;
    }

    public String getToPlayerId() {
        return toPlayerId;
    }

    public long getAmount() {
        return amount;
    }

    public DebtReason getReason() {
        return reason;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
