package com.sanjay.aisecurity.service;

import com.sanjay.aisecurity.dto.response.EnrichedVulnerabilityResponse;

/**
 * Service for enriching vulnerability findings with AI-generated context.
 *
 * @author Sanjay
 * @version 1.0.0
 */
public interface AiEnrichmentService {

    /**
     * Enriches a single vulnerability with AI-generated explanation, root cause,
     * business impact, and secure code example.
     *
     * @param vulnerabilityId the vulnerability to enrich
     * @return the enriched vulnerability response
     */
    EnrichedVulnerabilityResponse enrichVulnerability(Long vulnerabilityId);

    /**
     * Forces a retry of AI enrichment for a specific vulnerability, bypassing cache.
     *
     * @param vulnerabilityId the vulnerability to retry
     * @return the enriched vulnerability response
     */
    EnrichedVulnerabilityResponse retryVulnerability(Long vulnerabilityId);

    /**
     * Triggers asynchronous enrichment for all vulnerabilities in a scan.
     * Returns immediately; enrichment runs in background.
     *
     * @param scanId the scan history ID
     */
    void enrichScanAsync(Long scanId);

    /**
     * Retrieves the current enrichment state of a vulnerability.
     *
     * @param vulnerabilityId the vulnerability ID
     * @return the enriched vulnerability response
     */
    EnrichedVulnerabilityResponse getEnrichment(Long vulnerabilityId);
}
