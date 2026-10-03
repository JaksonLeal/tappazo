package com.tappazo.infrastructure.persistence.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "reveal_attempts")
public class RevealAttemptEntity {

    @Id
    @Column(length = 36, nullable = false)
    private String id;

    @Column(name = "reveal_id", length = 36, nullable = false)
    private String revealId;

    @Column(name = "attempt_number", nullable = false)
    private int attemptNumber;

    @Column(name = "proposed_number", nullable = false)
    private int proposedNumber;

    @Column(name = "photo_reference", length = 500)
    private String photoReference;

    @Column(name = "photo_omitted_reason", length = 255)
    private String photoOmittedReason;

    @Column(length = 20, nullable = false)
    private String status;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @JoinColumn(name = "reveal_attempt_id")
    private List<VoteEntity> votes = new ArrayList<>();

    public RevealAttemptEntity() {}

    public RevealAttemptEntity(String id, String revealId, int attemptNumber, int proposedNumber,
                               String photoReference, String photoOmittedReason, String status,
                               Instant createdAt, List<VoteEntity> votes) {
        this.id = id;
        this.revealId = revealId;
        this.attemptNumber = attemptNumber;
        this.proposedNumber = proposedNumber;
        this.photoReference = photoReference;
        this.photoOmittedReason = photoOmittedReason;
        this.status = status;
        this.createdAt = createdAt;
        this.votes = votes != null ? votes : new ArrayList<>();
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getRevealId() { return revealId; }
    public void setRevealId(String revealId) { this.revealId = revealId; }
    public int getAttemptNumber() { return attemptNumber; }
    public void setAttemptNumber(int attemptNumber) { this.attemptNumber = attemptNumber; }
    public int getProposedNumber() { return proposedNumber; }
    public void setProposedNumber(int proposedNumber) { this.proposedNumber = proposedNumber; }
    public String getPhotoReference() { return photoReference; }
    public void setPhotoReference(String photoReference) { this.photoReference = photoReference; }
    public String getPhotoOmittedReason() { return photoOmittedReason; }
    public void setPhotoOmittedReason(String photoOmittedReason) { this.photoOmittedReason = photoOmittedReason; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public List<VoteEntity> getVotes() { return votes; }
    public void setVotes(List<VoteEntity> votes) { this.votes = votes; }
}
