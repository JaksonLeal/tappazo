package com.tappazo.domain.model;

/**
 * Estados del ciclo de vida de una ronda (Round) (Sección 11, 58).
 */
public enum RoundState {
    STARTING,
    REVEALING,
    VALIDATING,
    RESOLVING,
    TIE_BREAK,
    FINISHED
}
