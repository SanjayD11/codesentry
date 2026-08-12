package com.sanjay.aisecurity.service.analytics;

import com.sanjay.aisecurity.entity.Vulnerability;
import com.sanjay.aisecurity.enums.Severity;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Service responsible for computing the overall Security Risk Score of a project.
 *
 * <p>Uses a weighted factor model rather than simple counting, taking into account
 * severity, confidence, and specific vulnerability types (e.g., Secrets).</p>
 */
@Service
public class RiskScoreService {

    /**
     * Calculates the project security score out of 100.
     *
     * @param vulnerabilities The complete list of deduplicated vulnerabilities.
     * @return Final security score (0 - 100)
     */
    public double calculateScore(List<Vulnerability> vulnerabilities) {
        if (vulnerabilities == null || vulnerabilities.isEmpty()) {
            return 100.0;
        }

        double totalPenalty = 0.0;
        boolean hasCritical = false;

        for (Vulnerability v : vulnerabilities) {
            if (v.getSeverity() == Severity.CRITICAL) {
                hasCritical = true;
            }

            // 1. Base Severity Weight
            double basePenalty = getSeverityWeight(v.getSeverity());

            // 2. Confidence Modifier
            double confidence = v.getConfidenceScore();
            if (confidence <= 0) confidence = 0.1; // fallback
            
            double adjustedPenalty = basePenalty * confidence;

            // 3. Specialized Category Modifiers
            adjustedPenalty += getCategoryModifier(v);

            totalPenalty += adjustedPenalty;
        }

        double finalScore = 100.0 - totalPenalty;

        // 4. Clamping Rules
        // If there are ANY findings, max score is 99 (never perfect)
        finalScore = Math.min(finalScore, 99.0);
        
        // If there is a CRITICAL finding, max score is capped at 79 (C-grade max)
        if (hasCritical) {
            finalScore = Math.min(finalScore, 79.0);
        }

        // Floor at 0
        return Math.max(finalScore, 0.0);
    }

    private double getSeverityWeight(Severity severity) {
        if (severity == null) return 0;
        return switch (severity) {
            case CRITICAL -> 20.0;
            case HIGH -> 10.0;
            case MEDIUM -> 5.0;
            case LOW -> 1.0;
            case INFORMATIONAL -> 0.0;
        };
    }

    private double getCategoryModifier(Vulnerability v) {
        double extraPenalty = 0.0;
        String type = v.getVulnerabilityType() != null ? v.getVulnerabilityType().toLowerCase() : "";
        String cwe = v.getCweId() != null ? v.getCweId().toUpperCase() : "";

        // Secrets Exposure - high business risk
        if (cwe.equals("CWE-798") || cwe.equals("CWE-259") || type.contains("secret") || type.contains("password") || type.contains("token")) {
            extraPenalty += 5.0;
        }
        
        // Authentication Issues
        if (cwe.equals("CWE-287") || cwe.equals("CWE-288") || cwe.equals("CWE-306") || type.contains("auth")) {
            extraPenalty += 3.0;
        }
        
        // Configuration Issues (often widespread)
        if (cwe.equals("CWE-16") || v.getDetectionSource() != null && v.getDetectionSource().toLowerCase().contains("config")) {
            extraPenalty += 2.0;
        }

        return extraPenalty;
    }
}
