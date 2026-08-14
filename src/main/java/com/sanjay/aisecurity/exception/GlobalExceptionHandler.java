package com.sanjay.aisecurity.exception;

import com.sanjay.aisecurity.constants.MessageConstants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Global Exception Handler.
 *
 * <p>Intercepts all exceptions thrown by controllers and services and converts
 * them into standardized {@link ErrorResponse} JSON payloads with appropriate
 * HTTP status codes. This ensures no raw exception details are ever exposed
 * to API clients.</p>
 *
 * <p>Responsibilities:</p>
 * <ul>
 *   <li>Handles Bean Validation failures with field-level error maps</li>
 *   <li>Maps custom domain exceptions to correct HTTP status codes</li>
 *   <li>Handles Spring Security authentication/authorization exceptions</li>
 *   <li>Handles file upload size exceeded errors</li>
 *   <li>Catches all unhandled exceptions with a generic 500 response</li>
 * </ul>
 *
 * @author Sanjay
 * @version 1.0.0
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    // =========================================================================
    // VALIDATION ERRORS
    // =========================================================================

    /**
     * Handles {@code @Valid} bean validation failures.
     * Returns a map of field name → error message pairs.
     *
     * @param ex the validation exception
     * @return 400 Bad Request with field-level error details
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationErrors(
            MethodArgumentNotValidException ex) {

        Map<String, String> fieldErrors = new HashMap<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            fieldErrors.put(error.getField(), error.getDefaultMessage());
        }

        log.warn("Validation failed: {}", fieldErrors);

        ErrorResponse response = ErrorResponse.builder()
                .success(false)
                .status(HttpStatus.BAD_REQUEST.value())
                .message(MessageConstants.VALIDATION_FAILED)
                .errors(fieldErrors)
                .timestamp(LocalDateTime.now())
                .build();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    // =========================================================================
    // CUSTOM DOMAIN EXCEPTIONS
    // =========================================================================

    /**
     * Handles duplicate user registration attempts or duplicate scan requests.
     *
     * @param ex the conflict exception
     * @return 409 Conflict
     */
    @ExceptionHandler({UserAlreadyExistsException.class, DuplicateRequestException.class})
    public ResponseEntity<ErrorResponse> handleConflictExceptions(
            RuntimeException ex) {

        log.warn("Conflict detected: {}", ex.getMessage());
        return buildResponse(HttpStatus.CONFLICT, ex.getMessage());
    }

    /**
     * Handles resource not found — used for all entity types (Project, Scan, etc.).
     *
     * @param ex the not found exception
     * @return 404 Not Found
     */
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleResourceNotFound(
            ResourceNotFoundException ex) {

        log.warn("Resource not found: {}", ex.getMessage());
        return buildResponse(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    /**
     * Handles access denied to another user's resource.
     *
     * @param ex the access denied exception
     * @return 403 Forbidden
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(
            AccessDeniedException ex) {

        log.warn("Access denied: {}", ex.getMessage());
        return buildResponse(HttpStatus.FORBIDDEN, ex.getMessage());
    }

    /**
     * Handles invalid file upload validations (extension, MIME type, empty file).
     *
     * @param ex the invalid file exception
     * @return 400 Bad Request
     */
    @ExceptionHandler(InvalidFileException.class)
    public ResponseEntity<ErrorResponse> handleInvalidFile(
            InvalidFileException ex) {

        log.warn("Invalid file upload: {}", ex.getMessage());
        return buildResponse(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    /**
     * Handles file system storage failures.
     *
     * @param ex the storage exception
     * @return 500 Internal Server Error
     */
    @ExceptionHandler(StorageException.class)
    public ResponseEntity<ErrorResponse> handleStorageError(
            StorageException ex) {

        log.error("Storage error: {}", ex.getMessage(), ex);
        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, ex.getMessage());
    }

    /**
     * Handles AI provider failures (timeouts, rate limits, parse errors).
     *
     * @param ex the AI provider exception
     * @return 502 Bad Gateway
     */
    @ExceptionHandler(AiProviderException.class)
    public ResponseEntity<ErrorResponse> handleAiProviderError(
            AiProviderException ex) {

        log.error("AI provider error: {}", ex.getMessage(), ex);
        return buildResponse(HttpStatus.BAD_GATEWAY, ex.getMessage());
    }

    /**
     * Handles user rate limits (too many AI requests).
     *
     * @param ex the rate limit exception
     * @return 429 Too Many Requests
     */
    @ExceptionHandler(RateLimitException.class)
    public ResponseEntity<ErrorResponse> handleRateLimitError(
            RateLimitException ex) {

        log.warn("Rate limit exceeded: {}", ex.getMessage());
        return buildResponse(HttpStatus.TOO_MANY_REQUESTS, ex.getMessage());
    }

    // =========================================================================
    // SPRING SECURITY EXCEPTIONS
    // =========================================================================

    /**
     * Handles invalid login credentials (wrong email or password).
     *
     * @param ex the bad credentials exception
     * @return 401 Unauthorized
     */
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ErrorResponse> handleBadCredentials(
            BadCredentialsException ex) {

        log.warn("Authentication failed: bad credentials");
        return buildResponse(HttpStatus.UNAUTHORIZED, MessageConstants.INVALID_CREDENTIALS);
    }

    /**
     * Handles login attempts for deactivated accounts.
     *
     * @param ex the disabled exception
     * @return 403 Forbidden
     */
    @ExceptionHandler(DisabledException.class)
    public ResponseEntity<ErrorResponse> handleAccountDisabled(
            DisabledException ex) {

        log.warn("Login attempt for disabled account");
        return buildResponse(HttpStatus.FORBIDDEN, MessageConstants.ACCOUNT_DISABLED);
    }

    /**
     * Handles Spring Security's built-in access denied (overrides default behavior).
     *
     * @param ex the Spring Security access denied exception
     * @return 403 Forbidden
     */
    @ExceptionHandler(org.springframework.security.access.AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleSpringAccessDenied(
            org.springframework.security.access.AccessDeniedException ex) {

        log.warn("Spring Security access denied: {}", ex.getMessage());
        return buildResponse(HttpStatus.FORBIDDEN, MessageConstants.ACCESS_DENIED);
    }

    // =========================================================================
    // FILE UPLOAD SIZE EXCEEDED
    // =========================================================================

    /**
     * Handles multipart file size exceeded errors (beyond configured limits).
     *
     * @param ex the max size exception
     * @return 413 Payload Too Large
     */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ErrorResponse> handleMaxUploadSize(
            MaxUploadSizeExceededException ex) {

        log.warn("File upload size exceeded: {}", ex.getMessage());
        return buildResponse(HttpStatus.PAYLOAD_TOO_LARGE, MessageConstants.FILE_TOO_LARGE);
    }

    /**
     * Handles illegal arguments passed to services.
     *
     * @param ex the illegal argument exception
     * @return 400 Bad Request
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException ex) {
        log.warn("Invalid arguments: {}", ex.getMessage());
        return buildResponse(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    /**
     * Handles missing multipart request parts (e.g. no 'files' field in upload).
     *
     * @param ex the missing part exception
     * @return 400 Bad Request
     */
    @ExceptionHandler(org.springframework.web.multipart.support.MissingServletRequestPartException.class)
    public ResponseEntity<ErrorResponse> handleMissingPart(
            org.springframework.web.multipart.support.MissingServletRequestPartException ex) {
        log.warn("Missing multipart request part: {}", ex.getMessage());
        return buildResponse(HttpStatus.BAD_REQUEST,
                "File upload failed: required part '" + ex.getRequestPartName() + "' is missing. Please select a file.");
    }

    /**
     * Handles generic multipart parsing errors (corrupted upload, connection reset, etc.).
     *
     * @param ex the multipart exception
     * @return 400 Bad Request
     */
    @ExceptionHandler(org.springframework.web.multipart.MultipartException.class)
    public ResponseEntity<ErrorResponse> handleMultipartError(
            org.springframework.web.multipart.MultipartException ex) {
        log.warn("Multipart request error: {}", ex.getMessage());
        return buildResponse(HttpStatus.BAD_REQUEST,
                "File upload failed: the file could not be processed. Please try again.");
    }

    /**
     * Handles unsupported Content-Type headers.
     *
     * @param ex the media type exception
     * @return 415 Unsupported Media Type
     */
    @ExceptionHandler(org.springframework.web.HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleUnsupportedMediaType(
            org.springframework.web.HttpMediaTypeNotSupportedException ex) {
        log.warn("Unsupported media type: {}", ex.getMessage());
        return buildResponse(HttpStatus.UNSUPPORTED_MEDIA_TYPE,
                "Unsupported content type. File uploads require multipart/form-data.");
    }

    // =========================================================================
    // CATCH-ALL FALLBACK
    // =========================================================================

    /**
     * Catch-all handler for any unhandled exception.
     * Logs the full stack trace and returns a generic 500 response.
     *
     * @param ex the unhandled exception
     * @return 500 Internal Server Error
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(Exception ex) {
        log.error("Unexpected error occurred: {}", ex.getMessage(), ex);
        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR,
                MessageConstants.INTERNAL_SERVER_ERROR);
    }

    // =========================================================================
    // HELPER
    // =========================================================================

    /**
     * Builds a standard {@link ErrorResponse} entity for a given status and message.
     *
     * @param status  the HTTP status
     * @param message the user-facing error message
     * @return the wrapped {@link ResponseEntity}
     */
    private ResponseEntity<ErrorResponse> buildResponse(HttpStatus status, String message) {
        ErrorResponse response = ErrorResponse.builder()
                .success(false)
                .status(status.value())
                .message(message)
                .timestamp(LocalDateTime.now())
                .build();
        return ResponseEntity.status(status).body(response);
    }
}
