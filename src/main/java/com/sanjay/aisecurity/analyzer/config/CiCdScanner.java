package com.sanjay.aisecurity.analyzer.config;

import com.sanjay.aisecurity.analyzer.AbstractConfigScannerEngine;
import com.sanjay.aisecurity.analyzer.SecurityRule;
import com.sanjay.aisecurity.enums.Severity;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Scanner for CI/CD configuration files (e.g., GitHub Actions, GitLab CI, Jenkinsfile).
 */
@Component
public class CiCdScanner extends AbstractConfigScannerEngine {

    private static final List<SecurityRule> RULES = new ArrayList<>();

    static {
        RULES.add(SecurityRule.builder()
                .id("CICD-001")
                .name("Hardcoded Secret in CI/CD Pipeline")
                .description("Hardcoding secrets in pipeline scripts exposes them to anyone with read access to the repo.")
                .recommendation("Use GitHub Secrets or GitLab CI/CD Variables.")
                .severity(Severity.HIGH)
                .baseConfidence(0.9)
                .owaspCategory("A07:2021-Identification and Authentication Failures")
                .cweId("CWE-798")
                .detectionType("REGEX")
                .reference("https://cwe.mitre.org/data/definitions/798.html")
                .language("CONFIG_CICD")
                .subcategory("Authentication")
                .pattern(Pattern.compile("(?i)(PASSWORD|SECRET|API_KEY|TOKEN)\\s*[:=]\\s*[\"']?[a-zA-Z0-9]+[\"']?"))
                .build());
    }

    @Override
    public String getEngineId() { return "CONFIG_CICD"; }

    @Override
    public String getEngineName() { return "CI/CD Config Scanner"; }

    @Override
    public List<String> getSupportedExtensions() { return List.of("cicd"); }

    @Override
    protected List<SecurityRule> getRules() { return RULES; }
}
