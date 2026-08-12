package com.sanjay.aisecurity.analyzer;

import java.util.List;

/**
 * Abstract base for all new source-code scanner engines introduced in Phase 2+.
 *
 * <p>Extends {@link BaseRuleEngine} to inherit the complete, tested scanning
 * infrastructure: line iteration, comment skipping, pattern matching, evidence
 * generation, and confidence calculation. Implementing {@link ScannerEngine}
 * adds the metadata contract required for auto-registration in
 * {@link ScannerManager}.</p>
 *
 * <p><strong>Design contract:</strong> Subclasses only provide:</p>
 * <ol>
 *   <li>A static {@code RULES} list populated in a {@code static} block.</li>
 *   <li>Implementation of {@link #getRules()} returning that list.</li>
 *   <li>Implementation of {@link ScannerEngine} metadata methods.</li>
 *   <li>Optional override of {@link #passesContextualCheck} when contextual
 *       analysis is required.</li>
 * </ol>
 *
 * <p>All scanning logic (iteration, filtering, evidence building) lives in
 * {@link BaseRuleEngine} and is never duplicated.</p>
 *
 * @author Sanjay
 * @version 2.0.0
 * @see AbstractConfigScannerEngine
 */
public abstract class AbstractScannerEngine extends BaseRuleEngine implements ScannerEngine {

    /**
     * All source-code engines scan SOURCE files.
     * Override in subclasses only if a specific engine handles a different category.
     */
    @Override
    public FileCategory getCategory() {
        return FileCategory.SOURCE;
    }

    /**
     * Source-code engines are matched by extension, not by exact filename.
     * Returns an empty list by default; override for engines like Shell that
     * may also match shebang-only files.
     */
    @Override
    public List<String> getSupportedFileNames() {
        return List.of();
    }
}
