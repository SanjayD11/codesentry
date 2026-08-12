package com.sanjay.aisecurity.common;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;

/**
 * Generic API Response Wrapper.
 *
 * <p>Standardizes the JSON structure of every REST endpoint response in
 * the platform. All controllers return {@code ApiResponse<T>} to ensure
 * consistent field presence across success and error scenarios.</p>
 *
 * <p>Example JSON output:</p>
 * <pre>{@code
 * {
 *   "success": true,
 *   "status": 200,
 *   "message": "Project created successfully.",
 *   "data": { ... },
 *   "timestamp": "2024-01-15T10:30:00"
 * }
 * }</pre>
 *
 * @param <T> the type of the response data payload
 * @author Sanjay
 * @version 1.0.0
 */
@Getter
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {

    /** Whether the operation completed successfully. */
    private boolean success;

    /** HTTP status code of the response. */
    private int status;

    /** Human-readable message describing the result. */
    private String message;

    /** The response payload (null for error responses or empty operations). */
    private T data;

    /** Timestamp when the response was generated. */
    private LocalDateTime timestamp;

    // =========================================================================
    // PRIVATE CONSTRUCTOR — use static factory methods
    // =========================================================================

    private ApiResponse(boolean success, int status, String message, T data) {
        this.success   = success;
        this.status    = status;
        this.message   = message;
        this.data      = data;
        this.timestamp = LocalDateTime.now();
    }

    // =========================================================================
    // SUCCESS FACTORY METHODS
    // =========================================================================

    /**
     * Creates a 200 OK success response with data.
     *
     * @param message human-readable success message
     * @param data    the response payload
     * @param <T>     payload type
     * @return {@code ApiResponse} representing success
     */
    public static <T> ApiResponse<T> success(String message, T data) {
        return new ApiResponse<>(true, HttpStatus.OK.value(), message, data);
    }

    /**
     * Creates a 201 CREATED success response with data.
     *
     * @param message human-readable creation message
     * @param data    the created resource payload
     * @param <T>     payload type
     * @return {@code ApiResponse} representing a resource creation
     */
    public static <T> ApiResponse<T> created(String message, T data) {
        return new ApiResponse<>(true, HttpStatus.CREATED.value(), message, data);
    }

    /**
     * Creates a 200 OK success response without a data payload.
     *
     * @param message human-readable success message
     * @param <T>     unused payload type
     * @return {@code ApiResponse} with no data
     */
    public static <T> ApiResponse<T> success(String message) {
        return new ApiResponse<>(true, HttpStatus.OK.value(), message, null);
    }

    // =========================================================================
    // ERROR FACTORY METHODS
    // =========================================================================

    /**
     * Creates an error response for a given HTTP status.
     *
     * @param status  the HTTP status code
     * @param message the error message
     * @param <T>     unused payload type
     * @return {@code ApiResponse} representing an error
     */
    public static <T> ApiResponse<T> error(HttpStatus status, String message) {
        return new ApiResponse<>(false, status.value(), message, null);
    }

    /**
     * Creates a 400 Bad Request error response.
     *
     * @param message the validation or request error message
     * @param <T>     unused payload type
     * @return {@code ApiResponse} representing a bad request
     */
    public static <T> ApiResponse<T> badRequest(String message) {
        return error(HttpStatus.BAD_REQUEST, message);
    }

    /**
     * Creates a 404 Not Found error response.
     *
     * @param message the not found message
     * @param <T>     unused payload type
     * @return {@code ApiResponse} representing a not found error
     */
    public static <T> ApiResponse<T> notFound(String message) {
        return error(HttpStatus.NOT_FOUND, message);
    }

    /**
     * Creates a 500 Internal Server Error response.
     *
     * @param message the server error message
     * @param <T>     unused payload type
     * @return {@code ApiResponse} representing an internal error
     */
    public static <T> ApiResponse<T> internalError(String message) {
        return error(HttpStatus.INTERNAL_SERVER_ERROR, message);
    }
}
