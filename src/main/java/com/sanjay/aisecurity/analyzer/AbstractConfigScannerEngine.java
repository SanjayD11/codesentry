package com.sanjay.aisecurity.analyzer;

/**
 * Abstract base for all new configuration-file scanner engines introduced in Phase 2+.
 *
 * <p>Extends {@link AbstractScannerEngine} but overrides {@link #getCategory()}
 * to return {@link FileCategory#CONFIG}. Inherits all scanning infrastructure
 * from {@link BaseRuleEngine}.</p>
 *
 * @author Sanjay
 * @version 2.0.0
 */
public abstract class AbstractConfigScannerEngine extends AbstractScannerEngine {

    @Override
    public FileCategory getCategory() {
        return FileCategory.CONFIG;
    }
}
