package com.sanjay.aisecurity.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Scan summary response returned when querying a specific scan.
 *
 * @author Sanjay
 * @version 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ScanSummaryResponse {
    private Long scanId;
    private Long projectId;
    private String status;
    private String scanStart;
    private String scanEnd;
    private double durationSeconds;
    private int progressPercentage;
    private int scannedFiles;
    private int totalFiles;
    private int totalVulnerabilities;
    private double securityScore;
    private String aiSummary;
    private String language;
    private String scanType;
    private String snippetFilename;
    private String snippetLanguage;
    private Integer snippetLines;
    
    // Metadata for Traceability
    private String ruleEngineVersion;
    private String rulePackVersion;
    private String aiModel;
    private String configurationJson;
    
    // Optional, included when requested
    private List<VulnerabilityResponse> vulnerabilities;
}
