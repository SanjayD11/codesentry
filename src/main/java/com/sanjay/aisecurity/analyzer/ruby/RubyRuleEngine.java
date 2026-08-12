package com.sanjay.aisecurity.analyzer.ruby;

import com.sanjay.aisecurity.analyzer.AbstractScannerEngine;
import com.sanjay.aisecurity.analyzer.SecurityRule;
import com.sanjay.aisecurity.enums.Severity;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Static Analysis Engine for Ruby (and Ruby on Rails).
 *
 * <p>Detects Ruby-specific vulnerabilities such as eval(), system(), find_by_sql
 * with string concatenation, mass assignment, and hardcoded credentials.</p>
 *
 * @author Sanjay
 * @version 2.0.0
 */
@Component
public class RubyRuleEngine extends AbstractScannerEngine {

    private static final List<SecurityRule> RULES = new ArrayList<>();

    static {
        RULES.add(SecurityRule.builder()
                .id("RB-001")
                .name("OS Command Injection")
                .description("Usage of system(), exec(), or backticks (`) with untrusted data can lead to OS command injection.")
                .recommendation("Use parameterized APIs or strictly validate input before executing OS commands.")
                .severity(Severity.CRITICAL)
                .baseConfidence(0.9)
                .owaspCategory("A03:2021-Injection")
                .cweId("CWE-78")
                .detectionType("REGEX")
                .reference("https://cwe.mitre.org/data/definitions/78.html")
                .language("RUBY")
                .subcategory("Injection")
                .pattern(Pattern.compile("(?:system|exec)\\s*\\(|`.*#\\{.*}.*`"))
                .build());

        RULES.add(SecurityRule.builder()
                .id("RB-002")
                .name("Arbitrary Code Execution")
                .description("Usage of eval() with untrusted input allows arbitrary code execution.")
                .recommendation("Avoid using eval(). If necessary, sanitize input strictly.")
                .severity(Severity.CRITICAL)
                .baseConfidence(0.95)
                .owaspCategory("A03:2021-Injection")
                .cweId("CWE-94")
                .detectionType("REGEX")
                .reference("https://cwe.mitre.org/data/definitions/94.html")
                .language("RUBY")
                .subcategory("Injection")
                .pattern(Pattern.compile("\\beval\\s*\\("))
                .build());

        RULES.add(SecurityRule.builder()
                .id("RB-003")
                .name("SQL Injection (ActiveRecord)")
                .description("Using find_by_sql, execute, or where with string interpolation can lead to SQL Injection.")
                .recommendation("Use parameterized queries (e.g., where('name = ?', name)).")
                .severity(Severity.HIGH)
                .baseConfidence(0.85)
                .owaspCategory("A03:2021-Injection")
                .cweId("CWE-89")
                .detectionType("REGEX")
                .reference("https://cwe.mitre.org/data/definitions/89.html")
                .language("RUBY")
                .subcategory("Injection")
                .pattern(Pattern.compile("(?:find_by_sql|execute|where)\\s*\\([\"'].*#\\{.*}[\"']\\)"))
                .build());

        RULES.add(SecurityRule.builder()
                .id("RB-004")
                .name("Mass Assignment")
                .description("Use of attr_accessible without careful restriction can allow users to modify protected attributes.")
                .recommendation("Use Strong Parameters (require/permit) in Rails 4+ instead of attr_accessible.")
                .severity(Severity.MEDIUM)
                .baseConfidence(0.8)
                .owaspCategory("A01:2021-Broken Access Control")
                .cweId("CWE-915")
                .detectionType("REGEX")
                .reference("https://cwe.mitre.org/data/definitions/915.html")
                .language("RUBY")
                .subcategory("Access Control")
                .pattern(Pattern.compile("\\battr_accessible\\b"))
                .build());
                
        RULES.add(SecurityRule.builder()
                .id("RB-005")
                .name("Hardcoded Credentials")
                .description("Potential hardcoded password or secret detected.")
                .recommendation("Use environment variables (ENV['...']) instead of hardcoding credentials.")
                .severity(Severity.HIGH)
                .baseConfidence(0.7)
                .owaspCategory("A07:2021-Identification and Authentication Failures")
                .cweId("CWE-798")
                .detectionType("REGEX")
                .reference("https://cwe.mitre.org/data/definitions/798.html")
                .language("RUBY")
                .subcategory("Authentication")
                .pattern(Pattern.compile("(?i)(?:password|secret|api_?key|token)\\s*(?::|=)\\s*[\"'][^\"']{3,}[\"']"))
                .build());
    }

    @Override
    public String getEngineId() {
        return "RUBY";
    }

    @Override
    public String getEngineName() {
        return "Ruby Source Scanner";
    }

    @Override
    public List<String> getSupportedExtensions() {
        return List.of("rb");
    }

    @Override
    protected List<SecurityRule> getRules() {
        return RULES;
    }
}
