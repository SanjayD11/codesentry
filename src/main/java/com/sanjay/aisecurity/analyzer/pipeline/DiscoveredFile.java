package com.sanjay.aisecurity.analyzer.pipeline;

import lombok.Builder;
import lombok.Value;

/**
 * Immutable value object representing a single file discovered during the
 * file discovery stage of the scan pipeline.
 *
 * <p>Produced by {@link FileDiscoveryService} and consumed by
 * {@link FileClassifier} and {@link ScannerSelector} in subsequent stages.
 * Carries the raw file content in memory so the rest of the pipeline
 * never needs to re-read from disk or ZIP streams.</p>
 *
 * <p><strong>Integration point:</strong> Stage 3 (File Discovery) →
 * Stage 4 (File Classification).</p>
 *
 * @author Sanjay
 * @version 2.0.0
 */
@Value
@Builder
public class DiscoveredFile {

    /**
     * Full relative path as it appeared inside the ZIP or on disk.
     * Examples: {@code "src/main/java/App.java"}, {@code "Dockerfile"}
     */
    String path;

    /**
     * Just the filename portion of the path.
     * Example: {@code "App.java"}, {@code "Dockerfile"}
     */
    String fileName;

    /**
     * File extension including the leading dot, or empty string if none.
     * Example: {@code ".java"}, {@code ".yml"}, {@code ""}
     */
    String extension;

    /**
     * Raw file content bytes read from the ZIP or disk.
     * Never null; empty array for zero-byte files.
     */
    byte[] content;

    /** File size in bytes. */
    long sizeBytes;

    /**
     * Convenience method to return content as a UTF-8 string.
     * This is what rule engines receive as their {@code code} parameter.
     */
    public String getContentAsString() {
        if (content == null || content.length == 0) return "";
        return new String(content, java.nio.charset.StandardCharsets.UTF_8);
    }
}
