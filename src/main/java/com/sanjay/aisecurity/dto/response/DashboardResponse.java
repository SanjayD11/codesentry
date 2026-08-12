package com.sanjay.aisecurity.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * Dashboard analytics response for the authenticated user.
 *
 * @author Sanjay
 * @version 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DashboardResponse {
    // Counts
    private long totalProjects;
    private long activeProjects;
    private long totalScans;
    private long totalVulnerabilities;
    private long totalFilesUploaded;

    // Vulnerability breakdown by severity
    private long criticalCount;
    private long highCount;
    private long mediumCount;
    private long lowCount;

    // Scores
    private double averageSecurityScore;
    private double overallRiskScore;

    // Recent activity
    private List<ScanSummaryResponse> recentScans;

    // Trend: security score per scan (for chart)
    private Map<String, Double> riskScoreTrend;
}
