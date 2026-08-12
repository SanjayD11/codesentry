package com.sanjay.aisecurity.controller;

import com.sanjay.aisecurity.common.ApiResponse;
import com.sanjay.aisecurity.constants.MessageConstants;
import com.sanjay.aisecurity.dto.response.EnrichedVulnerabilityResponse;
import com.sanjay.aisecurity.service.AiEnrichmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Controller for AI Vulnerability Enrichment operations.
 *
 * @author Sanjay
 * @version 1.0.0
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/ai")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "AI Enrichment", description = "Endpoints for enriching vulnerabilities with AI-generated explanations and remediation guidance.")
public class AiEnrichmentController {

    private final AiEnrichmentService aiEnrichmentService;

    @PostMapping("/enrich/vulnerability/{vulnerabilityId}")
    @Operation(summary = "Enrich a single vulnerability", description = "Calls the AI provider to generate explanation, root cause, business impact, and secure code fix for a specific vulnerability.")
    public ResponseEntity<ApiResponse<EnrichedVulnerabilityResponse>> enrichVulnerability(
            @PathVariable Long vulnerabilityId) {
        log.info("REST request to enrich vulnerability ID {}", vulnerabilityId);
        EnrichedVulnerabilityResponse response = aiEnrichmentService.enrichVulnerability(vulnerabilityId);
        return ResponseEntity.ok(ApiResponse.success(MessageConstants.AI_ANALYSIS_SUCCESS, response));
    }

    @PostMapping("/enrich/retry/{vulnerabilityId}")
    @Operation(summary = "Force retry AI enrichment", description = "Forces a retry of AI enrichment for a specific vulnerability, bypassing cache. Useful for temporary AI service failures.")
    public ResponseEntity<ApiResponse<EnrichedVulnerabilityResponse>> retryVulnerability(
            @PathVariable Long vulnerabilityId) {
        log.info("REST request to force retry AI enrichment for vulnerability ID {}", vulnerabilityId);
        EnrichedVulnerabilityResponse response = aiEnrichmentService.retryVulnerability(vulnerabilityId);
        return ResponseEntity.ok(ApiResponse.success("AI Enrichment retried successfully.", response));
    }

    @PostMapping("/enrich/scan/{scanId}")
    @Operation(summary = "Enrich all vulnerabilities in a scan", description = "Triggers async AI enrichment for all vulnerabilities discovered in the specified scan. Returns immediately.")
    public ResponseEntity<ApiResponse<Map<String, Object>>> enrichScan(@PathVariable Long scanId) {
        log.info("REST request to async-enrich all vulnerabilities in scan ID {}", scanId);
        aiEnrichmentService.enrichScanAsync(scanId);
        return ResponseEntity.accepted().body(
                ApiResponse.success("AI enrichment started for scan " + scanId + ". Results will appear shortly.",
                        Map.of("scanId", scanId, "status", "ENRICHING"))
        );
    }

    @GetMapping("/enrich/vulnerability/{vulnerabilityId}")
    @Operation(summary = "Get enrichment result", description = "Retrieves the current AI enrichment data for a specific vulnerability.")
    public ResponseEntity<ApiResponse<EnrichedVulnerabilityResponse>> getEnrichment(
            @PathVariable Long vulnerabilityId) {
        EnrichedVulnerabilityResponse response = aiEnrichmentService.getEnrichment(vulnerabilityId);
        return ResponseEntity.ok(ApiResponse.success(MessageConstants.SUCCESS, response));
    }
}
