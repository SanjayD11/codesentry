package com.sanjay.aisecurity.enums;

/**
 * Represents the state of the AI enrichment process for a vulnerability.
 *
 * @author Sanjay
 * @version 1.0.0
 */
public enum AiStatus {
    /**
     * AI enrichment has not yet been started or is queued.
     */
    PENDING,

    /**
     * AI enrichment is currently in progress.
     */
    PROCESSING,

    /**
     * AI enrichment completed successfully.
     */
    COMPLETED,

    /**
     * AI enrichment failed (either permanent error or max retries exceeded).
     */
    FAILED
}
