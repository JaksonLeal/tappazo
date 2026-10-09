package com.tappazo.infrastructure.persistence.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "debt_movements")
public class DebtMovementEntity {

    @Id
    @Column(length = 36, nullable = false)
    private String id;

    @Column(name = "game_id", length = 36, nullable = false)
    private String gameId;

    @Column(name = "round_id", length = 36, nullable = false)
    private String roundId;

    @Column(name = "from_player_id", length = 36, nullable = false)
    private String fromPlayerId;

    @Column(name = "to_player_id", length = 36, nullable = false)
    private String toPlayerId;

    @Column(nullable = false)
    private long amount;

    @Column(length = 30, nullable = false)
    private String reason;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    public DebtMovementEntity() {}

    public DebtMovementEntity(String id, String gameId, String roundId, String fromPlayerId, String toPlayerId, long amount, String reason, Instant createdAt) {
        this.id = id;
        this.gameId = gameId;
        this.roundId = roundId;
        this.fromPlayerId = fromPlayerId;
        this.toPlayerId = toPlayerId;
        this.amount = amount;
        this.reason = reason;
        this.createdAt = createdAt;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getGameId() { return gameId; }
    public void setGameId(String gameId) { this.gameId = gameId; }
    public String getRoundId() { return roundId; }
    public void setRoundId(String roundId) { this.roundId = roundId; }
    public String getFromPlayerId() { return fromPlayerId; }
    public void setFromPlayerId(String fromPlayerId) { this.fromPlayerId = fromPlayerId; }
    public String getToPlayerId() { return toPlayerId; }
    public void setToPlayerId(String toPlayerId) { this.toPlayerId = toPlayerId; }
    public long getAmount() { return amount; }
    public void setAmount(long amount) { this.amount = amount; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
