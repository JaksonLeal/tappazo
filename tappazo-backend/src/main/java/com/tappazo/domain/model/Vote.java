package com.tappazo.domain.model;

import java.time.Instant;
import java.util.Objects;

/**
 * Entidad de dominio Vote (Sección 23, 60).
 * Cada Vote referencia un revealAttemptId concreto, nunca el Reveal general (Sección 24).
 */
public class Vote {
    private final String id;
    private final String revealAttemptId;
    private final String voterId;
    private final Decision decision;
    private final Instant createdAt;

    public Vote(String id, String revealAttemptId, String voterId, Decision decision) {
        this.id = Objects.requireNonNull(id, "id must not be null");
        this.revealAttemptId = Objects.requireNonNull(revealAttemptId, "revealAttemptId must not be null");
        this.voterId = Objects.requireNonNull(voterId, "voterId must not be null");
        this.decision = Objects.requireNonNull(decision, "decision must not be null");
        this.createdAt = Instant.now();
    }

    public Vote(String id, String revealAttemptId, String voterId, Decision decision, Instant createdAt) {
        this.id = Objects.requireNonNull(id, "id must not be null");
        this.revealAttemptId = Objects.requireNonNull(revealAttemptId, "revealAttemptId must not be null");
        this.voterId = Objects.requireNonNull(voterId, "voterId must not be null");
        this.decision = Objects.requireNonNull(decision, "decision must not be null");
        this.createdAt = createdAt != null ? createdAt : Instant.now();
    }

    public String getId() {
        return id;
    }

    public String getRevealAttemptId() {
        return revealAttemptId;
    }

    public String getVoterId() {
        return voterId;
    }

    public Decision getDecision() {
        return decision;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
