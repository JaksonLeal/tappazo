package com.tappazo.domain.model;

import com.tappazo.domain.exception.InvalidGameStateException;

import java.time.Instant;
import java.util.Objects;

/**
 * Entidad de dominio Round (Sección 11, 58).
 * El precio de la bebida queda congelado al iniciar la ronda.
 */
public class Round {
    private final String id;
    private final String gameId;
    private final GameMode mode;
    private RoundState state;
    private final long drinkPrice;
    private final Instant startedAt;
    private Instant finishedAt;
    private final RoundType roundType;
    private Long version;

    public Round(String id, String gameId, GameMode mode, long drinkPrice, RoundType roundType) {
        this.id = Objects.requireNonNull(id, "id must not be null");
        this.gameId = Objects.requireNonNull(gameId, "gameId must not be null");
        this.mode = Objects.requireNonNull(mode, "mode must not be null");
        if (drinkPrice <= 0) {
            throw new IllegalArgumentException("El precio de la bebida de la ronda debe ser mayor a 0");
        }
        this.drinkPrice = drinkPrice;
        this.roundType = roundType != null ? roundType : RoundType.NORMAL;
        this.state = RoundState.STARTING;
        this.startedAt = Instant.now();
        this.version = 0L;
    }

    public Round(String id, String gameId, GameMode mode, RoundState state, long drinkPrice,
                 Instant startedAt, Instant finishedAt, RoundType roundType, Long version) {
        this.id = Objects.requireNonNull(id, "id must not be null");
        this.gameId = Objects.requireNonNull(gameId, "gameId must not be null");
        this.mode = Objects.requireNonNull(mode, "mode must not be null");
        this.state = Objects.requireNonNull(state, "state must not be null");
        this.drinkPrice = drinkPrice;
        this.startedAt = startedAt != null ? startedAt : Instant.now();
        this.finishedAt = finishedAt;
        this.roundType = roundType != null ? roundType : RoundType.NORMAL;
        this.version = version != null ? version : 0L;
    }

    public void startRevealing() {
        if (state != RoundState.STARTING) {
            throw new InvalidGameStateException("Solo se puede pasar a REVEALING desde STARTING");
        }
        this.state = RoundState.REVEALING;
    }

    public void startValidating() {
        if (state != RoundState.REVEALING) {
            throw new InvalidGameStateException("Solo se puede pasar a VALIDATING desde REVEALING");
        }
        this.state = RoundState.VALIDATING;
    }

    public void startResolving() {
        if (state != RoundState.VALIDATING && state != RoundState.REVEALING) {
            throw new InvalidGameStateException("Solo se puede pasar a RESOLVING desde VALIDATING o REVEALING");
        }
        this.state = RoundState.RESOLVING;
    }

    public void startTieBreak() {
        if (state != RoundState.RESOLVING) {
            throw new InvalidGameStateException("Solo se puede pasar a TIE_BREAK desde RESOLVING");
        }
        this.state = RoundState.TIE_BREAK;
    }

    public void finish() {
        if (state == RoundState.FINISHED) {
            throw new InvalidGameStateException("La ronda ya está finalizada");
        }
        this.state = RoundState.FINISHED;
        this.finishedAt = Instant.now();
    }

    public String getId() {
        return id;
    }

    public String getGameId() {
        return gameId;
    }

    public GameMode getMode() {
        return mode;
    }

    public RoundState getState() {
        return state;
    }

    public long getDrinkPrice() {
        return drinkPrice;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public Instant getFinishedAt() {
        return finishedAt;
    }

    public RoundType getRoundType() {
        return roundType;
    }

    public Long getVersion() {
        return version;
    }

    public void incrementVersion() {
        this.version = (this.version != null ? this.version : 0L) + 1;
    }
}
