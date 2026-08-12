package com.sanjay.aisecurity.analyzer;

import java.util.List;

/**
 * Rich scanner engine interface for all new language and config scanners.
 *
 * <p>Extends the existing {@link RuleEngine} interface for full backward
 * compatibility. New engines (Phase 2+) implement this interface. Existing
 * engines (Java, Python, etc.) continue implementing {@link RuleEngine} only
 * and are adapted inside {@link ScannerManager} without any modification.</p>
 *
 * <p>The additional methods provide self-describing metadata that allows the
 * {@link ScannerManager} to auto-wire engines without any manual switch
 * statements — simply adding a new {@code @Component} that implements this
 * interface is enough for it to be discovered and registered automatically.</p>
 *
 * @author Sanjay
 * @version 2.0.0
 * @see RuleEngine
 * @see ScannerManager
 */
public interface ScannerEngine extends RuleEngine {

    /**
     * Short unique identifier for this engine.
     * Used in logging and evidence records.
     * Example: {@code "JAVA"}, {@code "DOCKER"}, {@code "SQL"}
     */
    String getEngineId();

    /**
     * Human-readable name for this engine.
     * Example: {@code "Java Source Scanner"}, {@code "Dockerfile Scanner"}
     */
    String getEngineName();

    /**
     * Top-level category this engine handles.
     * Used by {@link ScannerManager} to route files to the correct tier.
     */
    FileCategory getCategory();

    /**
     * File extensions (without dot) supported by this engine.
     * Example: {@code ["java", "jav"]}
     */
    List<String> getSupportedExtensions();

    /**
     * Exact file names (without extension) supported by this engine.
     * Used for files like {@code Dockerfile} or {@code Jenkinsfile} that
     * have no extension.
     * Returns an empty list if this engine does not use filename matching.
     */
    List<String> getSupportedFileNames();
}
