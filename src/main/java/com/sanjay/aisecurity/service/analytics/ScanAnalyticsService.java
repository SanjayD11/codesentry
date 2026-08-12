package com.sanjay.aisecurity.service.analytics;

import com.sanjay.aisecurity.entity.ScanHistory;
import com.sanjay.aisecurity.entity.Vulnerability;
import com.sanjay.aisecurity.enums.Severity;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Service responsible for computing on-demand analytics for a scan.
 *
 * <p>Separates analytics computation (OWASP, CWE, Language distributions)
 * from the core Scan orchestration.</p>
 */
@Service
public class ScanAnalyticsService {

    /**
     * Generates a comprehensive summary of a completed scan.
     */
    public ScanSummary generateSummary(ScanHistory scanHistory, 
                                       List<Vulnerability> vulnerabilities, 
                                       int discovered, int scanned, int skipped, int failed,
                                       double score) {
        
        // Severity Counts
        int crit = 0, high = 0, med = 0, low = 0, info = 0;
        for (Vulnerability v : vulnerabilities) {
            if (v.getSeverity() == null) continue;
            switch (v.getSeverity()) {
                case CRITICAL -> crit++;
                case HIGH -> high++;
                case MEDIUM -> med++;
                case LOW -> low++;
                case INFORMATIONAL -> info++;
            }
        }

        // Advanced Distributions
        Map<String, Long> owaspDist = vulnerabilities.stream()
                .filter(v -> v.getOwaspCategory() != null && !v.getOwaspCategory().isEmpty())
                .collect(Collectors.groupingBy(Vulnerability::getOwaspCategory, Collectors.counting()));

        Map<String, Long> cweDist = vulnerabilities.stream()
                .filter(v -> v.getCweId() != null && !v.getCweId().isEmpty())
                .collect(Collectors.groupingBy(Vulnerability::getCweId, Collectors.counting()));

        // We infer language from file extension for simplicity in analytics
        Map<String, Long> langDist = vulnerabilities.stream()
                .filter(v -> v.getFileName() != null)
                .collect(Collectors.groupingBy(v -> getExtension(v.getFileName()), Collectors.counting()));

        Map<String, Long> ruleDist = vulnerabilities.stream()
                .filter(v -> v.getRuleId() != null && !v.getRuleId().isEmpty())
                .collect(Collectors.groupingBy(Vulnerability::getRuleId, Collectors.counting()));

        Map<String, Long> sourceDist = vulnerabilities.stream()
                .filter(v -> v.getDetectionSource() != null && !v.getDetectionSource().isEmpty())
                .collect(Collectors.groupingBy(Vulnerability::getDetectionSource, Collectors.counting()));

        return ScanSummary.builder()
                .scanId(scanHistory.getId())
                .filesDiscovered(discovered)
                .filesScanned(scanned)
                .filesSkipped(skipped)
                .filesFailed(failed)
                .totalVulnerabilities(vulnerabilities.size())
                .criticalCount(crit)
                .highCount(high)
                .mediumCount(med)
                .lowCount(low)
                .informationalCount(info)
                .scanDurationSeconds(scanHistory.getDuration() / 1000.0)
                .finalScore(score)
                .owaspDistribution(owaspDist)
                .cweDistribution(cweDist)
                .languageDistribution(langDist)
                .ruleDistribution(ruleDist)
                .detectionSourceDistribution(sourceDist)
                .build();
    }

    private String getExtension(String fileName) {
        int idx = fileName.lastIndexOf('.');
        if (idx > 0 && idx < fileName.length() - 1) {
            return fileName.substring(idx + 1).toLowerCase();
        }
        return "unknown";
    }
}
