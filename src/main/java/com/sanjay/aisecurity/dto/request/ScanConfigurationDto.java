package com.sanjay.aisecurity.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Complete scan configuration DTO.
 *
 * <p>Carries every user-configurable scan setting from the frontend drawer
 * through the entire backend pipeline. A single instance is passed from
 * controller → service → rule engine → file discovery → AI enrichment.
 * No individual boolean parameters are scattered across method signatures.</p>
 *
 * @author Sanjay
 * @version 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScanConfigurationDto {

    // =========================================================================
    // DETECTION SCOPE — control which rule categories run
    // =========================================================================

    /** Include OWASP classification metadata in findings. Does NOT suppress detection. */
    @Builder.Default private boolean owasp = true;

    /** Include CWE ID metadata in findings. Does NOT suppress detection. */
    @Builder.Default private boolean cwe = true;

    /** Enable Secrets / Hardcoded-Credentials scanner rules. */
    @Builder.Default private boolean secrets = true;

    /** Enable SQL Injection scanner rules. */
    @Builder.Default private boolean sqlInjection = true;

    /** Enable Cross-Site Scripting (XSS) scanner rules. */
    @Builder.Default private boolean xss = true;

    /** Enable Command Injection / OS Command scanner rules. */
    @Builder.Default private boolean commandInjection = true;

    /** Enable Path Traversal scanner rules. */
    @Builder.Default private boolean pathTraversal = true;

    /** Enable JWT / weak-signing-key scanner rules. */
    @Builder.Default private boolean jwtIssues = true;

    /** Enable Insecure Deserialization scanner rules. */
    @Builder.Default private boolean insecureDeserialization = true;

    /** Enable Weak Cryptography scanner rules. */
    @Builder.Default private boolean weakCryptography = true;

    /** Enable Directory Traversal scanner rules. */
    @Builder.Default private boolean directoryTraversal = true;

    // =========================================================================
    // AI CONFIGURATION — control what the LLM generates per finding
    // =========================================================================

    /** Generate AI explanation for each finding. */
    @Builder.Default private boolean enableExplanation = true;

    /** Generate root-cause analysis for each finding. */
    @Builder.Default private boolean enableRootCause = true;

    /** Generate business impact for each finding. */
    @Builder.Default private boolean enableBusinessImpact = true;

    /** Generate secure code fix / example for each finding. */
    @Builder.Default private boolean enableSecureFix = true;

    // =========================================================================
    // SCAN BEHAVIOUR — control resource limits and filtering
    // =========================================================================

    /**
     * Minimum confidence score (0–100) for a finding to be kept.
     * Findings with confidence*100 below this threshold are discarded.
     */
    @Min(value = 0, message = "confidenceThreshold must be between 0 and 100")
    @Max(value = 100, message = "confidenceThreshold must be between 0 and 100")
    @Builder.Default private int confidenceThreshold = 70;

    /** 
     * Maximum file size in MB. Files larger than this will be skipped. 
     * Supports decimals (e.g., 0.5 for 500KB).
     */
    @Positive(message = "maxFileSizeMB must be strictly greater than 0")
    @Builder.Default private double maxFileSizeMB = 10.0;

    /** Per-file and overall scan timeout in seconds. */
    @Positive(message = "timeoutSeconds must be greater than 0")
    @Builder.Default private int timeoutSeconds = 300;

    /**
     * Comma-separated list of directory names to skip during file discovery.
     * Merged with the built-in noise directory list.
     * Example: {@code "node_modules, .git, target, build"}
     */
    @Builder.Default private String ignoreDirectories = "node_modules, .git, target, build";

    /** Skip files that appear to be machine-generated (*.min.js, generated/, etc.). */
    @Builder.Default private boolean skipGeneratedFiles = true;

    // =========================================================================
    // FACTORY
    // =========================================================================

    /** Returns a default configuration matching the frontend DEFAULT_CONFIG. */
    public static ScanConfigurationDto defaults() {
        return ScanConfigurationDto.builder().build();
    }

    /** Returns the max file size in bytes for comparison against file.sizeBytes. */
    public long getMaxFileSizeBytes() {
        return (long) maxFileSizeMB * 1024 * 1024;
    }

    /**
     * Parses {@link #ignoreDirectories} into a trimmed, lower-cased set.
     * Safe to call repeatedly — result is not cached.
     */
    public java.util.Set<String> parsedIgnoreDirectories() {
        if (ignoreDirectories == null || ignoreDirectories.isBlank()) {
            return java.util.Set.of();
        }
        java.util.Set<String> result = new java.util.LinkedHashSet<>();
        for (String part : ignoreDirectories.split(",")) {
            String trimmed = part.strip();
            if (!trimmed.isEmpty()) result.add(trimmed.toLowerCase());
        }
        return result;
    }


    /** Produces a human-readable summary block for logging. */
    public String toLogString() {
        return "\n===== Effective Scan Configuration =====\n" +
               "  OWASP Mapping      : " + flag(owasp) + "\n" +
               "  CWE Mapping        : " + flag(cwe) + "\n" +
               "  Secrets            : " + flag(secrets) + "\n" +
               "  SQL Injection      : " + flag(sqlInjection) + "\n" +
               "  XSS                : " + flag(xss) + "\n" +
               "  Command Injection  : " + flag(commandInjection) + "\n" +
               "  Path Traversal     : " + flag(pathTraversal) + "\n" +
               "  JWT Issues         : " + flag(jwtIssues) + "\n" +
               "  Insecure Deser.    : " + flag(insecureDeserialization) + "\n" +
               "  Weak Cryptography  : " + flag(weakCryptography) + "\n" +
               "  Directory Traversal: " + flag(directoryTraversal) + "\n" +
               "  AI Explanation     : " + flag(enableExplanation) + "\n" +
               "  AI Root Cause      : " + flag(enableRootCause) + "\n" +
               "  AI Business Impact : " + flag(enableBusinessImpact) + "\n" +
               "  AI Secure Fix      : " + flag(enableSecureFix) + "\n" +
               "  Confidence Thr.    : " + confidenceThreshold + "%\n" +
               "  Max File Size      : " + maxFileSizeMB + " MB\n" +
               "  Timeout            : " + timeoutSeconds + "s\n" +
               "  Ignored Dirs       : " + (ignoreDirectories != null ? ignoreDirectories : "none") + "\n" +
               "  Skip Generated     : " + flag(skipGeneratedFiles) + "\n" +
               "========================================";
    }

    private static String flag(boolean v) { return v ? "ON" : "OFF"; }
}
