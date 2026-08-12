package com.sanjay.aisecurity.analyzer;

import com.sanjay.aisecurity.enums.Severity;
import lombok.Builder;
import lombok.Data;

import java.util.regex.Pattern;

/**
 * Represents a single security rule for static analysis.
 */
@Data
@Builder
public class SecurityRule {
    private String id;
    private String name;
    private String description;
    private Severity severity;
    private double baseConfidence;
    private String owaspCategory;
    private String cweId;
    private String recommendation;
    private Pattern pattern;
    
    private String version;
    private String category; // e.g., "A1: Injection"
    private String detectionType; // e.g., "REGEX", "CONTEXT", "HYBRID"
    private String reference;
    private boolean supportsAutoFix;
    
    // Phase 2 new fields
    private String language;          // "JAVA", "PYTHON", "CONFIG", etc.
    private String subcategory;       // e.g., "Injection", "Cryptography", "Auth"
    private String remediationGuide;  // step-by-step text
    private boolean requiresContext;  // true → must pass contextual check to fire
    private int minLineContext;       // how many lines back to check for context
}
