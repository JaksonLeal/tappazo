package com.tappazo.domain.model;

import java.time.Instant;
import java.util.Objects;

/**
 * Participante a nivel de partida (Sección 8, 13, 57).
 */
public class GameParticipant {
    private final String id;
    private final String gameId;
    private final String userId;
    private ParticipantRole role;
    private ParticipantState participantState;
    private final Instant joinedAt;

    public GameParticipant(String id, String gameId, String userId, ParticipantRole role, ParticipantState participantState, Instant joinedAt) {
        this.id = Objects.requireNonNull(id, "id must not be null");
        this.gameId = Objects.requireNonNull(gameId, "gameId must not be null");
        this.userId = Objects.requireNonNull(userId, "userId must not be null");
        this.role = Objects.requireNonNull(role, "role must not be null");
        this.participantState = Objects.requireNonNull(participantState, "participantState must not be null");
        this.joinedAt = joinedAt != null ? joinedAt : Instant.now();
    }

    public static GameParticipant createPlayer(String id, String gameId, String userId) {
        return new GameParticipant(id, gameId, userId, ParticipantRole.PLAYER, ParticipantState.ACTIVE, Instant.now());
    }

    public static GameParticipant createSpectator(String id, String gameId, String userId) {
        return new GameParticipant(id, gameId, userId, ParticipantRole.SPECTATOR, ParticipantState.ACTIVE, Instant.now());
    }

    /**
     * Promueve automáticamente un espectador a jugador al terminar la ronda (Sección 8).
     */
    public void promoteToPlayer() {
        if (this.role == ParticipantRole.SPECTATOR) {
            this.role = ParticipantRole.PLAYER;
        }
    }

    public void markStandBy() {
        this.participantState = ParticipantState.STAND_BY;
    }

    public void markActive() {
        this.participantState = ParticipantState.ACTIVE;
    }

    public void markRemoved() {
        this.participantState = ParticipantState.REMOVED;
    }

    public String getId() {
        return id;
    }

    public String getGameId() {
        return gameId;
    }

    public String getUserId() {
        return userId;
    }

    public ParticipantRole getRole() {
        return role;
    }

    public ParticipantState getParticipantState() {
        return participantState;
    }

    public Instant getJoinedAt() {
        return joinedAt;
    }

    public boolean isPlayer() {
        return this.role == ParticipantRole.PLAYER;
    }

    public boolean isSpectator() {
        return this.role == ParticipantRole.SPECTATOR;
    }

    public boolean isActive() {
        return this.participantState == ParticipantState.ACTIVE;
    }
}
