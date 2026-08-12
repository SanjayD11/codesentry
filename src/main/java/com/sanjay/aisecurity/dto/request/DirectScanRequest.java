package com.sanjay.aisecurity.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for a direct inline source-code scan (no project required).
 *
 * @author Sanjay
 * @version 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DirectScanRequest {

    /** The raw source code to analyse. */
    @NotBlank(message = "Source code must not be empty")
    private String code;

    /**
     * File extension hint for language detection (e.g. "java", "py", "js").
     * Defaults to "txt" / UNKNOWN when omitted.
     */
    private String language;

    /** Optional human-readable filename shown in the vulnerability report. */
    private String filename;
}
