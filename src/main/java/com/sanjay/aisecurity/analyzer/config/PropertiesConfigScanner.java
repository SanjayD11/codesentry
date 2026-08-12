package com.sanjay.aisecurity.analyzer.config;

import com.sanjay.aisecurity.analyzer.AbstractConfigScannerEngine;
import com.sanjay.aisecurity.analyzer.SecurityRule;
import com.sanjay.aisecurity.enums.Severity;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Scanner for Java .properties files.
 */
@Component
public class PropertiesConfigScanner extends AbstractConfigScannerEngine {

    private static final List<SecurityRule> RULES = new ArrayList<>();

    static {
        RULES.add(SecurityRule.builder()
                .id("PROP-001")
                .name("Hardcoded Database Password")
                .description("Database password is in cleartext in the properties file.")
                .recommendation("Use environment variables like ${DB_PASSWORD} instead of hardcoding.")
                .severity(Severity.HIGH)
                .baseConfidence(0.9)
                .owaspCategory("A07:2021-Identification and Authentication Failures")
                .cweId("CWE-798")
                .detectionType("REGEX")
                .reference("https://cwe.mitre.org/data/definitions/798.html")
                .language("CONFIG_PROPERTIES")
                .subcategory("Authentication")
                .pattern(Pattern.compile("(?i)spring\\.datasource\\.password\\s*=\\s*(?!\\$\\{)[a-zA-Z0-9]+"))
                .build());

        RULES.add(SecurityRule.builder()
                .id("PROP-002")
                .name("Hardcoded Secret Key")
                .description("Secret key or JWT secret is in cleartext.")
                .recommendation("Use environment variables or a secrets manager.")
                .severity(Severity.HIGH)
                .baseConfidence(0.9)
                .owaspCategory("A07:2021-Identification and Authentication Failures")
                .cweId("CWE-798")
                .detectionType("REGEX")
                .reference("https://cwe.mitre.org/data/definitions/798.html")
                .language("CONFIG_PROPERTIES")
                .subcategory("Authentication")
                .pattern(Pattern.compile("(?i)(jwt\\.secret|app\\.secret|api\\.key)\\s*=\\s*(?!\\$\\{)[a-zA-Z0-9_-]+"))
                .build());

        RULES.add(SecurityRule.builder()
                .id("PROP-003")
                .name("Debug Mode Enabled in Properties")
                .description("debug=true or logging.level.root=DEBUG detected — may expose sensitive internals in production.")
                .recommendation("Set debug=false and use production logging levels in production properties.")
                .severity(Severity.LOW)
                .baseConfidence(0.75)
                .owaspCategory("A05:2021-Security Misconfiguration")
                .cweId("CWE-489")
                .detectionType("REGEX")
                .reference("https://cwe.mitre.org/data/definitions/489.html")
                .language("CONFIG_PROPERTIES")
                .subcategory("Configuration")
                .pattern(Pattern.compile("(?i)^\\s*(?:debug|spring\\.jpa\\.show-sql)\\s*=\\s*true"))
                .build());

        RULES.add(SecurityRule.builder()
                .id("PROP-004")
                .name("Wildcard CORS in Properties")
                .description("allowed.origins=* in properties file permits unrestricted cross-origin access.")
                .recommendation("Specify an explicit list of allowed origins.")
                .severity(Severity.MEDIUM)
                .baseConfidence(0.80)
                .owaspCategory("A05:2021-Security Misconfiguration")
                .cweId("CWE-942")
                .detectionType("REGEX")
                .reference("https://owasp.org/www-community/attacks/CORS_OriginHeaderScrutiny")
                .language("CONFIG_PROPERTIES")
                .subcategory("Configuration")
                .pattern(Pattern.compile("(?i)(?:allowed[._-]origins?|cors[._]origins?)\\s*=\\s*\\*"))
                .build());
    }

    @Override
    public String getEngineId() { return "CONFIG_PROPERTIES"; }

    @Override
    public String getEngineName() { return "Properties Config Scanner"; }

    @Override
    public List<String> getSupportedExtensions() { return List.of("properties"); }

    @Override
    protected List<SecurityRule> getRules() { return RULES; }
}
