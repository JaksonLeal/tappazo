package com.tappazo.infrastructure.persistence.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "player_statistics")
public class PlayerStatisticsEntity {

    @Id
    @Column(name = "user_id", length = 36, nullable = false)
    private String userId;

    @Column(name = "rounds_played", nullable = false)
    private int roundsPlayed;

    @Column(name = "rounds_won", nullable = false)
    private int roundsWon;

    @Column(name = "rounds_lost", nullable = false)
    private int roundsLost;

    @Column(name = "money_spent", nullable = false)
    private long moneySpent;

    @Column(name = "money_received", nullable = false)
    private long moneyReceived;

    public PlayerStatisticsEntity() {}

    public PlayerStatisticsEntity(String userId, int roundsPlayed, int roundsWon, int roundsLost, long moneySpent, long moneyReceived) {
        this.userId = userId;
        this.roundsPlayed = roundsPlayed;
        this.roundsWon = roundsWon;
        this.roundsLost = roundsLost;
        this.moneySpent = moneySpent;
        this.moneyReceived = moneyReceived;
    }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public int getRoundsPlayed() { return roundsPlayed; }
    public void setRoundsPlayed(int roundsPlayed) { this.roundsPlayed = roundsPlayed; }
    public int getRoundsWon() { return roundsWon; }
    public void setRoundsWon(int roundsWon) { this.roundsWon = roundsWon; }
    public int getRoundsLost() { return roundsLost; }
    public void setRoundsLost(int roundsLost) { this.roundsLost = roundsLost; }
    public long getMoneySpent() { return moneySpent; }
    public void setMoneySpent(long moneySpent) { this.moneySpent = moneySpent; }
    public long getMoneyReceived() { return moneyReceived; }
    public void setMoneyReceived(long moneyReceived) { this.moneyReceived = moneyReceived; }
}
