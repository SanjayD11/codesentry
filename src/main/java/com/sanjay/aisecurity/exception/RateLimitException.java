package com.sanjay.aisecurity.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Thrown when a user exceeds their allowed AI request rate limit.
 * Mapped to 429 Too Many Requests.
 *
 * @author Sanjay
 * @version 1.0.0
 */
@ResponseStatus(HttpStatus.TOO_MANY_REQUESTS)
public class RateLimitException extends RuntimeException {

    public RateLimitException(String message) {
        super(message);
    }
}
