package com.tappazo.domain.model;

import java.util.Objects;

/**
 * Participante a nivel de ronda (Sección 13, 59).
 * El estado a nivel de ronda es independiente del de la partida.
 * Un REMOVED de ronda no saca al jugador de la partida completa (Sección 29).
 */
public class RoundParticipant {
    private final String id;
    private final String roundId;
    private final String userId;
    private final String nicknameSnapshot;
    private final int turnOrder;
    private ParticipantState state;
    private boolean confirmedLoser;
    private boolean winner;
    private boolean automaticLoserCandidate;
    private boolean removedByDisconnection;
    private boolean revealInvalid;

    public RoundParticipant(String id, String roundId, String userId, String nicknameSnapshot, int turnOrder) {
        this.id = Objects.requireNonNull(id, "id must not be null");
        this.roundId = Objects.requireNonNull(roundId, "roundId must not be null");
        this.userId = Objects.requireNonNull(userId, "userId must not be null");
        this.nicknameSnapshot = Objects.requireNonNull(nicknameSnapshot, "nicknameSnapshot must not be null");
        this.turnOrder = turnOrder;
        this.state = ParticipantState.ACTIVE;
        this.confirmedLoser = false;
        this.winner = false;
        this.automaticLoserCandidate = false;
        this.removedByDisconnection = false;
        this.revealInvalid = false;
    }

    public RoundParticipant(String id, String roundId, String userId, String nicknameSnapshot, int turnOrder,
                            ParticipantState state, boolean confirmedLoser, boolean winner,
                            boolean automaticLoserCandidate, boolean removedByDisconnection, boolean revealInvalid) {
        this.id = Objects.requireNonNull(id, "id must not be null");
        this.roundId = Objects.requireNonNull(roundId, "roundId must not be null");
        this.userId = Objects.requireNonNull(userId, "userId must not be null");
        this.nicknameSnapshot = Objects.requireNonNull(nicknameSnapshot, "nicknameSnapshot must not be null");
        this.turnOrder = turnOrder;
        this.state = Objects.requireNonNull(state, "state must not be null");
        this.confirmedLoser = confirmedLoser;
        this.winner = winner;
        this.automaticLoserCandidate = automaticLoserCandidate;
        this.removedByDisconnection = removedByDisconnection;
        this.revealInvalid = revealInvalid;
    }

    public void markStandBy() {
        this.state = ParticipantState.STAND_BY;
    }

    public void markActive() {
        this.state = ParticipantState.ACTIVE;
    }

    public void markRemovedByDisconnection() {
        this.state = ParticipantState.REMOVED;
        this.removedByDisconnection = true;
        this.automaticLoserCandidate = true;
    }

    public void markRevealInvalid() {
        this.revealInvalid = true;
        this.automaticLoserCandidate = true;
    }

    public void markConfirmedLoser() {
        this.confirmedLoser = true;
        this.winner = false;
    }

    public void markWinner() {
        this.winner = true;
        this.confirmedLoser = false;
    }

    public String getId() {
        return id;
    }

    public String getRoundId() {
        return roundId;
    }

    public String getUserId() {
        return userId;
    }

    public String getNicknameSnapshot() {
        return nicknameSnapshot;
    }

    public int getTurnOrder() {
        return turnOrder;
    }

    public ParticipantState getState() {
        return state;
    }

    public boolean isConfirmedLoser() {
        return confirmedLoser;
    }

    public boolean isWinner() {
        return winner;
    }

    public boolean isAutomaticLoserCandidate() {
        return automaticLoserCandidate;
    }

    public boolean isRemovedByDisconnection() {
        return removedByDisconnection;
    }

    public boolean isRevealInvalid() {
        return revealInvalid;
    }
}
