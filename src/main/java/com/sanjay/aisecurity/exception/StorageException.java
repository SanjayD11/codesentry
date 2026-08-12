package com.sanjay.aisecurity.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Thrown when a file system operation fails (disk write, directory creation,
 * file read, or file deletion). Typically wraps an {@code IOException}.
 *
 * <p>Mapped to {@code 500 Internal Server Error} HTTP status by the global exception handler.</p>
 *
 * @author Sanjay
 * @version 1.0.0
 */
@ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
public class StorageException extends RuntimeException {

    /**
     * Constructs a new exception with the specified detail message.
     *
     * @param message the detail message
     */
    public StorageException(String message) {
        super(message);
    }

    /**
     * Constructs a new exception with a detail message and a root cause.
     *
     * @param message the detail message
     * @param cause   the underlying IOException or similar
     */
    public StorageException(String message, Throwable cause) {
        super(message, cause);
    }
}
