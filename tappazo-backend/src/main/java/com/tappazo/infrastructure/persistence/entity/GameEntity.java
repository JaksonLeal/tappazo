package com.tappazo.infrastructure.persistence.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "games")
public class GameEntity {

    @Id
    @Column(length = 36, nullable = false)
    private String id;

    @Column(length = 10, nullable = false, unique = true)
    private String code;

    @Column(name = "host_id", length = 36, nullable = false)
    private String hostId;

    @Column(length = 20, nullable = false)
    private String state;

    @Column(name = "max_players", nullable = false)
    private int maxPlayers;

    @Column(name = "current_drink_price", nullable = false)
    private long currentDrinkPrice;

    @Column(name = "current_mode", length = 30, nullable = false)
    private String currentMode;

    @Version
    @Column(nullable = false)
    private Long version;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public GameEntity() {}

    public GameEntity(String id, String code, String hostId, String state, int maxPlayers,
                      long currentDrinkPrice, String currentMode, Long version, Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.code = code;
        this.hostId = hostId;
        this.state = state;
        this.maxPlayers = maxPlayers;
        this.currentDrinkPrice = currentDrinkPrice;
        this.currentMode = currentMode;
        this.version = version;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getHostId() { return hostId; }
    public void setHostId(String hostId) { this.hostId = hostId; }
    public String getState() { return state; }
    public void setState(String state) { this.state = state; }
    public int getMaxPlayers() { return maxPlayers; }
    public void setMaxPlayers(int maxPlayers) { this.maxPlayers = maxPlayers; }
    public long getCurrentDrinkPrice() { return currentDrinkPrice; }
    public void setCurrentDrinkPrice(long currentDrinkPrice) { this.currentDrinkPrice = currentDrinkPrice; }
    public String getCurrentMode() { return currentMode; }
    public void setCurrentMode(String currentMode) { this.currentMode = currentMode; }
    public Long getVersion() { return version; }
    public void setVersion(Long version) { this.version = version; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
