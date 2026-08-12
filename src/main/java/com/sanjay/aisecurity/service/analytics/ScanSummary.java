package com.sanjay.aisecurity.service.analytics;

import lombok.Builder;
import lombok.Data;

import java.util.Map;

/**
 * DTO holding aggregated analytics for a specific scan.
 */
@Data
@Builder
public class ScanSummary {
    private long scanId;
    private int filesDiscovered;
    private int filesScanned;
    private int filesSkipped;
    private int filesFailed;
    
    private int totalVulnerabilities;
    private int criticalCount;
    private int highCount;
    private int mediumCount;
    private int lowCount;
    private int informationalCount;
    
    private double scanDurationSeconds;
    private double finalScore;

    // Advanced breakdowns
    private Map<String, Long> owaspDistribution;
    private Map<String, Long> cweDistribution;
    private Map<String, Long> languageDistribution;
    private Map<String, Long> ruleDistribution;
    private Map<String, Long> detectionSourceDistribution;

    public String formatForLogs() {
        return String.format(
            "Scan Summary [ID: %d] | Score: %.1f | Discovered: %d | Scanned: %d | Skipped: %d | Failed: %d | Vulns: %d (C:%d, H:%d, M:%d, L:%d, I:%d) | Duration: %.1fs",
            scanId, finalScore, filesDiscovered, filesScanned, filesSkipped, filesFailed,
            totalVulnerabilities, criticalCount, highCount, mediumCount, lowCount, informationalCount,
            scanDurationSeconds
        );
    }
}
