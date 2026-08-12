package com.sanjay.aisecurity.analyzer.config;

import com.sanjay.aisecurity.analyzer.AbstractConfigScannerEngine;
import com.sanjay.aisecurity.analyzer.SecurityRule;
import com.sanjay.aisecurity.enums.Severity;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Scanner for YAML configuration files.
 */
@Component
public class YamlConfigScanner extends AbstractConfigScannerEngine {

    private static final List<SecurityRule> RULES = new ArrayList<>();

    static {
        RULES.add(SecurityRule.builder()
                .id("YAML-001")
                .name("Hardcoded Password in YAML")
                .description("Password is in cleartext in the YAML file.")
                .recommendation("Use environment variables like ${DB_PASSWORD} instead of hardcoding.")
                .severity(Severity.HIGH)
                .baseConfidence(0.9)
                .owaspCategory("A07:2021-Identification and Authentication Failures")
                .cweId("CWE-798")
                .detectionType("REGEX")
                .reference("https://cwe.mitre.org/data/definitions/798.html")
                .language("CONFIG_YAML")
                .subcategory("Authentication")
                .pattern(Pattern.compile("(?i)(?:password|secret|key):\\s*(?!\\$\\{)[\"']?[a-zA-Z0-9]+[\"']?"))
                .build());

        RULES.add(SecurityRule.builder()
                .id("YAML-002")
                .name("Debug Mode Enabled in Config")
                .description("debug: true detected in YAML configuration — exposes stack traces and internals in production.")
                .recommendation("Set debug: false in production profile. Use Spring Boot profiles to isolate dev config.")
                .severity(Severity.LOW)
                .baseConfidence(0.80)
                .owaspCategory("A05:2021-Security Misconfiguration")
                .cweId("CWE-489")
                .detectionType("REGEX")
                .reference("https://cwe.mitre.org/data/definitions/489.html")
                .language("CONFIG_YAML")
                .subcategory("Configuration")
                .pattern(Pattern.compile("(?i)^\\s*debug:\\s*true"))
                .build());

        RULES.add(SecurityRule.builder()
                .id("YAML-003")
                .name("Wildcard CORS in Config")
                .description("Wildcard allowed-origins: '*' in YAML configuration permits unrestricted cross-origin access.")
                .recommendation("Specify explicit allowed origins instead of '*'.")
                .severity(Severity.MEDIUM)
                .baseConfidence(0.85)
                .owaspCategory("A05:2021-Security Misconfiguration")
                .cweId("CWE-942")
                .detectionType("REGEX")
                .reference("https://owasp.org/www-community/attacks/CORS_OriginHeaderScrutiny")
                .language("CONFIG_YAML")
                .subcategory("Configuration")
                .pattern(Pattern.compile("(?i)allowed-origins?:\\s*[\"']?\\*[\"']?"))
                .build());
    }

    @Override
    public String getEngineId() { return "CONFIG_YAML"; }

    @Override
    public String getEngineName() { return "YAML Config Scanner"; }

    @Override
    public List<String> getSupportedExtensions() { return List.of("yml", "yaml"); }

    @Override
    protected List<SecurityRule> getRules() { return RULES; }
}
