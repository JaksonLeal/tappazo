package com.tappazo.domain.exception;

/**
 * Excepción lanzada cuando una operación no es válida para el estado actual del Game o Round.
 */
public class InvalidGameStateException extends DomainException {
    public InvalidGameStateException(String message) {
        super(message);
    }
}
