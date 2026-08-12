package com.sanjay.aisecurity.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Thrown when a requested resource (User, Project, Scan, Report, etc.)
 * is not found, or the authenticated user does not have access to it.
 *
 * <p>Intentionally ambiguous — we use the same exception for "not found"
 * and "access denied to another user's resource" to avoid leaking data
 * about resource existence to unauthorized callers.</p>
 *
 * <p>Mapped to {@code 404 Not Found} HTTP status by the global exception handler.</p>
 *
 * @author Sanjay
 * @version 1.0.0
 */
@ResponseStatus(HttpStatus.NOT_FOUND)
public class ResourceNotFoundException extends RuntimeException {

    /**
     * Constructs a new exception with the specified error message.
     *
     * @param message the detail message
     */
    public ResourceNotFoundException(String message) {
        super(message);
    }

    /**
     * Constructs a new exception with a formatted resource name and identifier.
     *
     * @param resourceName the name of the resource type (e.g. "Project")
     * @param identifier   the identifier that was not found (e.g. the ID)
     */
    public ResourceNotFoundException(String resourceName, Object identifier) {
        super(resourceName + " not found with identifier: " + identifier);
    }
}
