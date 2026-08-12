package com.sanjay.aisecurity.analyzer.scala;

import com.sanjay.aisecurity.analyzer.AbstractScannerEngine;
import com.sanjay.aisecurity.analyzer.SecurityRule;
import com.sanjay.aisecurity.enums.Severity;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Stub Static Analysis Engine for Scala.
 *
 * <p>Registers Scala support in the framework but does not yet implement a full
 * rule set. Logs an informational finding that the file was detected.</p>
 *
 * @author Sanjay
 * @version 2.0.0
 */
@Component
public class ScalaRuleEngine extends AbstractScannerEngine {

    private static final List<SecurityRule> RULES = new ArrayList<>();

    static {
        RULES.add(SecurityRule.builder()
                .id("SCALA-STUB")
                .name("Scala Engine Stub")
                .description("Scala file detected. Comprehensive Scala rule set is pending implementation.")
                .recommendation("No action required. File is recognized by the scanner framework.")
                .severity(Severity.INFORMATIONAL)
                .baseConfidence(1.0)
                .owaspCategory("N/A")
                .cweId("N/A")
                .detectionType("REGEX")
                .reference("")
                .language("SCALA")
                .subcategory("Framework")
                .pattern(Pattern.compile("package\\s+|import\\s+")) // Just to match *something* valid
                .build());
    }

    @Override
    public String getEngineId() {
        return "SCALA";
    }

    @Override
    public String getEngineName() {
        return "Scala Source Scanner (Stub)";
    }

    @Override
    public List<String> getSupportedExtensions() {
        return List.of("scala");
    }

    @Override
    protected List<SecurityRule> getRules() {
        return RULES;
    }
}
