package com.tappazo.domain.model;

/**
 * Estados del participante tanto a nivel de partida como a nivel de ronda (Sección 13, 57, 59).
 * El estado a nivel de ronda es independiente del estado a nivel de partida.
 */
public enum ParticipantState {
    ACTIVE,
    STAND_BY,
    REMOVED
}
