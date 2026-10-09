package com.tappazo.domain.model;

import java.time.Instant;
import java.util.*;

/**
 * Entidad de dominio TieBreak (Sección 33, 61).
 * Representa un proceso de desempate originado en una ronda.
 */
public class TieBreak {
    private final String id;
    private final String sourceRoundId;
    private final String tieType;
    private final int affectedLoserSlots;
    private TieBreakStatus status;
    private TieBreakResolutionMode resolutionMode;
    private String selectedDeciderId;
    private final boolean allTiedCase;
    private String tieBreakRoundId;
    private final Instant createdAt;
    private Instant resolvedAt;
    private Long version;
    private final List<TieBreakParticipant> participants;

    public TieBreak(String id, String sourceRoundId, String tieType, int affectedLoserSlots, boolean allTiedCase) {
        this.id = Objects.requireNonNull(id, "id must not be null");
        this.sourceRoundId = Objects.requireNonNull(sourceRoundId, "sourceRoundId must not be null");
        this.tieType = Objects.requireNonNull(tieType, "tieType must not be null");
        this.affectedLoserSlots = affectedLoserSlots;
        this.allTiedCase = allTiedCase;
        this.status = TieBreakStatus.PENDING;
        this.createdAt = Instant.now();
        this.version = 0L;
        this.participants = new ArrayList<>();
    }

    public TieBreak(String id, String sourceRoundId, String tieType, int affectedLoserSlots,
                    TieBreakStatus status, TieBreakResolutionMode resolutionMode, String selectedDeciderId,
                    boolean allTiedCase, String tieBreakRoundId, Instant createdAt, Instant resolvedAt,
                    Long version, List<TieBreakParticipant> participants) {
        this.id = Objects.requireNonNull(id, "id must not be null");
        this.sourceRoundId = Objects.requireNonNull(sourceRoundId, "sourceRoundId must not be null");
        this.tieType = Objects.requireNonNull(tieType, "tieType must not be null");
        this.affectedLoserSlots = affectedLoserSlots;
        this.status = Objects.requireNonNull(status, "status must not be null");
        this.resolutionMode = resolutionMode;
        this.selectedDeciderId = selectedDeciderId;
        this.allTiedCase = allTiedCase;
        this.tieBreakRoundId = tieBreakRoundId;
        this.createdAt = createdAt != null ? createdAt : Instant.now();
        this.resolvedAt = resolvedAt;
        this.version = version != null ? version : 0L;
        this.participants = participants != null ? new ArrayList<>(participants) : new ArrayList<>();
    }

    public void addParticipant(TieBreakParticipant participant) {
        this.participants.add(participant);
    }

    public void resolve(TieBreakResolutionMode mode, String deciderId, String tieBreakRoundId) {
        this.resolutionMode = Objects.requireNonNull(mode, "resolutionMode must not be null");
        this.selectedDeciderId = deciderId;
        this.tieBreakRoundId = tieBreakRoundId;
        this.status = TieBreakStatus.RESOLVED;
        this.resolvedAt = Instant.now();
    }

    public String getId() {
        return id;
    }

    public String getSourceRoundId() {
        return sourceRoundId;
    }

    public String getTieType() {
        return tieType;
    }

    public int getAffectedLoserSlots() {
        return affectedLoserSlots;
    }

    public TieBreakStatus getStatus() {
        return status;
    }

    public TieBreakResolutionMode getResolutionMode() {
        return resolutionMode;
    }

    public String getSelectedDeciderId() {
        return selectedDeciderId;
    }

    public boolean isAllTiedCase() {
        return allTiedCase;
    }

    public String getTieBreakRoundId() {
        return tieBreakRoundId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getResolvedAt() {
        return resolvedAt;
    }

    public Long getVersion() {
        return version;
    }

    public List<TieBreakParticipant> getParticipants() {
        return Collections.unmodifiableList(participants);
    }
}
