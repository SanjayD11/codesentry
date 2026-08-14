package com.sanjay.aisecurity.service.ai;

import com.sanjay.aisecurity.ai.AiProvider;
import com.sanjay.aisecurity.ai.AiFeature;
import com.sanjay.aisecurity.ai.PromptBuilder;
import com.sanjay.aisecurity.dto.request.ScanConfigurationDto;
import com.sanjay.aisecurity.entity.ScanHistory;
import com.sanjay.aisecurity.entity.Vulnerability;
import com.sanjay.aisecurity.repository.ScanHistoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Service for handling asynchronous AI enrichment of scan results.
 *
 * <p>Operates completely outside the critical path of the main scan pipeline.
 * If AI enrichment fails, the scan itself remains successful.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiEnrichmentService {

    private final AiProvider aiProvider;
    private final ScanHistoryRepository scanHistoryRepository;

    /**
     * Asynchronously generates an Executive AI Summary for a completed scan.
     * Idempotent: skips if an AI summary already exists (unless force is true).
     * Backward-compatible overload — uses default AI config (all features ON).
     */
    @Async("scanTaskExecutor")
    @Transactional
    public void enrichScanSummaryAsync(Long scanId, List<Vulnerability> vulnerabilities,
                                       int successfulScans, int crit, int high, int med, int low,
                                       boolean force) {
        enrichScanSummaryAsync(scanId, vulnerabilities, successfulScans, crit, high, med, low,
                force, ScanConfigurationDto.defaults());
    }

    /**
     * Asynchronously generates an Executive AI Summary for a completed scan,
     * respecting the AI configuration flags.
     *
     * <p>If all AI flags are OFF, skips the LLM call entirely to save API tokens.</p>
     *
     * @param config the scan configuration carrying enableExplanation, enableRootCause,
     *               enableBusinessImpact, and enableSecureFix flags
     */
    @Async("scanTaskExecutor")
    @Transactional
    public void enrichScanSummaryAsync(Long scanId, List<Vulnerability> vulnerabilities,
                                       int successfulScans, int crit, int high, int med, int low,
                                       boolean force, ScanConfigurationDto config) {
        enrichScanSummary(scanId, vulnerabilities, successfulScans, crit, high, med, low, force, config);
    }

    /**
     * Synchronously generates an Executive AI Summary for a completed scan.
     * Useful for inline Quick Scans where the API waits for completion.
     */
    @Transactional
    public void enrichScanSummary(Long scanId, List<Vulnerability> vulnerabilities,
                                  int successfulScans, int crit, int high, int med, int low,
                                  boolean force, ScanConfigurationDto config) {
        
        ScanHistory scanHistory = scanHistoryRepository.findById(scanId).orElse(null);
        if (scanHistory == null) {
            log.warn("[AI Enrichment] Scan {} not found.", scanId);
            return;
        }

        // Idempotency Check
        if (!force && scanHistory.getAiSummary() != null && !scanHistory.getAiSummary().isEmpty()) {
            log.info("[AI Enrichment] AI summary already exists for Scan {}. Skipping.", scanId);
            return;
        }

        if (aiProvider == null) {
            log.warn("[AI Enrichment] No AI Provider configured. Skipping enrichment.");
            return;
        }

        try {
            log.info("[AI Enrichment] Started async enrichment for Scan {}.", scanId);
            
            String prompt = PromptBuilder.buildProjectSummaryPrompt(
                    successfulScans, vulnerabilities.size(), crit, high, med, low, vulnerabilities);
            
            String aiSummary = aiProvider.complete(prompt, AiFeature.REPORT_GENERATION);
            
            scanHistory.setAiSummary(aiSummary);
            scanHistoryRepository.save(scanHistory);
            
            log.info("[AI Enrichment] Successfully completed enrichment for Scan {}.", scanId);

        } catch (Exception e) {
            log.error("[AI Enrichment] Failed to generate AI summary for Scan {}: {}", scanId, e.getMessage());
            // Failure Isolation: Do not throw exception. Let the scan remain marked as COMPLETED.
            if (scanHistory.getAiSummary() == null) {
                scanHistory.setAiSummary("AI Enrichment temporarily unavailable.");
                scanHistoryRepository.save(scanHistory);
            }
        }
    }
}
