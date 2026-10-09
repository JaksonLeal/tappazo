package com.tappazo.infrastructure.persistence.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "reveals")
public class RevealEntity {

    @Id
    @Column(length = 36, nullable = false)
    private String id;

    @Column(name = "round_id", length = 36, nullable = false)
    private String roundId;

    @Column(name = "player_id", length = 36, nullable = false)
    private String playerId;

    @Column(length = 20, nullable = false)
    private String status;

    @Column(name = "official_number")
    private Integer officialNumber;

    @Column(name = "approved_at")
    private Instant approvedAt;

    @Version
    @Column(nullable = false)
    private Long version;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @JoinColumn(name = "reveal_id")
    private List<RevealAttemptEntity> attempts = new ArrayList<>();

    public RevealEntity() {}

    public RevealEntity(String id, String roundId, String playerId, String status,
                        Integer officialNumber, Instant approvedAt, Long version, List<RevealAttemptEntity> attempts) {
        this.id = id;
        this.roundId = roundId;
        this.playerId = playerId;
        this.status = status;
        this.officialNumber = officialNumber;
        this.approvedAt = approvedAt;
        this.version = version;
        this.attempts = attempts != null ? attempts : new ArrayList<>();
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getRoundId() { return roundId; }
    public void setRoundId(String roundId) { this.roundId = roundId; }
    public String getPlayerId() { return playerId; }
    public void setPlayerId(String playerId) { this.playerId = playerId; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Integer getOfficialNumber() { return officialNumber; }
    public void setOfficialNumber(Integer officialNumber) { this.officialNumber = officialNumber; }
    public Instant getApprovedAt() { return approvedAt; }
    public void setApprovedAt(Instant approvedAt) { this.approvedAt = approvedAt; }
    public Long getVersion() { return version; }
    public void setVersion(Long version) { this.version = version; }
    public List<RevealAttemptEntity> getAttempts() { return attempts; }
    public void setAttempts(List<RevealAttemptEntity> attempts) { this.attempts = attempts; }
}
