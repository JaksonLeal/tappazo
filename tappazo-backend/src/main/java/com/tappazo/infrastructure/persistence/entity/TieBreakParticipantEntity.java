package com.tappazo.infrastructure.persistence.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "tie_break_participants")
public class TieBreakParticipantEntity {

    @Id
    @Column(length = 36, nullable = false)
    private String id;

    @Column(name = "tie_break_id", length = 36, nullable = false)
    private String tieBreakId;

    @Column(name = "player_id", length = 36, nullable = false)
    private String playerId;

    @Column(length = 30)
    private String decision;

    @Column(name = "decision_at")
    private Instant decisionAt;

    public TieBreakParticipantEntity() {}

    public TieBreakParticipantEntity(String id, String tieBreakId, String playerId, String decision, Instant decisionAt) {
        this.id = id;
        this.tieBreakId = tieBreakId;
        this.playerId = playerId;
        this.decision = decision;
        this.decisionAt = decisionAt;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getTieBreakId() { return tieBreakId; }
    public void setTieBreakId(String tieBreakId) { this.tieBreakId = tieBreakId; }
    public String getPlayerId() { return playerId; }
    public void setPlayerId(String playerId) { this.playerId = playerId; }
    public String getDecision() { return decision; }
    public void setDecision(String decision) { this.decision = decision; }
    public Instant getDecisionAt() { return decisionAt; }
    public void setDecisionAt(Instant decisionAt) { this.decisionAt = decisionAt; }
}
