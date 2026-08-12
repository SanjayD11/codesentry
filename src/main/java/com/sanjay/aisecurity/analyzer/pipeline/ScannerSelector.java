package com.sanjay.aisecurity.analyzer.pipeline;

import com.sanjay.aisecurity.analyzer.FileCategory;
import com.sanjay.aisecurity.analyzer.LanguageDetector;
import com.sanjay.aisecurity.analyzer.LanguageDetector.Language;
import com.sanjay.aisecurity.analyzer.RuleEngine;
import com.sanjay.aisecurity.analyzer.ScannerManager;
import com.sanjay.aisecurity.dto.request.ScanConfigurationDto;
import com.sanjay.aisecurity.enums.ProjectType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Stage 5+6 of the scan pipeline: Language Detection + Scanner Selection.
 *
 * <p>Combines {@link FileClassifier} and {@link ScannerManager} into a single,
 * convenient façade that answers the question: "Given this file, which scanner
 * should run on it?" The answer is returned as an {@code Optional<RuleEngine>}
 * which is empty when no scanner is applicable (SKIP category or unknown language).</p>
 *
 * <h3>Pipeline stages encapsulated here</h3>
 * <ol>
 *   <li>Classify the file: {@link FileClassifier#classify(DiscoveredFile)} → {@link FileCategory}</li>
 *   <li>Detect language: {@link LanguageDetector#detect(String, String, ProjectType)} → {@link Language}</li>
 *   <li>Select engine: {@link ScannerManager#resolve(Language, FileCategory)} → {@code Optional<RuleEngine>}</li>
 * </ol>
 *
 * <h3>Integration point</h3>
 * <p>Consumed by the new scan orchestration layer in Phase 3. During Phase 1–2,
 * it exists as a ready-to-use service that any component can inject.</p>
 *
 * @author Sanjay
 * @version 2.0.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ScannerSelector {

    private final FileClassifier fileClassifier;
    private final ScannerManager scannerManager;

    // =========================================================================
    // PUBLIC API
    // =========================================================================

    /**
     * Selects the appropriate rule engine for a discovered file (default — no config filtering).
     *
     * @param file        the discovered file (must have path, fileName, extension populated)
     * @param projectType the project type for language fallback; may be null
     * @return Optional containing the matched engine, or empty if the file should be skipped
     */
    public Optional<RuleEngine> select(DiscoveredFile file, ProjectType projectType) {
        return select(file, projectType, null);
    }

    /**
     * Selects a config-filtered rule engine for a discovered file.
     *
     * <p>When {@code config} is non-null, only rules enabled by the configuration
     * are loaded into the returned engine. This is the preferred overload for
     * production scans initiated via the UI scan drawer.</p>
     *
     * @param file        the discovered file
     * @param projectType the project type for language fallback; may be null
     * @param config      the scan configuration; null falls back to default behaviour
     * @return Optional containing the config-filtered engine, or empty if skipped
     */
    public Optional<RuleEngine> select(DiscoveredFile file, ProjectType projectType, ScanConfigurationDto config) {
        // Stage 4: Classify
        FileCategory category = fileClassifier.classify(file);
        if (category == FileCategory.SKIP) {
            log.trace("[Selector] SKIP: {}", file.getPath());
            return Optional.empty();
        }

        // Stage 5: Detect language (filename-aware overload)
        Language language = LanguageDetector.detect(file.getExtension(), file.getFileName(), projectType);
        if (language == Language.UNKNOWN) {
            log.debug("[Selector] Unknown language for file: {}", file.getPath());
            return Optional.empty();
        }

        // Stage 6: Select engine (config-aware)
        Optional<RuleEngine> engine = (config != null)
                ? scannerManager.resolve(language, category, config)
                : scannerManager.resolve(language, category);

        if (engine.isEmpty()) {
            log.debug("[Selector] No engine for language={} category={} file={}", language, category, file.getPath());
        }
        return engine;
    }

    /**
     * Classifies a file without selecting an engine.
     * Useful for building statistics without triggering scanning.
     */
    public FileCategory classify(DiscoveredFile file) {
        return fileClassifier.classify(file);
    }

    /**
     * Detects the language of a file without selecting an engine.
     */
    public Language detectLanguage(DiscoveredFile file, ProjectType projectType) {
        return LanguageDetector.detect(file.getExtension(), file.getFileName(), projectType);
    }

    /**
     * Returns true if this file would be scanned (i.e., a suitable engine exists).
     */
    public boolean isScannableFile(DiscoveredFile file, ProjectType projectType) {
        return select(file, projectType).isPresent();
    }
}
