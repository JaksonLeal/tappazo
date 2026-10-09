package com.tappazo.infrastructure.persistence.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "game_participants", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"game_id", "user_id"})
})
public class GameParticipantEntity {

    @Id
    @Column(length = 36, nullable = false)
    private String id;

    @Column(name = "game_id", length = 36, nullable = false)
    private String gameId;

    @Column(name = "user_id", length = 36, nullable = false)
    private String userId;

    @Column(length = 20, nullable = false)
    private String role;

    @Column(name = "participant_state", length = 20, nullable = false)
    private String participantState;

    @Column(name = "joined_at", nullable = false)
    private Instant joinedAt;

    public GameParticipantEntity() {}

    public GameParticipantEntity(String id, String gameId, String userId, String role, String participantState, Instant joinedAt) {
        this.id = id;
        this.gameId = gameId;
        this.userId = userId;
        this.role = role;
        this.participantState = participantState;
        this.joinedAt = joinedAt;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getGameId() { return gameId; }
    public void setGameId(String gameId) { this.gameId = gameId; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    public String getParticipantState() { return participantState; }
    public void setParticipantState(String participantState) { this.participantState = participantState; }
    public Instant getJoinedAt() { return joinedAt; }
    public void setJoinedAt(Instant joinedAt) { this.joinedAt = joinedAt; }
}
