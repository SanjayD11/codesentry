package com.sanjay.aisecurity.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Thrown during file upload operations when a file fails validation
 * checks (unsupported extension, invalid MIME type, empty file, etc.).
 *
 * <p>Mapped to {@code 400 Bad Request} HTTP status by the global exception handler.</p>
 *
 * @author Sanjay
 * @version 1.0.0
 */
@ResponseStatus(HttpStatus.BAD_REQUEST)
public class InvalidFileException extends RuntimeException {

    /**
     * Constructs a new exception with the specified detail message.
     *
     * @param message the detail message explaining the validation failure
     */
    public InvalidFileException(String message) {
        super(message);
    }

    /**
     * Constructs a new exception with a detail message and a root cause.
     *
     * @param message the detail message
     * @param cause   the underlying cause
     */
    public InvalidFileException(String message, Throwable cause) {
        super(message, cause);
    }
}
