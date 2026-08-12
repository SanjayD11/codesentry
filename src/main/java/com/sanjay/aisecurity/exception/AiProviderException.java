package com.sanjay.aisecurity.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Thrown when an AI provider returns an error, times out, exceeds
 * rate limits, or returns an unparseable response.
 *
 * <p>Mapped to {@code 502 Bad Gateway} HTTP status by the global exception
 * handler to clearly distinguish AI provider failures from internal errors.</p>
 *
 * @author Sanjay
 * @version 1.0.0
 */
@ResponseStatus(HttpStatus.BAD_GATEWAY)
public class AiProviderException extends RuntimeException {

    /**
     * Constructs a new exception with the specified detail message.
     *
     * @param message the detail message
     */
    public AiProviderException(String message) {
        super(message);
    }

    /**
     * Constructs a new exception with a detail message and a root cause.
     *
     * @param message the detail message
     * @param cause   the underlying cause (timeout, connection error, etc.)
     */
    public AiProviderException(String message, Throwable cause) {
        super(message, cause);
    }
}
