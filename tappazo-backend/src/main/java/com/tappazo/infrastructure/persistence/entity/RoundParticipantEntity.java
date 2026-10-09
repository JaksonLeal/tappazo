package com.tappazo.infrastructure.persistence.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "round_participants", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"round_id", "user_id"})
})
public class RoundParticipantEntity {

    @Id
    @Column(length = 36, nullable = false)
    private String id;

    @Column(name = "round_id", length = 36, nullable = false)
    private String roundId;

    @Column(name = "user_id", length = 36, nullable = false)
    private String userId;

    @Column(name = "nickname_snapshot", length = 50, nullable = false)
    private String nicknameSnapshot;

    @Column(name = "turn_order", nullable = false)
    private int turnOrder;

    @Column(length = 20, nullable = false)
    private String state;

    @Column(name = "confirmed_loser", nullable = false)
    private boolean confirmedLoser;

    @Column(name = "winner", nullable = false)
    private boolean winner;

    @Column(name = "automatic_loser_candidate", nullable = false)
    private boolean automaticLoserCandidate;

    @Column(name = "removed_by_disconnection", nullable = false)
    private boolean removedByDisconnection;

    @Column(name = "reveal_invalid", nullable = false)
    private boolean revealInvalid;

    public RoundParticipantEntity() {}

    public RoundParticipantEntity(String id, String roundId, String userId, String nicknameSnapshot, int turnOrder,
                                  String state, boolean confirmedLoser, boolean winner,
                                  boolean automaticLoserCandidate, boolean removedByDisconnection, boolean revealInvalid) {
        this.id = id;
        this.roundId = roundId;
        this.userId = userId;
        this.nicknameSnapshot = nicknameSnapshot;
        this.turnOrder = turnOrder;
        this.state = state;
        this.confirmedLoser = confirmedLoser;
        this.winner = winner;
        this.automaticLoserCandidate = automaticLoserCandidate;
        this.removedByDisconnection = removedByDisconnection;
        this.revealInvalid = revealInvalid;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getRoundId() { return roundId; }
    public void setRoundId(String roundId) { this.roundId = roundId; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public String getNicknameSnapshot() { return nicknameSnapshot; }
    public void setNicknameSnapshot(String nicknameSnapshot) { this.nicknameSnapshot = nicknameSnapshot; }
    public int getTurnOrder() { return turnOrder; }
    public void setTurnOrder(int turnOrder) { this.turnOrder = turnOrder; }
    public String getState() { return state; }
    public void setState(String state) { this.state = state; }
    public boolean isConfirmedLoser() { return confirmedLoser; }
    public void setConfirmedLoser(boolean confirmedLoser) { this.confirmedLoser = confirmedLoser; }
    public boolean isWinner() { return winner; }
    public void setWinner(boolean winner) { this.winner = winner; }
    public boolean isAutomaticLoserCandidate() { return automaticLoserCandidate; }
    public void setAutomaticLoserCandidate(boolean automaticLoserCandidate) { this.automaticLoserCandidate = automaticLoserCandidate; }
    public boolean isRemovedByDisconnection() { return removedByDisconnection; }
    public void setRemovedByDisconnection(boolean removedByDisconnection) { this.removedByDisconnection = removedByDisconnection; }
    public boolean isRevealInvalid() { return revealInvalid; }
    public void setRevealInvalid(boolean revealInvalid) { this.revealInvalid = revealInvalid; }
}
