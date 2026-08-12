package com.sanjay.aisecurity.analyzer.swift;

import com.sanjay.aisecurity.analyzer.AbstractScannerEngine;
import com.sanjay.aisecurity.analyzer.SecurityRule;
import com.sanjay.aisecurity.enums.Severity;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Stub Static Analysis Engine for Swift.
 *
 * <p>Registers Swift support in the framework but does not yet implement a full
 * rule set. Logs an informational finding that the file was detected.</p>
 *
 * @author Sanjay
 * @version 2.0.0
 */
@Component
public class SwiftRuleEngine extends AbstractScannerEngine {

    private static final List<SecurityRule> RULES = new ArrayList<>();

    static {
        RULES.add(SecurityRule.builder()
                .id("SWIFT-STUB")
                .name("Swift Engine Stub")
                .description("Swift file detected. Comprehensive Swift rule set is pending implementation.")
                .recommendation("No action required. File is recognized by the scanner framework.")
                .severity(Severity.INFORMATIONAL)
                .baseConfidence(1.0)
                .owaspCategory("N/A")
                .cweId("N/A")
                .detectionType("REGEX")
                .reference("")
                .language("SWIFT")
                .subcategory("Framework")
                .pattern(Pattern.compile("func\\s+|import\\s+")) // Just to match *something* valid
                .build());
    }

    @Override
    public String getEngineId() {
        return "SWIFT";
    }

    @Override
    public String getEngineName() {
        return "Swift Source Scanner (Stub)";
    }

    @Override
    public List<String> getSupportedExtensions() {
        return List.of("swift");
    }

    @Override
    protected List<SecurityRule> getRules() {
        return RULES;
    }
}
