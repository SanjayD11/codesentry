package com.sanjay.aisecurity.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sanjay.aisecurity.ai.AiProvider;
import com.sanjay.aisecurity.ai.AiFeature;
import com.sanjay.aisecurity.ai.PromptBuilder;
import com.sanjay.aisecurity.ai.SimpleRateLimiter;
import com.sanjay.aisecurity.dto.request.ScanConfigurationDto;
import com.sanjay.aisecurity.dto.response.EnrichedVulnerabilityResponse;
import com.sanjay.aisecurity.entity.Vulnerability;
import com.sanjay.aisecurity.enums.AiStatus;
import com.sanjay.aisecurity.exception.ResourceNotFoundException;
import com.sanjay.aisecurity.repository.VulnerabilityRepository;
import com.sanjay.aisecurity.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * AI Enrichment Service Implementation.
 *
 * <p>Calls the AI provider with a structured security prompt, parses the JSON
 * response, and persists enrichment data back to the Vulnerability entity.</p>
 *
 * @author Sanjay
 * @version 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiEnrichmentServiceImpl implements AiEnrichmentService {

    private final AiProvider aiProvider;
    private final VulnerabilityRepository vulnerabilityRepository;
    private final ObjectMapper objectMapper;
    private final SimpleRateLimiter rateLimiter;
    private final java.util.concurrent.ConcurrentMap<Long, java.util.concurrent.locks.ReentrantLock> enrichScanLocks = new java.util.concurrent.ConcurrentHashMap<>();

    @org.springframework.beans.factory.annotation.Value("${ai.retry.max-attempts:3}")
    private int maxAttempts;

    @org.springframework.beans.factory.annotation.Value("${ai.retry.delay:5000}")
    private long retryDelay;

    @org.springframework.beans.factory.annotation.Value("${ai.retry.exponential-backoff:true}")
    private boolean exponentialBackoff;

    @Override
    @Transactional
    public EnrichedVulnerabilityResponse enrichVulnerability(Long vulnerabilityId) {
        String email = SecurityUtils.requireCurrentUserEmail();
        Vulnerability vuln = resolveOwnedVulnerability(vulnerabilityId, email);

        // Cache explanation check: if already enriched, return immediately without calling AI provider
        if (vuln.getAiGeneratedAt() != null) {
            log.info("Vulnerability ID {} is already enriched. Returning cached explanation.", vulnerabilityId);
            return toResponse(vuln);
        }

        // Enforce rate limiting
        rateLimiter.checkLimit(email);

        log.info("Enriching vulnerability ID {} with AI provider: {}", vulnerabilityId, aiProvider.getProviderName());

        String prompt = PromptBuilder.buildEnrichmentPrompt(vuln);
        String rawResponse = callAiWithRetry(prompt, vulnerabilityId);

        parseAndApply(vuln, rawResponse, ScanConfigurationDto.defaults());

        vuln.setProviderName(aiProvider.getProviderName());
        vuln.setAiGeneratedAt(LocalDateTime.now());
        vuln.setAiStatus(AiStatus.COMPLETED);
        vulnerabilityRepository.save(vuln);

        log.info("Vulnerability ID {} enriched successfully.", vulnerabilityId);
        return toResponse(vuln);
    }

    @Override
    @Transactional
    public EnrichedVulnerabilityResponse retryVulnerability(Long vulnerabilityId) {
        String email = SecurityUtils.requireCurrentUserEmail();
        Vulnerability vuln = resolveOwnedVulnerability(vulnerabilityId, email);

        // Enforce rate limiting
        rateLimiter.checkLimit(email);

        log.info("Force retrying AI enrichment for vulnerability ID {} with AI provider: {}", vulnerabilityId, aiProvider.getProviderName());

        String prompt = PromptBuilder.buildEnrichmentPrompt(vuln);
        String rawResponse = callAiWithRetry(prompt, vulnerabilityId);

        parseAndApply(vuln, rawResponse, ScanConfigurationDto.defaults());

        vuln.setProviderName(aiProvider.getProviderName());
        vuln.setAiGeneratedAt(LocalDateTime.now());
        vuln.setAiStatus(AiStatus.COMPLETED);
        vulnerabilityRepository.save(vuln);

        log.info("Vulnerability ID {} retry enriched successfully.", vulnerabilityId);
        return toResponse(vuln);
    }

    @Override
    @Async("taskExecutor")
    public void enrichScanAsync(Long scanId) {
        java.util.concurrent.locks.ReentrantLock lock = enrichScanLocks.computeIfAbsent(scanId, k -> new java.util.concurrent.locks.ReentrantLock());
        if (!lock.tryLock()) {
            log.warn("AI enrichment is already running for scan ID {}. Skipping duplicate request.", scanId);
            return;
        }
        try {
            log.info("Starting async AI enrichment for scan ID {}", scanId);
            List<Vulnerability> vulnerabilities = vulnerabilityRepository.findByScanHistoryId(scanId);

            // First pass: Set all non-completed findings to PENDING so UI shows queue state immediately
            for (Vulnerability vuln : vulnerabilities) {
                if (!AiStatus.COMPLETED.equals(vuln.getAiStatus())) {
                    vuln.setAiStatus(AiStatus.PENDING);
                    vulnerabilityRepository.saveAndFlush(vuln);
                }
            }

            for (Vulnerability vuln : vulnerabilities) {
                if (AiStatus.COMPLETED.equals(vuln.getAiStatus())) {
                    continue;
                }

                try {
                    vuln.setAiStatus(AiStatus.PROCESSING);
                    vulnerabilityRepository.saveAndFlush(vuln);

                    String prompt = PromptBuilder.buildEnrichmentPrompt(vuln);
                    String rawResponse = callAiWithRetry(prompt, vuln.getId());
                    // enrichScanAsync uses default config (all AI features ON) for backward compatibility
                    parseAndApply(vuln, rawResponse, ScanConfigurationDto.defaults());
                    vuln.setProviderName(aiProvider.getProviderName());
                    vuln.setAiGeneratedAt(LocalDateTime.now());
                    vuln.setAiStatus(AiStatus.COMPLETED);
                    vulnerabilityRepository.saveAndFlush(vuln);
                    log.info("Enriched vulnerability ID {}", vuln.getId());
                } catch (Exception e) {
                    log.error("Failed to enrich vulnerability ID {}: {}", vuln.getId(), e.getMessage());
                    vuln.setAiStatus(AiStatus.FAILED);
                    vulnerabilityRepository.saveAndFlush(vuln);
                }
            }
            log.info("Async enrichment completed for scan ID {}", scanId);
        } finally {
            lock.unlock();
            enrichScanLocks.remove(scanId);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public EnrichedVulnerabilityResponse getEnrichment(Long vulnerabilityId) {
        String email = SecurityUtils.requireCurrentUserEmail();
        Vulnerability vuln = resolveOwnedVulnerability(vulnerabilityId, email);
        return toResponse(vuln);
    }

    // =========================================================================
    // PRIVATE HELPERS
    // =========================================================================
    
    private String callAiWithRetry(String prompt, Long contextId) {
        // Retry logic is now strictly delegated to the AiProvider's bounded fallback chain
        // to prevent combinatorial explosion of HTTP requests.
        try {
            return aiProvider.complete(prompt, AiFeature.ENRICHMENT);
        } catch (Exception e) {
            log.error("[AI Enrichment] Failed for ID {}. Error: {}", contextId, e.getMessage());
            throw new RuntimeException("AI Provider failed: " + e.getMessage(), e);
        }
    }

    private Vulnerability resolveOwnedVulnerability(Long vulnId, String email) {
        Vulnerability vuln = vulnerabilityRepository.findById(vulnId)
                .orElseThrow(() -> new ResourceNotFoundException("Vulnerability not found."));
                
        if (!vuln.getScanHistory().getProject().getUser().getEmail().equals(email)) {
            throw new org.springframework.security.access.AccessDeniedException("Access Denied: You do not own this vulnerability.");
        }
        return vuln;
    }

    private void parseAndApply(Vulnerability vuln, String rawResponse, ScanConfigurationDto config) {
        try {
            // Strip markdown code fences if present
            String cleaned = rawResponse
                    .replaceAll("```json", "")
                    .replaceAll("```", "")
                    .trim();

            JsonNode node = objectMapper.readTree(cleaned);

            if (config.isEnableExplanation()) {
                vuln.setAiExplanation(getTextSafely(node, "explanation"));
            } else {
                log.debug("[AI Config] Skipping explanation for vuln {} (enableExplanation=OFF)", vuln.getId());
            }

            if (config.isEnableBusinessImpact()) {
                vuln.setBusinessImpact(getTextSafely(node, "businessImpact"));
            } else {
                log.debug("[AI Config] Skipping businessImpact for vuln {} (enableBusinessImpact=OFF)", vuln.getId());
            }

            if (config.isEnableSecureFix()) {
                vuln.setSecureCodeExample(getTextSafely(node, "secureCodeExample"));
            } else {
                log.debug("[AI Config] Skipping secureCodeExample for vuln {} (enableSecureFix=OFF)", vuln.getId());
            }

            if (config.isEnableRootCause()) {
                vuln.setAiRecommendation(getTextSafely(node, "rootCause"));
            } else {
                log.debug("[AI Config] Skipping rootCause for vuln {} (enableRootCause=OFF)", vuln.getId());
            }

        } catch (Exception e) {
            log.warn("Failed to parse AI JSON response for vulnerability ID {}. Storing raw text. Error: {}",
                    vuln.getId(), e.getMessage());
            if (config.isEnableExplanation()) {
                vuln.setAiExplanation(rawResponse);
            }
        }
    }

    private String getTextSafely(JsonNode node, String field) {
        JsonNode fieldNode = node.get(field);
        return (fieldNode != null && !fieldNode.isNull()) ? fieldNode.asText() : null;
    }



    private EnrichedVulnerabilityResponse toResponse(Vulnerability v) {
        return EnrichedVulnerabilityResponse.builder()
                .id(v.getId())
                .vulnerabilityType(v.getVulnerabilityType())
                .severity(v.getSeverity().name())
                .description(v.getDescription())
                .recommendation(v.getRecommendation())
                .fileName(v.getFileName())
                .lineNumber(v.getLineNumber())
                .codeSnippet(v.getCodeSnippet())
                .confidenceScore(v.getConfidenceScore())
                .owaspCategory(v.getOwaspCategory())
                .cweId(v.getCweId())
                .aiExplanation(v.getAiExplanation())
                .aiRecommendation(v.getAiRecommendation())
                .businessImpact(v.getBusinessImpact())
                .secureCodeExample(v.getSecureCodeExample())
                .providerName(v.getProviderName())
                .aiGeneratedAt(v.getAiGeneratedAt())
                .aiStatus(v.getAiStatus() != null ? v.getAiStatus().name() : null)
                .enriched(v.getAiExplanation() != null)
                .build();
    }
}
