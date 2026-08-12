package com.sanjay.aisecurity.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.sanjay.aisecurity.entity.Project;
import com.sanjay.aisecurity.enums.ProjectType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Project Response DTO.
 *
 * @author Sanjay
 * @version 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ProjectResponse {

    private Long id;
    private String projectName;
    private String description;
    private String version;
    private ProjectType projectType;
    private String status;
    private boolean active;
    private int totalFiles;
    private long totalScans;
    private long totalVulnerabilities;
    private double overallRiskScore;
    private LocalDateTime lastScanTime;
    private Long ownerId;
    private String ownerEmail;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /**
     * Maps a Project entity to ProjectResponse.
     *
     * @param project the domain entity
     * @return populated DTO
     */
    public static ProjectResponse from(Project project) {
        long scansCount = project.getScanHistories() != null ? project.getScanHistories().size() : 0L;
        long vulnerabilitiesCount = project.getScanHistories() != null ? 
                project.getScanHistories().stream()
                        .filter(sh -> sh.getVulnerabilities() != null)
                        .flatMap(sh -> sh.getVulnerabilities().stream())
                        .count() : 0L;

        return ProjectResponse.builder()
                .id(project.getId())
                .projectName(project.getName())
                .description(project.getDescription())
                .version(project.getVersion())
                .projectType(project.getProjectType())
                .status(project.getStatus())
                .active(project.isActive())
                .totalFiles(project.getTotalFiles())
                .totalScans(scansCount)
                .totalVulnerabilities(vulnerabilitiesCount)
                .overallRiskScore(Math.max(0.0, 100.0 - project.getSecurityScore()))
                .lastScanTime(project.getLastScan())
                .ownerId(project.getUser() != null ? project.getUser().getId() : null)
                .ownerEmail(project.getUser() != null ? project.getUser().getEmail() : null)
                .createdAt(project.getCreatedAt())
                .updatedAt(project.getUpdatedAt())
                .build();
    }
}
