package com.tappazo.domain.model;

import java.time.Instant;
import java.util.Objects;

/**
 * Participante en un desempate (Sección 33, 61).
 */
public class TieBreakParticipant {
    private final String tieBreakId;
    private final String playerId;
    private TieBreakResolutionMode decision;
    private Instant decisionAt;

    public TieBreakParticipant(String tieBreakId, String playerId) {
        this.tieBreakId = Objects.requireNonNull(tieBreakId, "tieBreakId must not be null");
        this.playerId = Objects.requireNonNull(playerId, "playerId must not be null");
    }

    public TieBreakParticipant(String tieBreakId, String playerId, TieBreakResolutionMode decision, Instant decisionAt) {
        this.tieBreakId = Objects.requireNonNull(tieBreakId, "tieBreakId must not be null");
        this.playerId = Objects.requireNonNull(playerId, "playerId must not be null");
        this.decision = decision;
        this.decisionAt = decisionAt;
    }

    public void castDecision(TieBreakResolutionMode decision) {
        this.decision = Objects.requireNonNull(decision, "decision must not be null");
        this.decisionAt = Instant.now();
    }

    public String getTieBreakId() {
        return tieBreakId;
    }

    public String getPlayerId() {
        return playerId;
    }

    public TieBreakResolutionMode getDecision() {
        return decision;
    }

    public Instant getDecisionAt() {
        return decisionAt;
    }
}
