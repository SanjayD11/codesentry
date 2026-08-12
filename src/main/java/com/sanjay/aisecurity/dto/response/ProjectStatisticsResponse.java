package com.sanjay.aisecurity.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Project Statistics Response DTO.
 *
 * @author Sanjay
 * @version 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ProjectStatisticsResponse {

    private long totalProjects;
    private long activeProjects;
    private long inactiveProjects;
    private long totalScans;
    private long totalUploadedFiles;
    private long totalVulnerabilities;
    private double averageProjectRiskScore;
    private String lastProjectCreatedName;
    private LocalDateTime lastProjectCreatedAt;
    private Map<String, Long> projectsByType;
}
