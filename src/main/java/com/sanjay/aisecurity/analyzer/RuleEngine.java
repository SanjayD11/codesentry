package com.sanjay.aisecurity.analyzer;

import java.util.List;

/**
 * Common interface for all language-specific static analysis rule engines.
 *
 * @author Sanjay
 * @version 1.0.0
 */
public interface RuleEngine {
    
    /**
     * Scans the provided source code for language-specific vulnerabilities.
     *
     * @param code     the raw source code content
     * @param fileName the original name of the file being scanned
     * @return a list of detected vulnerabilities
     */
    List<VulnerabilityResult> scan(String code, String fileName);
}
