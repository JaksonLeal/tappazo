package com.tappazo.domain.model;

import com.tappazo.domain.exception.InvalidGameStateException;

import java.time.Instant;
import java.util.Objects;

/**
 * Entidad de dominio Game (Sección 5, 6, 7, 8, 9, 10, 11, 56).
 * Modificaciones de precio, modo o finalización solo permitidas en LOBBY.
 */
public class Game {
    public static final int DEFAULT_MAX_PLAYERS = 10;
    public static final int GLOBAL_MIN_PLAYERS = 2;

    private final String id;
    private final String code;
    private String hostId;
    private GameState state;
    private final int maxPlayers;
    private long currentDrinkPrice; // En pesos COP (Sección 46)
    private GameMode currentMode;
    private Long version;
    private final Instant createdAt;
    private Instant updatedAt;

    public Game(String id, String code, String hostId, int maxPlayers, long currentDrinkPrice, GameMode currentMode) {
        this.id = Objects.requireNonNull(id, "id must not be null");
        this.code = Objects.requireNonNull(code, "code must not be null");
        this.hostId = Objects.requireNonNull(hostId, "hostId must not be null");
        if (maxPlayers < GLOBAL_MIN_PLAYERS) {
            throw new IllegalArgumentException("maxPlayers no puede ser menor a " + GLOBAL_MIN_PLAYERS);
        }
        this.maxPlayers = maxPlayers;
        if (currentDrinkPrice <= 0) {
            throw new IllegalArgumentException("El precio de la bebida debe ser mayor a 0");
        }
        this.currentDrinkPrice = currentDrinkPrice;
        this.currentMode = Objects.requireNonNull(currentMode, "currentMode must not be null");
        this.state = GameState.LOBBY;
        this.version = 0L;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    public Game(String id, String code, String hostId, GameState state, int maxPlayers,
                long currentDrinkPrice, GameMode currentMode, Long version, Instant createdAt, Instant updatedAt) {
        this.id = Objects.requireNonNull(id, "id must not be null");
        this.code = Objects.requireNonNull(code, "code must not be null");
        this.hostId = Objects.requireNonNull(hostId, "hostId must not be null");
        this.state = Objects.requireNonNull(state, "state must not be null");
        this.maxPlayers = maxPlayers;
        this.currentDrinkPrice = currentDrinkPrice;
        this.currentMode = Objects.requireNonNull(currentMode, "currentMode must not be null");
        this.version = version != null ? version : 0L;
        this.createdAt = createdAt != null ? createdAt : Instant.now();
        this.updatedAt = updatedAt != null ? updatedAt : Instant.now();
    }

    /**
     * Modifica el precio de la bebida (Sección 10). Solo permitido si state == LOBBY.
     */
    public void updateDrinkPrice(long newPrice) {
        if (state != GameState.LOBBY) {
            throw new InvalidGameStateException("Solo se puede modificar el precio mientras la partida esté en LOBBY");
        }
        if (newPrice <= 0) {
            throw new IllegalArgumentException("El precio de la bebida debe ser mayor a 0");
        }
        this.currentDrinkPrice = newPrice;
        this.updatedAt = Instant.now();
    }

    /**
     * Modifica la modalidad de juego (Sección 10). Solo permitido si state == LOBBY.
     */
    public void changeMode(GameMode newMode) {
        if (state != GameState.LOBBY) {
            throw new InvalidGameStateException("Solo se puede cambiar la modalidad mientras la partida esté en LOBBY");
        }
        this.currentMode = Objects.requireNonNull(newMode, "newMode must not be null");
        this.updatedAt = Instant.now();
    }

    /**
     * Inicia una ronda: pasa el estado del Game a IN_PROGRESS (Sección 11).
     */
    public void startRound() {
        if (state != GameState.LOBBY) {
            throw new InvalidGameStateException("No se puede iniciar una ronda si el juego no está en LOBBY");
        }
        this.state = GameState.IN_PROGRESS;
        this.updatedAt = Instant.now();
    }

    /**
     * Finaliza una ronda en curso: devuelve el estado del Game a LOBBY (Sección 11).
     */
    public void roundFinished() {
        if (state != GameState.IN_PROGRESS) {
            throw new InvalidGameStateException("No se puede regresar a LOBBY si el juego no está IN_PROGRESS");
        }
        this.state = GameState.LOBBY;
        this.updatedAt = Instant.now();
    }

    /**
     * Finaliza la partida completa (Sección 9, 11). Solo permitido desde LOBBY, nunca a mitad de una ronda.
     */
    public void finishGame() {
        if (state != GameState.LOBBY) {
            throw new InvalidGameStateException("Solo se puede finalizar la partida mientras esté en LOBBY, nunca a mitad de una ronda");
        }
        this.state = GameState.FINISHED;
        this.updatedAt = Instant.now();
    }

    /**
     * Cambia el host de la partida (Sección 9).
     */
    public void changeHost(String newHostId) {
        this.hostId = Objects.requireNonNull(newHostId, "newHostId must not be null");
        this.updatedAt = Instant.now();
    }

    public String getId() {
        return id;
    }

    public String getCode() {
        return code;
    }

    public String getHostId() {
        return hostId;
    }

    public GameState getState() {
        return state;
    }

    public int getMaxPlayers() {
        return maxPlayers;
    }

    public long getCurrentDrinkPrice() {
        return currentDrinkPrice;
    }

    public GameMode getCurrentMode() {
        return currentMode;
    }

    public Long getVersion() {
        return version;
    }

    public void incrementVersion() {
        this.version = (this.version != null ? this.version : 0L) + 1;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
