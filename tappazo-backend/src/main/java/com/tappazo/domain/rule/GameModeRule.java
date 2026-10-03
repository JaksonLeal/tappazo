package com.tappazo.domain.rule;

import com.tappazo.domain.model.GameMode;
import com.tappazo.domain.model.GameResult;
import com.tappazo.domain.model.PlayerResult;

import java.util.List;

/**
 * Patrón Strategy para las reglas de modalidad de juego (Sección 28, 45).
 * El modo es lógica pura de dominio, sin conocer HTTP, WebSocket, JPA ni MySQL.
 */
public interface GameModeRule {

    /**
     * Mínimo estricto de jugadores requerido para iniciar en esta modalidad (Sección 12).
     */
    int minimoJugadores();

    /**
     * Modalidad asociada.
     */
    GameMode getMode();

    /**
     * Calcula la cantidad de loser slots para un número dado de jugadores.
     */
    int calculateLoserSlots(int totalPlayers);

    /**
     * Resuelve los resultados de la ronda para esta modalidad (Sección 28).
     */
    GameResult resolve(List<PlayerResult> results);
}
