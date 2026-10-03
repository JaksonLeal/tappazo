package com.tappazo.domain.model;

/**
 * Motivos por los que se origina un movimiento de deuda (Sección 50).
 */
public enum DebtReason {
    ROUND_LOSS,
    TIE_BREAK,
    TIE_BREAK_ROUND,
    STAND_BY_REMOVAL,
    REVEAL_INVALID
}
