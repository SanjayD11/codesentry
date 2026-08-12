package com.sanjay.aisecurity.service;

import com.sanjay.aisecurity.dto.response.DashboardResponse;
import com.sanjay.aisecurity.dto.response.ScanSummaryResponse;
import com.sanjay.aisecurity.entity.ScanHistory;
import com.sanjay.aisecurity.enums.Severity;
import com.sanjay.aisecurity.repository.ProjectRepository;
import com.sanjay.aisecurity.repository.ScanHistoryRepository;
import com.sanjay.aisecurity.repository.UploadedFileRepository;
import com.sanjay.aisecurity.repository.VulnerabilityRepository;
import com.sanjay.aisecurity.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Dashboard Service Implementation.
 *
 * <p>Aggregates user-scoped analytics from multiple repositories in a single response.</p>
 *
 * @author Sanjay
 * @version 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DashboardServiceImpl implements DashboardService {

    private final ProjectRepository projectRepository;
    private final ScanHistoryRepository scanHistoryRepository;
    private final VulnerabilityRepository vulnerabilityRepository;
    private final UploadedFileRepository uploadedFileRepository;

    @Override
    @Transactional(readOnly = true)
    public DashboardResponse getMyDashboard() {
        String email = SecurityUtils.requireCurrentUserEmail();
        log.debug("Building dashboard for user: {}", email);

        long totalProjects = projectRepository.countByUserEmailAndActiveTrueAndNameNot(email, "[Direct Scans]");
        long totalScans = scanHistoryRepository.countByProjectUserEmail(email);
        long totalVulns = vulnerabilityRepository.countByScanHistoryProjectUserEmail(email);

        long critical = vulnerabilityRepository.countByScanHistoryProjectUserEmailAndSeverity(email, Severity.CRITICAL);
        long high = vulnerabilityRepository.countByScanHistoryProjectUserEmailAndSeverity(email, Severity.HIGH);
        long medium = vulnerabilityRepository.countByScanHistoryProjectUserEmailAndSeverity(email, Severity.MEDIUM);
        long low = vulnerabilityRepository.countByScanHistoryProjectUserEmailAndSeverity(email, Severity.LOW);

        Long totalFilesLong = uploadedFileRepository.sumFileSizeByProjectUserEmailAndIsDeletedFalse(email);
        long totalFiles = uploadedFileRepository.countByProjectUserEmailAndIsDeletedFalse(email);

        Double avgScore = projectRepository.averageSecurityScoreByUserEmailAndActiveTrueAndNameNot(email);
        double averageSecurityScore = avgScore != null ? Math.round(avgScore * 10.0) / 10.0 : 100.0;

        // Risk score: inverse of average security score  
        double overallRisk = Math.max(0, 100.0 - averageSecurityScore);

        // Recent 5 scans
        List<ScanHistory> recentScans = scanHistoryRepository.findTop5ByProjectUserEmailOrderByScanStartDesc(email);
        List<ScanSummaryResponse> recentScanResponses = recentScans.stream()
                .map(this::toScanSummary)
                .collect(Collectors.toList());

        // Risk score trend (scan label → security score)
        Map<String, Double> riskTrend = new LinkedHashMap<>();
        recentScans.forEach(scan -> {
            String label = "Scan #" + scan.getId();
            riskTrend.put(label, scan.getSecurityScore());
        });

        return DashboardResponse.builder()
                .totalProjects(totalProjects)
                .activeProjects(totalProjects)
                .totalScans(totalScans)
                .totalVulnerabilities(totalVulns)
                .totalFilesUploaded(totalFiles)
                .criticalCount(critical)
                .highCount(high)
                .mediumCount(medium)
                .lowCount(low)
                .averageSecurityScore(averageSecurityScore)
                .overallRiskScore(overallRisk)
                .recentScans(recentScanResponses)
                .riskScoreTrend(riskTrend)
                .build();
    }

    private ScanSummaryResponse toScanSummary(ScanHistory scan) {
        return ScanSummaryResponse.builder()
                .scanId(scan.getId())
                .projectId(scan.getProject().getId())
                .status(scan.getStatus().name())
                .scanStart(scan.getScanStart() != null ? scan.getScanStart().toString() : null)
                .scanEnd(scan.getScanEnd() != null ? scan.getScanEnd().toString() : null)
                .durationSeconds(scan.getDuration() / 1000.0)
                .scannedFiles(scan.getScannedFiles())
                .totalFiles(scan.getTotalFiles())
                .totalVulnerabilities(scan.getTotalVulnerabilities())
                .securityScore(scan.getSecurityScore())
                .configurationJson(scan.getConfigurationJson())
                .build();
    }
}
