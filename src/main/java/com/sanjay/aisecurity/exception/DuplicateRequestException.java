package com.sanjay.aisecurity.exception;

/**
 * Exception thrown when a user attempts an operation that conflicts with
 * an already running identical operation (e.g., triggering a duplicate scan).
 */
public class DuplicateRequestException extends RuntimeException {
    public DuplicateRequestException(String message) {
        super(message);
    }
}
