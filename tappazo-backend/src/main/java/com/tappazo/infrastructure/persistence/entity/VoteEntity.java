package com.tappazo.infrastructure.persistence.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "votes", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"reveal_attempt_id", "voter_id"})
})
public class VoteEntity {

    @Id
    @Column(length = 36, nullable = false)
    private String id;

    @Column(name = "reveal_attempt_id", length = 36, nullable = false)
    private String revealAttemptId;

    @Column(name = "voter_id", length = 36, nullable = false)
    private String voterId;

    @Column(length = 10, nullable = false)
    private String decision;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    public VoteEntity() {}

    public VoteEntity(String id, String revealAttemptId, String voterId, String decision, Instant createdAt) {
        this.id = id;
        this.revealAttemptId = revealAttemptId;
        this.voterId = voterId;
        this.decision = decision;
        this.createdAt = createdAt;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getRevealAttemptId() { return revealAttemptId; }
    public void setRevealAttemptId(String revealAttemptId) { this.revealAttemptId = revealAttemptId; }
    public String getVoterId() { return voterId; }
    public void setVoterId(String voterId) { this.voterId = voterId; }
    public String getDecision() { return decision; }
    public void setDecision(String decision) { this.decision = decision; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
