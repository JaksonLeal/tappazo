package com.tappazo.domain.exception;

/**
 * Excepción lanzada cuando un voto es inválido (ej. revelador vota su propia tapa, o vota fuera de turno).
 */
public class InvalidVoteException extends DomainException {
    public InvalidVoteException(String message) {
        super(message);
    }
}
