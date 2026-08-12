package com.sanjay.aisecurity.analyzer.config;

import com.sanjay.aisecurity.analyzer.AbstractConfigScannerEngine;
import com.sanjay.aisecurity.analyzer.SecurityRule;
import com.sanjay.aisecurity.enums.Severity;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Scanner for Dockerfiles.
 */
@Component
public class DockerfileScanner extends AbstractConfigScannerEngine {

    private static final List<SecurityRule> RULES = new ArrayList<>();

    static {
        RULES.add(SecurityRule.builder()
                .id("DOCKER-001")
                .name("Running as Root")
                .description("Running containers as root is a security risk.")
                .recommendation("Add a USER instruction to run as a non-root user.")
                .severity(Severity.MEDIUM)
                .baseConfidence(0.8)
                .owaspCategory("A05:2021-Security Misconfiguration")
                .cweId("CWE-269")
                .detectionType("REGEX")
                .reference("https://cwe.mitre.org/data/definitions/269.html")
                .language("CONFIG_DOCKER")
                .subcategory("Configuration")
                .pattern(Pattern.compile("(?i)^USER\\s+root"))
                .build());

        RULES.add(SecurityRule.builder()
                .id("DOCKER-002")
                .name("Hardcoded Secret in ENV")
                .description("Storing secrets in ENV instructions exposes them in image layers.")
                .recommendation("Pass secrets at build time or runtime. Do not bake them into the image.")
                .severity(Severity.HIGH)
                .baseConfidence(0.9)
                .owaspCategory("A07:2021-Identification and Authentication Failures")
                .cweId("CWE-798")
                .detectionType("REGEX")
                .reference("https://cwe.mitre.org/data/definitions/798.html")
                .language("CONFIG_DOCKER")
                .subcategory("Authentication")
                .pattern(Pattern.compile("(?i)^ENV\\s+.*(PASSWORD|SECRET|API_KEY|TOKEN)\\s*=?\\s*[\"']?[^\"']+[\"']?"))
                .build());
    }

    @Override
    public String getEngineId() { return "CONFIG_DOCKER"; }

    @Override
    public String getEngineName() { return "Dockerfile Scanner"; }

    @Override
    public List<String> getSupportedExtensions() { return List.of("docker"); } // The LanguageDetector maps "Dockerfile" to this.

    @Override
    protected List<SecurityRule> getRules() { return RULES; }
}
