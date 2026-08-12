package com.sanjay.aisecurity.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Thrown when a user registration attempt is made with an email address
 * that already exists in the database.
 *
 * <p>Mapped to {@code 409 Conflict} HTTP status by the global exception handler.</p>
 *
 * @author Sanjay
 * @version 1.0.0
 */
@ResponseStatus(HttpStatus.CONFLICT)
public class UserAlreadyExistsException extends RuntimeException {

    /**
     * Constructs a new exception with the specified error message.
     *
     * @param message the detail message explaining the conflict
     */
    public UserAlreadyExistsException(String message) {
        super(message);
    }
}
