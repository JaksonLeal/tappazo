package com.tappazo.domain.exception;

/**
 * Excepción lanzada cuando no se cumple el número mínimo de jugadores (Sección 12).
 */
public class InsufficientPlayersException extends DomainException {
    public InsufficientPlayersException(String message) {
        super(message);
    }
}
