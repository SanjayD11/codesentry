package com.sanjay.aisecurity.analyzer;

import com.sanjay.aisecurity.enums.ProjectType;
import lombok.extern.slf4j.Slf4j;

/**
 * Utility class to detect programming language from file extensions.
 *
 * @author Sanjay
 * @version 1.0.0
 */
@Slf4j
public class LanguageDetector {

    public enum Language {
        // =====================================================================
        // Source Code Languages
        // =====================================================================
        JAVA,
        PYTHON,
        JAVASCRIPT,
        TYPESCRIPT,
        PHP,
        CSHARP,
        GO,
        KOTLIN,       // Phase 2
        C_CPP,
        SQL,
        RUBY,         // Phase 2
        SHELL,        // Phase 2 — .sh, .bash, .zsh
        POWERSHELL,   // Phase 2 — .ps1
        SCALA,        // Phase 2 stub
        RUST,         // Phase 2 stub
        SWIFT,        // Phase 2 stub

        // =====================================================================
        // Configuration / Infrastructure Languages
        // =====================================================================
        CONFIG_PROPERTIES,  // .properties, application.properties
        CONFIG_YAML,        // .yml, .yaml, docker-compose, k8s
        CONFIG_DOCKER,      // Dockerfile
        CONFIG_ENV,         // .env files
        CONFIG_XML,         // web.xml, pom.xml, Spring XML
        CONFIG_CICD,        // Jenkinsfile, GitHub Actions, GitLab CI

        UNKNOWN
    }

    /**
     * Detects language from file extension, with project type fallback.
     * This signature is preserved exactly for backward compatibility.
     *
     * @param extension   the file extension (e.g., ".java")
     * @param projectType the selected project type for contextual fallback
     * @return the detected Language enum
     */
    public static Language detect(String extension, ProjectType projectType) {
        return detect(extension, null, projectType);
    }

    /**
     * Detects language from file extension AND exact filename.
     * Filename-based matching takes priority for files like {@code Dockerfile}
     * or {@code Jenkinsfile} which have no extension.
     *
     * <p>Priority order:</p>
     * <ol>
     *   <li>Exact filename match (handles Dockerfile, .env, Jenkinsfile)</li>
     *   <li>File extension match</li>
     *   <li>Project type fallback</li>
     * </ol>
     *
     * @param extension   the file extension including dot (e.g., ".java"), or empty
     * @param fileName    the exact filename (e.g., "Dockerfile"), may be null
     * @param projectType the selected project type for contextual fallback
     * @return the detected Language enum
     */
    public static Language detect(String extension, String fileName, ProjectType projectType) {
        // Priority 1: Exact filename detection (for extensionless config files)
        if (fileName != null && !fileName.isBlank()) {
            Language fromFilename = detectFromFilename(fileName);
            if (fromFilename != Language.UNKNOWN) {
                return fromFilename;
            }
        }

        // Priority 2: Extension-based detection
        Language detectedFromExtension = Language.UNKNOWN;

        if (extension != null) {
            String ext = extension.toLowerCase().trim();
            if (ext.startsWith(".")) {
                ext = ext.substring(1);
            }

            switch (ext) {
                case "java":                   detectedFromExtension = Language.JAVA; break;
                case "kt": case "kts":         detectedFromExtension = Language.KOTLIN; break;
                case "py":                     detectedFromExtension = Language.PYTHON; break;
                case "js": case "jsx":         detectedFromExtension = Language.JAVASCRIPT; break;
                case "ts": case "tsx":         detectedFromExtension = Language.TYPESCRIPT; break;
                case "php":                    detectedFromExtension = Language.PHP; break;
                case "cs":                     detectedFromExtension = Language.CSHARP; break;
                case "go":                     detectedFromExtension = Language.GO; break;
                case "c": case "cpp": case "cxx":
                case "cc": case "h": case "hpp":  detectedFromExtension = Language.C_CPP; break;
                case "sql": case "ddl": case "dml": detectedFromExtension = Language.SQL; break;
                case "rb":                     detectedFromExtension = Language.RUBY; break;
                case "sh": case "bash": case "zsh": detectedFromExtension = Language.SHELL; break;
                case "ps1":                    detectedFromExtension = Language.POWERSHELL; break;
                case "scala":                  detectedFromExtension = Language.SCALA; break;
                case "rs":                     detectedFromExtension = Language.RUST; break;
                case "swift":                  detectedFromExtension = Language.SWIFT; break;
                case "properties":             detectedFromExtension = Language.CONFIG_PROPERTIES; break;
                case "yml": case "yaml":       detectedFromExtension = Language.CONFIG_YAML; break;
                case "env":                    detectedFromExtension = Language.CONFIG_ENV; break;
                case "xml":                    detectedFromExtension = Language.CONFIG_XML; break;
                default: break;
            }
        }

        // Priority 3: Project type fallback
        Language expectedFromProject = mapProjectTypeToLanguage(projectType);

        if (detectedFromExtension == Language.UNKNOWN) {
            if (expectedFromProject != Language.UNKNOWN) {
                log.info("Language unknown from extension '{}'. Falling back to project type '{}'.", extension, expectedFromProject);
                return expectedFromProject;
            }
            return Language.UNKNOWN;
        }

        // Mismatch warning (existing behaviour preserved)
        if (expectedFromProject != Language.UNKNOWN && detectedFromExtension != expectedFromProject) {
            log.debug("Language mismatch warning: File extension indicates {}, but project type is {}. Trusting file extension.",
                    detectedFromExtension, expectedFromProject);
        }

        return detectedFromExtension;
    }

    private static Language mapProjectTypeToLanguage(ProjectType type) {
        if (type == null) return Language.UNKNOWN;
        switch (type) {
            case JAVA:
            case SPRING_BOOT:
                return Language.JAVA;
            case JAVASCRIPT:
            case REACT:
            case ANGULAR:
            case NODE:
                return Language.JAVASCRIPT;
            case PYTHON:
                return Language.PYTHON;
            default:
                return Language.UNKNOWN;
        }
    }

    /**
     * Detects language from exact filename for files without meaningful extensions.
     * Handles: Dockerfile, .env, Jenkinsfile, GitHub Actions workflows.
     */
    private static Language detectFromFilename(String fileName) {
        switch (fileName) {
            case "Dockerfile":
            case "dockerfile":  return Language.CONFIG_DOCKER;
            case "Jenkinsfile": return Language.CONFIG_CICD;
            case ".env":        return Language.CONFIG_ENV;
            default:
                String lower = fileName.toLowerCase();
                if (lower.startsWith(".github") || lower.endsWith("-ci.yml") ||
                    lower.contains("gitlab-ci")) {
                    return Language.CONFIG_CICD;
                }
                return Language.UNKNOWN;
        }
    }
}
