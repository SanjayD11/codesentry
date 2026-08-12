package com.sanjay.aisecurity.analyzer.rust;

import com.sanjay.aisecurity.analyzer.AbstractScannerEngine;
import com.sanjay.aisecurity.analyzer.SecurityRule;
import com.sanjay.aisecurity.enums.Severity;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Stub Static Analysis Engine for Rust.
 *
 * <p>Registers Rust support in the framework but does not yet implement a full
 * rule set. Logs an informational finding that the file was detected.</p>
 *
 * @author Sanjay
 * @version 2.0.0
 */
@Component
public class RustRuleEngine extends AbstractScannerEngine {

    private static final List<SecurityRule> RULES = new ArrayList<>();

    static {
        RULES.add(SecurityRule.builder()
                .id("RUST-STUB")
                .name("Rust Engine Stub")
                .description("Rust file detected. Comprehensive Rust rule set is pending implementation.")
                .recommendation("No action required. File is recognized by the scanner framework.")
                .severity(Severity.INFORMATIONAL)
                .baseConfidence(1.0)
                .owaspCategory("N/A")
                .cweId("N/A")
                .detectionType("REGEX")
                .reference("")
                .language("RUST")
                .subcategory("Framework")
                .pattern(Pattern.compile("fn\\s+|use\\s+")) // Just to match *something* valid
                .build());
    }

    @Override
    public String getEngineId() {
        return "RUST";
    }

    @Override
    public String getEngineName() {
        return "Rust Source Scanner (Stub)";
    }

    @Override
    public List<String> getSupportedExtensions() {
        return List.of("rs");
    }

    @Override
    protected List<SecurityRule> getRules() {
        return RULES;
    }
}
