package com.tappazo.domain.model;

/**
 * Modalidades de juego disponibles en Tappazo (Sección 28).
 */
public enum GameMode {
    ULTIMO_PIERDE(2),
    PEQUENOS_PAGAN(3),
    ULTIMOS_DOS_PIERDEN(4);

    private final int minPlayers;

    GameMode(int minPlayers) {
        this.minPlayers = minPlayers;
    }

    public int getMinPlayers() {
        return minPlayers;
    }
}
