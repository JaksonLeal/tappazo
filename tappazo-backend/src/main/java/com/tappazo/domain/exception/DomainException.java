package com.tappazo.domain.exception;

/**
 * Excepción base para violaciones de reglas de negocio en el dominio de Tappazo.
 */
public class DomainException extends RuntimeException {
    public DomainException(String message) {
        super(message);
    }

    public DomainException(String message, Throwable cause) {
        super(message, cause);
    }
}
