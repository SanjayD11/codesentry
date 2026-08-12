package com.sanjay.aisecurity.analyzer.pipeline;

import com.sanjay.aisecurity.dto.request.ScanConfigurationDto;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FilenameUtils;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Stage 3 of the scan pipeline: File Discovery.
 *
 * <p>Responsible for extracting all candidate files from a ZIP archive or
 * reading a single uploaded file, entirely in memory. No files are written
 * to disk during this process.</p>
 *
 * <p>Applies early-stage safety guards:</p>
 * <ul>
 *   <li>Maximum 1,000 files per ZIP</li>
 *   <li>Maximum 2 MB per individual file</li>
 *   <li>Automatic skipping of noise directories (node_modules, .git, target, etc.)</li>
 * </ul>
 *
 * <p><strong>Integration point:</strong> Called by {@code ScanServiceImpl}
 * (Phase 3+ migration) and directly usable by any new scan orchestration
 * component. Does NOT replace existing ZIP logic in {@code ScanServiceImpl}
 * — the existing logic remains functional. This service is additive.</p>
 *
 * @author Sanjay
 * @version 2.0.0
 */
@Slf4j
@Service
public class FileDiscoveryService {

    /** Maximum individual file size in bytes (2 MB). */
    public static final long MAX_FILE_SIZE_BYTES = 2 * 1024 * 1024;

    /** Maximum number of files to process from a single ZIP. */
    public static final int MAX_FILES_PER_ZIP = 1000;

    /** Directories whose contents should never be scanned (built-in, always applied). */
    private static final Set<String> NOISE_DIRECTORIES = Set.of(
            "node_modules", "target", "build", "dist", "bin", "obj",
            ".gradle", ".git", ".idea", ".vscode", "__pycache__", ".mvn",
            "vendor", "coverage", ".next", ".nuxt", "out", ".DS_Store",
            "bower_components", ".terraform"
    );

    /**
     * File path patterns that indicate machine-generated content.
     * Applied only when {@code skipGeneratedFiles = true} in the scan config.
     */
    private static final Set<String> GENERATED_FILE_PATTERNS = Set.of(
            ".min.js", ".min.css",           // Minified assets
            "generated/",                     // Generic generated folder
            "generated-sources/",             // Maven annotation processor output
            "swagger-generated/",             // Swagger codegen
            "protobuf/",                      // Protocol Buffers generated
            "build/generated/",               // Gradle generated
            "-gen.go", "_generated.go",       // Go generated files
            ".pb.go", ".pb.swift",            // Protobuf generated
            "__generated__/",                 // GraphQL code-gen
            "grpc_stubs/"
    );

    // =========================================================================
    // PUBLIC API
    // =========================================================================

    /**
     * Discovers all scannable files within a ZIP archive — default (no config override).
     *
     * @param zipStream the ZIP input stream (caller responsible for closing)
     * @return list of discovered files
     * @throws IOException if the ZIP stream cannot be read
     */
    public List<DiscoveredFile> discoverFromZip(InputStream zipStream) throws IOException {
        return discoverFromZip(zipStream, ScanConfigurationDto.defaults());
    }

    /**
     * Discovers all scannable files within a ZIP archive, respecting the scan configuration.
     *
     * <p>Applies:
     * <ul>
     *   <li>{@code config.maxFileSizeMB} — per-file size limit (overrides hardcoded 2 MB)</li>
     *   <li>{@code config.ignoreDirectories} — merged with built-in NOISE_DIRECTORIES</li>
     *   <li>{@code config.skipGeneratedFiles} — skips files matching GENERATED_FILE_PATTERNS</li>
     * </ul></p>
     *
     * @param zipStream the ZIP input stream (caller responsible for closing)
     * @param config    the active scan configuration
     * @return list of discovered files
     * @throws IOException if the ZIP stream cannot be read
     */
    public List<DiscoveredFile> discoverFromZip(InputStream zipStream, ScanConfigurationDto config) throws IOException {
        List<DiscoveredFile> discovered = new ArrayList<>();
        int processedCount = 0;
        long effectiveMaxBytes = config.getMaxFileSizeBytes();
        Set<String> ignoreDirs = mergeIgnoreDirectories(config);

        try (ZipInputStream zis = new ZipInputStream(zipStream)) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {

                if (processedCount >= MAX_FILES_PER_ZIP) {
                    log.warn("[Discovery] ZIP exceeds {} file limit — stopping early.", MAX_FILES_PER_ZIP);
                    break;
                }

                try {
                    if (entry.isDirectory()) {
                        continue;
                    }

                    String entryPath = normalizePath(entry.getName());

                    if (isIgnoredDirectory(entryPath, ignoreDirs)) {
                        log.trace("[Discovery] Skipping ignored path: {}", entryPath);
                        continue;
                    }

                    if (config.isSkipGeneratedFiles() && isGeneratedFile(entryPath)) {
                        log.debug("[Discovery] Skipping generated file: {}", entryPath);
                        continue;
                    }

                    // Guard: skip files whose declared size already exceeds limit
                    if (entry.getSize() > 0 && entry.getSize() > effectiveMaxBytes) {
                        log.info("[Discovery] Skipping oversized entry: {} ({} bytes > {} MB limit)",
                                entryPath, entry.getSize(), config.getMaxFileSizeMB());
                        continue;
                    }

                    byte[] content = readSafely(zis, entryPath, effectiveMaxBytes);
                    if (content == null) {
                        log.info("[Discovery] File '{}' exceeded {} MB size limit during streaming — skipped.",
                                entryPath, config.getMaxFileSizeMB());
                        continue;
                    }

                    discovered.add(buildDiscoveredFile(entryPath, content));
                    processedCount++;

                } catch (Exception e) {
                    log.warn("[Discovery] Failed to read entry '{}': {}", entry.getName(), e.getMessage());
                } finally {
                    try { zis.closeEntry(); } catch (Exception ignored) {}
                }
            }
        }

