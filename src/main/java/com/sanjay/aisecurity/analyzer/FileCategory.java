package com.sanjay.aisecurity.analyzer;

/**
 * Classifies a discovered file into one of four top-level categories.
 *
 * <p>Used by {@link com.sanjay.aisecurity.analyzer.pipeline.FileClassifier} to
 * route each file to the appropriate scanner tier in the {@link ScannerManager}.</p>
 *
 * <ul>
 *   <li>{@code SOURCE}  – Human-written source code (Java, Python, JS, etc.)</li>
 *   <li>{@code SQL}     – SQL scripts and query files</li>
 *   <li>{@code CONFIG}  – Configuration files (YAML, Properties, Dockerfile, .env, etc.)</li>
 *   <li>{@code SKIP}    – Binary files, generated artifacts, or noise directories</li>
 * </ul>
 *
 * @author Sanjay
 * @version 2.0.0
 */
public enum FileCategory {

    /** Human-written source code files. */
    SOURCE,

    /** SQL scripts, stored procedures, and query files. */
    SQL,

    /** Configuration and infrastructure-as-code files. */
    CONFIG,

    /** Files that should not be scanned (binary, generated, oversized). */
    SKIP
}
