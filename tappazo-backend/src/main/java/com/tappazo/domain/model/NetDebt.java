package com.tappazo.domain.model;

import java.util.Objects;

/**
 * Representa una deuda neta consolidada entre dos jugadores dentro de una partida (Sección 52).
 */
public class NetDebt {
    private final String fromPlayerId;
    private final String toPlayerId;
    private final long amount;

    public NetDebt(String fromPlayerId, String toPlayerId, long amount) {
        this.fromPlayerId = Objects.requireNonNull(fromPlayerId, "fromPlayerId must not be null");
        this.toPlayerId = Objects.requireNonNull(toPlayerId, "toPlayerId must not be null");
        if (amount <= 0) {
            throw new IllegalArgumentException("El monto neto debe ser positivo");
        }
        this.amount = amount;
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        NetDebt netDebt = (NetDebt) o;
        return amount == netDebt.amount &&
                Objects.equals(fromPlayerId, netDebt.fromPlayerId) &&
                Objects.equals(toPlayerId, netDebt.toPlayerId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(fromPlayerId, toPlayerId, amount);
    }
}
