package com.sanjay.aisecurity.exception;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Standardized error response body returned by the global exception handler.
 *
 * <p>Provides consistent error payloads that clients can reliably parse.
 * Field validation errors are included in the optional {@code errors} map.</p>
 *
 * @author Sanjay
 * @version 1.0.0
 */
@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponse {

    /** Whether the operation was successful — always {@code false} for errors. */
    private final boolean success;

    /** HTTP status code of the error response. */
    private final int status;

    /** Human-readable error message. */
    private final String message;

    /**
     * Field-level validation errors.
     * Key = field name, Value = validation failure message.
     * Only present for {@code 400} validation errors.
     */
    private final Map<String, String> errors;

    /** Timestamp when the error response was generated. */
    private final LocalDateTime timestamp;
}
