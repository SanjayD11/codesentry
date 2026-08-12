package com.sanjay.aisecurity.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Admin Dashboard Statistics Response DTO.
 *
 * <p>Every field is calculated dynamically from the database at request time.
 * No values are stored separately or hardcoded.</p>
 *
 * @author Sanjay
 * @version 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminDashboardStatsResponse {

    // ── User Statistics ──────────────────────────────────────────────────────
    private long totalUsers;
    private long activeUsers;
    private long disabledUsers;
    private long usersRegisteredToday;

    // ── Project Statistics ───────────────────────────────────────────────────
    private long totalProjects;
    private long activeProjects;
    private long archivedProjects;
    private long projectsCreatedToday;

    // ── Scan Statistics ──────────────────────────────────────────────────────
    private long totalScans;
    private long completedScans;
    private long failedScans;
    private long scansToday;

    // ── Vulnerability Statistics ─────────────────────────────────────────────
    private long criticalVulnerabilities;
    private long highVulnerabilities;
    private long mediumVulnerabilities;
    private long lowVulnerabilities;

    // ── Aggregate Metrics ────────────────────────────────────────────────────
    private double averageScanScore;
    private double averageFindings;
    private double averageScanDuration;

    // ── Reports & AI ────────────────────────────────────────────────────────
    private long totalReports;
    private long aiReportsGenerated;
    private long aiRequests;
}
