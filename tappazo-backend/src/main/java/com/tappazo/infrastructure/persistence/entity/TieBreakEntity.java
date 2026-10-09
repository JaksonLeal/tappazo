package com.tappazo.infrastructure.persistence.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "tie_breaks")
public class TieBreakEntity {

    @Id
    @Column(length = 36, nullable = false)
    private String id;

    @Column(name = "source_round_id", length = 36, nullable = false)
    private String sourceRoundId;

    @Column(name = "tie_type", length = 50, nullable = false)
    private String tieType;

    @Column(name = "affected_loser_slots", nullable = false)
    private int affectedLoserSlots;

    @Column(length = 20, nullable = false)
    private String status;

    @Column(name = "resolution_mode", length = 30)
    private String resolutionMode;

    @Column(name = "selected_decider_id", length = 36)
    private String selectedDeciderId;

    @Column(name = "all_tied_case", nullable = false)
    private boolean allTiedCase;

    @Column(name = "tie_break_round_id", length = 36)
    private String tieBreakRoundId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "resolved_at")
    private Instant resolvedAt;

    @Version
    @Column(nullable = false)
    private Long version;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @JoinColumn(name = "tie_break_id")
    private List<TieBreakParticipantEntity> participants = new ArrayList<>();

    public TieBreakEntity() {}

    public TieBreakEntity(String id, String sourceRoundId, String tieType, int affectedLoserSlots,
                          String status, String resolutionMode, String selectedDeciderId,
                          boolean allTiedCase, String tieBreakRoundId, Instant createdAt,
                          Instant resolvedAt, Long version, List<TieBreakParticipantEntity> participants) {
        this.id = id;
        this.sourceRoundId = sourceRoundId;
        this.tieType = tieType;
        this.affectedLoserSlots = affectedLoserSlots;
        this.status = status;
        this.resolutionMode = resolutionMode;
        this.selectedDeciderId = selectedDeciderId;
        this.allTiedCase = allTiedCase;
        this.tieBreakRoundId = tieBreakRoundId;
        this.createdAt = createdAt;
        this.resolvedAt = resolvedAt;
        this.version = version;
        this.participants = participants != null ? participants : new ArrayList<>();
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getSourceRoundId() { return sourceRoundId; }
    public void setSourceRoundId(String sourceRoundId) { this.sourceRoundId = sourceRoundId; }
    public String getTieType() { return tieType; }
    public void setTieType(String tieType) { this.tieType = tieType; }
    public int getAffectedLoserSlots() { return affectedLoserSlots; }
    public void setAffectedLoserSlots(int affectedLoserSlots) { this.affectedLoserSlots = affectedLoserSlots; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getResolutionMode() { return resolutionMode; }
    public void setResolutionMode(String resolutionMode) { this.resolutionMode = resolutionMode; }
    public String getSelectedDeciderId() { return selectedDeciderId; }
    public void setSelectedDeciderId(String selectedDeciderId) { this.selectedDeciderId = selectedDeciderId; }
    public boolean isAllTiedCase() { return allTiedCase; }
    public void setAllTiedCase(boolean allTiedCase) { this.allTiedCase = allTiedCase; }
    public String getTieBreakRoundId() { return tieBreakRoundId; }
    public void setTieBreakRoundId(String tieBreakRoundId) { this.tieBreakRoundId = tieBreakRoundId; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getResolvedAt() { return resolvedAt; }
    public void setResolvedAt(Instant resolvedAt) { this.resolvedAt = resolvedAt; }
    public Long getVersion() { return version; }
    public void setVersion(Long version) { this.version = version; }
    public List<TieBreakParticipantEntity> getParticipants() { return participants; }
    public void setParticipants(List<TieBreakParticipantEntity> participants) { this.participants = participants; }
}
