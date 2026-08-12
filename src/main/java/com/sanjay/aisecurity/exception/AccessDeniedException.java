package com.sanjay.aisecurity.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Thrown when an authenticated user attempts to perform an operation
 * on a resource they do not own or have explicit permission to access.
 *
 * <p>Mapped to {@code 403 Forbidden} HTTP status by the global exception handler.</p>
 *
 * @author Sanjay
 * @version 1.0.0
 */
@ResponseStatus(HttpStatus.FORBIDDEN)
public class AccessDeniedException extends RuntimeException {

    /**
     * Constructs a new exception with the specified detail message.
     *
     * @param message the detail message
     */
    public AccessDeniedException(String message) {
        super(message);
    }
}
