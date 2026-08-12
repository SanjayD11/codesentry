package com.sanjay.aisecurity.enums;

/**
 * Represents the static analysis scanning status of an uploaded file or scan history.
 *
 * @author Sanjay
 * @version 1.0.0
 */
public enum ScanStatus {
    NOT_SCANNED,
    PENDING,
    RUNNING,
    IN_PROGRESS,
    COMPLETED,
    FAILED,
    CANCELLED
}
