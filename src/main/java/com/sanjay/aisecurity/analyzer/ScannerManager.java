package com.sanjay.aisecurity.analyzer;

import com.sanjay.aisecurity.analyzer.LanguageDetector.Language;
import com.sanjay.aisecurity.analyzer.cpp.CppRuleEngine;
import com.sanjay.aisecurity.analyzer.csharp.CSharpRuleEngine;
import com.sanjay.aisecurity.analyzer.go.GoRuleEngine;
import com.sanjay.aisecurity.analyzer.java.JavaRuleEngine;
import com.sanjay.aisecurity.analyzer.js.JavaScriptRuleEngine;
import com.sanjay.aisecurity.analyzer.php.PhpRuleEngine;
import com.sanjay.aisecurity.analyzer.python.PythonRuleEngine;
import com.sanjay.aisecurity.analyzer.sql.SqlRuleEngine;
import com.sanjay.aisecurity.dto.request.ScanConfigurationDto;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Central Scanner Manager — Stage 6 of the scan pipeline: Scanner Selection.
 *
 * <p>Acts as the single routing authority for selecting the correct
 * {@link RuleEngine} for a given detected language and file category.
 * This class replaces the private {@code resolveEngine()} switch in
 * {@code ScanServiceImpl} as a reusable, injectable Spring component
 * without modifying that class until Phase 3.</p>
 *
 * <h3>Design</h3>
 * <ul>
 *   <li>Existing engines (Java, Python, JS, etc.) are registered in
 *       {@code @PostConstruct} from injected Spring beans — zero reflection,
 *       zero magic, fully transparent.</li>
 *   <li>New {@link ScannerEngine} implementations added in Phase 2
 *       are injected as a {@code List<ScannerEngine>} and registered
 *       automatically — adding a new language engine requires zero changes here.</li>
 *   <li>The {@code resolve()} method returns {@code Optional<RuleEngine>}
 *       which is backward-compatible with the existing scan loop.</li>
 * </ul>
 *
 * <h3>Integration point</h3>
 * <p>Consumed by {@link com.sanjay.aisecurity.analyzer.pipeline.ScannerSelector}
 * and (Phase 3+) directly by {@code ScanServiceImpl}.</p>
 *
 * @author Sanjay
 * @version 2.0.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ScannerManager {

    private final DynamicRuleEngine dynamicRuleEngine;

    private Map<Language, RuleEngine> engineMap;

    @PostConstruct
    void init() {
        engineMap = new EnumMap<>(Language.class);

        // Register DynamicRuleEngine for all relevant languages
        for (Language lang : Language.values()) {
            if (lang != Language.UNKNOWN) {
                engineMap.put(lang, dynamicRuleEngine.forLanguage(lang));
            }
        }

        log.info("[ScannerManager] Initialized with {} dynamic engine mappings.", engineMap.size());
    }

    // =========================================================================
    // PUBLIC API
    // =========================================================================

    /**
     * Resolves the appropriate engine for a given language.
     * Returns {@code Optional.empty()} if no engine is registered for that language.
     *
     * @param language the detected language
     * @return an Optional containing the matched engine, or empty
     */
    public Optional<RuleEngine> resolve(Language language) {
        return Optional.ofNullable(engineMap.get(language));
    }

    /**
     * Resolves the appropriate engine considering both language and file category.
     * Category is currently used for logging/auditing; routing is by language.
     *
     * @param language the detected language
     * @param category the file category (SOURCE, SQL, CONFIG)
     * @return an Optional containing the matched engine, or empty
     */
    public Optional<RuleEngine> resolve(Language language, FileCategory category) {
        Optional<RuleEngine> engine = resolve(language);
        if (engine.isPresent()) {
            log.debug("[ScannerManager] Resolved engine '{}' for language={}, category={}",
                    engine.get().getClass().getSimpleName(), language, category);
        } else {
            log.debug("[ScannerManager] No engine found for language={}, category={}", language, category);
        }
        return engine;
    }

    /**
     * Resolves a config-aware engine for a given language and category.
     *
     * <p>Unlike {@link #resolve(Language, FileCategory)}, this method creates a
     * fresh {@link DynamicRuleEngine} instance filtered by the provided
     * {@link ScanConfigurationDto}. Only rules enabled in the config will run.
     * Uses the cached no-config engine as a fallback when config is null.</p>
     *
     * @param language the detected language
     * @param category the file category
     * @param config   the scan configuration (may be null for default behaviour)
     * @return an Optional containing the config-filtered engine, or empty if no engine available
     */
    public Optional<RuleEngine> resolve(Language language, FileCategory category, ScanConfigurationDto config) {
        if (config == null) {
            return resolve(language, category);
        }
        if (language == Language.UNKNOWN || !engineMap.containsKey(language)) {
            log.debug("[ScannerManager] No engine for language={} category={}", language, category);
            return Optional.empty();
        }
        // Create a config-filtered engine on-demand (lightweight lambda from DynamicRuleEngine)
        RuleEngine configEngine = dynamicRuleEngine.forLanguage(language, config);
        log.debug("[ScannerManager] Resolved config-filtered engine for language={}, category={}", language, category);
        return Optional.of(configEngine);
    }

    /**
     * Returns true if an engine is available for the given language.
     */
    public boolean hasEngine(Language language) {
        return engineMap.containsKey(language);
    }

    /**
     * Returns the count of registered engine mappings.
     * Useful for health checks and startup validation.
     */
    public int getEngineCount() {
        return engineMap.size();
    }

    // =========================================================================
    // PRIVATE HELPERS
    // =========================================================================

    /**
     * Maps a raw file extension string to a Language enum value.
     * Used during auto-registration of Phase 2+ ScannerEngine implementations.
     */
    private Language mapExtensionToLanguage(String ext) {
        if (ext == null) return Language.UNKNOWN;
        switch (ext.toLowerCase().replace(".", "")) {
            case "kotlin": case "kt":  return Language.KOTLIN;
            case "ruby":   case "rb":  return Language.RUBY;
            case "shell":  case "sh":  return Language.SHELL;
            case "ps1":               return Language.POWERSHELL;
            case "scala":             return Language.SCALA;
            case "rust":   case "rs": return Language.RUST;
            case "swift":             return Language.SWIFT;
            case "properties":        return Language.CONFIG_PROPERTIES;
            case "yml": case "yaml":  return Language.CONFIG_YAML;
            case "docker":            return Language.CONFIG_DOCKER;
            case "env":               return Language.CONFIG_ENV;
            case "xml":               return Language.CONFIG_XML;
            case "cicd":              return Language.CONFIG_CICD;
            default:                  return Language.UNKNOWN;
        }
    }
}
