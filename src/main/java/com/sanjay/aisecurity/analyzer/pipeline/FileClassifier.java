package com.sanjay.aisecurity.analyzer.pipeline;

import com.sanjay.aisecurity.analyzer.FileCategory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * Stage 4 of the scan pipeline: File Classification.
 *
 * <p>Assigns a {@link FileCategory} to every {@link DiscoveredFile} based on
 * its extension, exact filename, and path context. This is a pure function with
 * no I/O and no Spring dependencies — it is deliberately kept simple and fast
 * so it can be unit-tested without a Spring context.</p>
 *
 * <p>Classification priority (first match wins):</p>
 * <ol>
 *   <li>Path contains a noise directory → {@code SKIP}</li>
 *   <li>Exact filename is a known config file (e.g., {@code Dockerfile}) → {@code CONFIG}</li>
 *   <li>Path matches a CI/CD pattern ({@code .github/workflows/}, {@code .gitlab-ci}) → {@code CONFIG}</li>
 *   <li>Extension is in the binary/generated skip list → {@code SKIP}</li>
 *   <li>Extension is in the SQL set → {@code SQL}</li>
 *   <li>Extension is in the config set → {@code CONFIG}</li>
 *   <li>Extension is in the source set → {@code SOURCE}</li>
 *   <li>Everything else → {@code SKIP}</li>
 * </ol>
 *
 * <p><strong>Integration point:</strong> Consumed by {@link ScannerSelector}
 * in Stage 5.</p>
 *
 * @author Sanjay
 * @version 2.0.0
 */
@Slf4j
@Component
public class FileClassifier {

    // Source code extensions (all lowercase, without dot)
    private static final Set<String> SOURCE_EXTENSIONS = Set.of(
            "java", "py", "js", "jsx", "ts", "tsx",
            "kt", "kts",                              // Kotlin
            "cs",                                     // C#
            "go",                                     // Go
            "cpp", "cxx", "cc", "c", "h", "hpp",     // C/C++
            "php",                                    // PHP
            "rb",                                     // Ruby
            "sh", "bash", "zsh",                      // Shell
            "ps1",                                    // PowerShell
            "scala",                                  // Scala (stub)
            "rs",                                     // Rust (stub)
            "swift"                                   // Swift (stub)
    );

    // SQL file extensions
    private static final Set<String> SQL_EXTENSIONS = Set.of("sql", "ddl", "dml");

    // Config / IaC file extensions
    private static final Set<String> CONFIG_EXTENSIONS = Set.of(
            "properties", "yml", "yaml",
            "xml",                                    // web.xml, pom.xml, Spring XML
            "tf",                                     // Terraform
            "conf", "cfg", "ini",                     // nginx.conf, etc.
            "toml",                                   // Cargo.toml, pyproject.toml
            "gradle",                                 // build.gradle
            "json"                                    // package.json, tsconfig.json, etc.
    );

    // Files identified by exact filename (no extension or extension-ambiguous)
    private static final Set<String> CONFIG_EXACT_FILENAMES = Set.of(
            "Dockerfile", "dockerfile",
            "Jenkinsfile",
            ".env",
            "docker-compose.yml", "docker-compose.yaml",
            "nginx.conf",
            ".htaccess",
            "Makefile",
            "Procfile"
    );

    // Binary, generated, or irrelevant extensions to skip
    private static final Set<String> SKIP_EXTENSIONS = Set.of(
            "class", "jar", "war", "ear",
            "zip", "tar", "gz", "bz2", "7z", "rar",
            "png", "jpg", "jpeg", "gif", "svg", "ico", "bmp", "webp", "tiff",
            "pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx",
            "mp4", "mp3", "avi", "mov", "wav", "ogg",
            "exe", "dll", "so", "dylib", "bin", "o", "a",
            "lock",                                   // package-lock.json, yarn.lock etc.
            "map",                                    // .js.map sourcemap files
            "min"                                     // minified artifacts (rare as sole ext)
    );

    // Directory segments that should never be scanned
    private static final Set<String> NOISE_DIRECTORIES = Set.of(
            "node_modules", "target", "build", "dist", "bin", "obj",
            ".gradle", ".git", ".idea", ".vscode", "__pycache__", ".mvn",
            "vendor", "coverage", ".next", ".nuxt", "out", ".terraform",
            "bower_components"
    );

    // =========================================================================
    // PUBLIC API
    // =========================================================================

    /**
     * Classifies a discovered file into its appropriate scan category.
     *
     * @param file the discovered file to classify
     * @return the file category, never null
     */
    public FileCategory classify(DiscoveredFile file) {
        String path = file.getPath();
        String fileName = file.getFileName();
        // Extension already has leading dot from DiscoveredFile; strip it for lookup
        String ext = file.getExtension().replace(".", "").toLowerCase();

        // Priority 1: noise directories — skip immediately
        if (isInNoiseDirectory(path)) {
            return FileCategory.SKIP;
        }

        // Priority 2: exact filename match (handles Dockerfile, .env, Jenkinsfile)
        if (CONFIG_EXACT_FILENAMES.contains(fileName)) {
            return FileCategory.CONFIG;
        }

        // Priority 3: CI/CD path patterns
        if (isCiCdPath(path)) {
            return FileCategory.CONFIG;
        }

        // Priority 4: binary / generated skip list
        if (SKIP_EXTENSIONS.contains(ext)) {
            return FileCategory.SKIP;
        }

        // Priority 5: files with no extension that aren't known config files
        if (ext.isEmpty()) {
            return FileCategory.SKIP;
        }

        // Priority 6–8: extension-based routing
        if (SQL_EXTENSIONS.contains(ext))    return FileCategory.SQL;
        if (CONFIG_EXTENSIONS.contains(ext)) return FileCategory.CONFIG;
        if (SOURCE_EXTENSIONS.contains(ext)) return FileCategory.SOURCE;

        log.trace("[Classifier] Unknown extension '{}' for file '{}' — SKIP", ext, path);
        return FileCategory.SKIP;
    }

    // =========================================================================
    // PRIVATE HELPERS
    // =========================================================================

    private boolean isInNoiseDirectory(String path) {
        String normalized = path.replace("\\", "/");
        for (String dir : NOISE_DIRECTORIES) {
            if (normalized.contains("/" + dir + "/") || normalized.startsWith(dir + "/")) {
                return true;
            }
        }
        return false;
    }

    private boolean isCiCdPath(String path) {
        String normalized = path.replace("\\", "/").toLowerCase();
        return normalized.contains(".github/workflows/")
                || normalized.contains(".gitlab-ci")
                || normalized.contains("jenkinsfile");
    }
}
