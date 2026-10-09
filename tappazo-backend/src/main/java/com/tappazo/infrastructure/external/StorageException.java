package com.tappazo.infrastructure.external;

/**
 * Excepción lanzada cuando ocurre un error en las operaciones de almacenamiento externo.
 */
public class StorageException extends RuntimeException {

    public StorageException(String message) {
        super(message);
    }

    public StorageException(String message, Throwable cause) {
        super(message, cause);
    }
}
