package com.sanjay.aisecurity.analyzer.config;

import com.sanjay.aisecurity.analyzer.AbstractConfigScannerEngine;
import com.sanjay.aisecurity.analyzer.SecurityRule;
import com.sanjay.aisecurity.enums.Severity;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Scanner for .env files.
 */
@Component
public class EnvFileScanner extends AbstractConfigScannerEngine {

    private static final List<SecurityRule> RULES = new ArrayList<>();

    static {
        RULES.add(SecurityRule.builder()
                .id("ENV-001")
                .name("Hardcoded Secret in .env")
                .description("Storing production secrets in checked-in .env files exposes them to all repository users.")
                .recommendation("Do not commit .env files containing real secrets to version control. Use .env.example instead.")
                .severity(Severity.HIGH)
                .baseConfidence(0.9)
                .owaspCategory("A07:2021-Identification and Authentication Failures")
                .cweId("CWE-798")
                .detectionType("REGEX")
                .reference("https://cwe.mitre.org/data/definitions/798.html")
                .language("CONFIG_ENV")
                .subcategory("Authentication")
                .pattern(Pattern.compile("(?i)(PASSWORD|SECRET|API_KEY|TOKEN)\\s*=\\s*[\"']?[a-zA-Z0-9]+[\"']?"))
                .build());
    }

    @Override
    public String getEngineId() { return "CONFIG_ENV"; }

    @Override
    public String getEngineName() { return "Env Config Scanner"; }

    @Override
    public List<String> getSupportedExtensions() { return List.of("env"); }

    @Override
    protected List<SecurityRule> getRules() { return RULES; }
}
