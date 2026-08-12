package com.sanjay.aisecurity.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Response DTO for a generated PDF report.
 *
 * @author Sanjay
 * @version 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ReportResponse {
    private Long id;
    private Long projectId;
    private String projectName;
    private Long scanHistoryId;
    private String reportName;
    private String reportType;
    private long reportSizeBytes;
    private Double securityScore;
    private Integer totalVulnerabilities;
    private LocalDateTime generatedAt;
    private String generatedBy;
    private String downloadUrl;
}
