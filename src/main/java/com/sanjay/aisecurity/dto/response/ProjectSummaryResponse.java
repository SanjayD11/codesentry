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
 * Project Summary Response DTO.
 *
 * @author Sanjay
 * @version 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ProjectSummaryResponse {

    private Long id;
    private String projectName;
    private String description;
    private ProjectType projectType;
    private String status;
    private boolean active;
    private int totalFiles;
    private double overallRiskScore;
    private LocalDateTime lastScanTime;
    private LocalDateTime createdAt;

    /**
     * Maps a Project entity to ProjectSummaryResponse.
     *
     * @param project the domain entity
     * @return populated DTO
     */
    public static ProjectSummaryResponse from(Project project) {
        return ProjectSummaryResponse.builder()
                .id(project.getId())
                .projectName(project.getName())
                .description(project.getDescription())
                .projectType(project.getProjectType())
                .status(project.getStatus())
                .active(project.isActive())
                .totalFiles(project.getTotalFiles())
                .overallRiskScore(Math.max(0.0, 100.0 - project.getSecurityScore()))
                .lastScanTime(project.getLastScan())
                .createdAt(project.getCreatedAt())
                .build();
    }
}
