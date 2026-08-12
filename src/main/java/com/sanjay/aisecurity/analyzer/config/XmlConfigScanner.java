package com.sanjay.aisecurity.analyzer.config;

import com.sanjay.aisecurity.analyzer.AbstractConfigScannerEngine;
import com.sanjay.aisecurity.analyzer.SecurityRule;
import com.sanjay.aisecurity.enums.Severity;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Scanner for XML configuration files (e.g., web.xml, pom.xml).
 */
@Component
public class XmlConfigScanner extends AbstractConfigScannerEngine {

    private static final List<SecurityRule> RULES = new ArrayList<>();

    static {
        RULES.add(SecurityRule.builder()
                .id("XML-001")
                .name("Missing HTTPOnly flag in session config")
                .description("Session cookies without HttpOnly can be accessed by JavaScript (XSS risk).")
                .recommendation("Set <http-only>true</http-only> in your session-config.")
                .severity(Severity.MEDIUM)
                .baseConfidence(0.9)
                .owaspCategory("A05:2021-Security Misconfiguration")
                .cweId("CWE-1004")
                .detectionType("REGEX")
                .reference("https://cwe.mitre.org/data/definitions/1004.html")
                .language("CONFIG_XML")
                .subcategory("Configuration")
                .pattern(Pattern.compile("<http-only>false</http-only>"))
                .build());
    }

    @Override
    public String getEngineId() { return "CONFIG_XML"; }

    @Override
    public String getEngineName() { return "XML Config Scanner"; }

    @Override
    public List<String> getSupportedExtensions() { return List.of("xml"); }

    @Override
    protected List<SecurityRule> getRules() { return RULES; }
}
