package com.tappazo.infrastructure.persistence.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "rounds")
public class RoundEntity {

    @Id
    @Column(length = 36, nullable = false)
    private String id;

    @Column(name = "game_id", length = 36, nullable = false)
    private String gameId;

    @Column(length = 30, nullable = false)
    private String mode;

    @Column(length = 20, nullable = false)
    private String state;

    @Column(name = "drink_price", nullable = false)
    private long drinkPrice;

    @Column(name = "started_at", nullable = false)
    private Instant startedAt;

    @Column(name = "finished_at")
    private Instant finishedAt;

    @Column(name = "round_type", length = 20, nullable = false)
    private String roundType;

    @Version
    @Column(nullable = false)
    private Long version;

    public RoundEntity() {}

    public RoundEntity(String id, String gameId, String mode, String state, long drinkPrice,
                       Instant startedAt, Instant finishedAt, String roundType, Long version) {
        this.id = id;
        this.gameId = gameId;
        this.mode = mode;
        this.state = state;
        this.drinkPrice = drinkPrice;
        this.startedAt = startedAt;
        this.finishedAt = finishedAt;
        this.roundType = roundType;
        this.version = version;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getGameId() { return gameId; }
    public void setGameId(String gameId) { this.gameId = gameId; }
    public String getMode() { return mode; }
    public void setMode(String mode) { this.mode = mode; }
    public String getState() { return state; }
    public void setState(String state) { this.state = state; }
    public long getDrinkPrice() { return drinkPrice; }
    public void setDrinkPrice(long drinkPrice) { this.drinkPrice = drinkPrice; }
    public Instant getStartedAt() { return startedAt; }
    public void setStartedAt(Instant startedAt) { this.startedAt = startedAt; }
    public Instant getFinishedAt() { return finishedAt; }
    public void setFinishedAt(Instant finishedAt) { this.finishedAt = finishedAt; }
    public String getRoundType() { return roundType; }
    public void setRoundType(String roundType) { this.roundType = roundType; }
    public Long getVersion() { return version; }
    public void setVersion(Long version) { this.version = version; }
}
