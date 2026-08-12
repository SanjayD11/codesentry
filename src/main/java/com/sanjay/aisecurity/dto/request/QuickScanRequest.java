package com.sanjay.aisecurity.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * Request payload for inline Quick Scans.
 */
@Data
public class QuickScanRequest {
    
    @NotBlank(message = "Source code is required for Quick Scan")
    private String sourceCode;
    
    private String language;
    private String filename;
    
    private ScanConfigurationDto configuration;
}