        log.info("[Discovery] ZIP scan complete — {} files discovered.", discovered.size());
        return discovered;
    }

    /**
     * Wraps a single uploaded non-ZIP file as a {@link DiscoveredFile} — default (no config override).
     *
     * @param inputStream the file input stream
     * @param fileName    the original file name
     * @return the discovered file, or {@code null} if the file exceeds the size limit
     * @throws IOException if the stream cannot be read
     */
    public DiscoveredFile discoverSingleFile(InputStream inputStream, String fileName) throws IOException {
        return discoverSingleFile(inputStream, fileName, ScanConfigurationDto.defaults());
    }

    /**
     * Wraps a single uploaded non-ZIP file as a {@link DiscoveredFile}, respecting the scan configuration.
     *
     * @param inputStream the file input stream
     * @param fileName    the original file name
     * @param config      the active scan configuration
     * @return the discovered file, or {@code null} if the file is skipped
     * @throws IOException if the stream cannot be read
     */
    public DiscoveredFile discoverSingleFile(InputStream inputStream, String fileName,
                                             ScanConfigurationDto config) throws IOException {
        Set<String> ignoreDirs = mergeIgnoreDirectories(config);

        if (isIgnoredDirectory(normalizePath(fileName), ignoreDirs)) {
            log.info("[Discovery] File '{}' is in an ignored directory — skipped.", fileName);
            return null;
        }
        if (config.isSkipGeneratedFiles() && isGeneratedFile(fileName)) {
            log.info("[Discovery] File '{}' matches generated-file pattern — skipped.", fileName);
            return null;
        }

        byte[] content = inputStream.readAllBytes();

        if (content.length > config.getMaxFileSizeBytes()) {
            log.info("[Discovery] File '{}' exceeds {} MB size limit ({} bytes) — skipped.",
                    fileName, config.getMaxFileSizeMB(), content.length);
            return null;
        }

        return buildDiscoveredFile(fileName, content);
    }

    // =========================================================================
    // PRIVATE HELPERS
    // =========================================================================

    private byte[] readSafely(ZipInputStream zis, String entryPath, long maxBytes) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        int len;
        long totalRead = 0;

        while ((len = zis.read(buffer)) > 0) {
            totalRead += len;
            if (totalRead > maxBytes) {
                return null; // caller logs the skip
            }
            baos.write(buffer, 0, len);
        }

        return baos.toByteArray();
    }

    /** Merges built-in noise directories with user-configured ignore directories. */
    private Set<String> mergeIgnoreDirectories(ScanConfigurationDto config) {
        Set<String> merged = new java.util.LinkedHashSet<>(NOISE_DIRECTORIES);
        merged.addAll(config.parsedIgnoreDirectories());
        return merged;
    }

    /** Returns true if the path belongs to an ignored directory. */
    private boolean isIgnoredDirectory(String path, Set<String> ignoreDirs) {
        for (String dir : ignoreDirs) {
            if (path.contains("/" + dir + "/") || path.startsWith(dir + "/")) {
                return true;
            }
        }
        return false;
    }

    /** Returns true if the path matches any known generated-file pattern. */
    private boolean isGeneratedFile(String path) {
        String lower = path.toLowerCase();
        for (String pattern : GENERATED_FILE_PATTERNS) {
            if (lower.contains(pattern) || lower.endsWith(pattern.replace("/", ""))) {
                return true;
            }
        }
        return false;
    }

    private DiscoveredFile buildDiscoveredFile(String path, byte[] content) {
        String fileName = extractFileName(path);
        String rawExt = FilenameUtils.getExtension(fileName);
        String extension = rawExt.isEmpty() ? "" : "." + rawExt.toLowerCase();

        return DiscoveredFile.builder()
                .path(path)
                .fileName(fileName)
                .extension(extension)
                .content(content)
                .sizeBytes(content.length)
                .build();
    }

    private String normalizePath(String raw) {
        return raw == null ? "" : raw.replace("\\", "/");
    }

    private String extractFileName(String path) {
        int lastSlash = path.lastIndexOf('/');
        return lastSlash >= 0 ? path.substring(lastSlash + 1) : path;
    }

    private boolean isNoiseDirectory(String path) {
        for (String dir : NOISE_DIRECTORIES) {
            if (path.contains("/" + dir + "/") || path.startsWith(dir + "/")) {
                return true;
            }
        }
        return false;
    }
}
