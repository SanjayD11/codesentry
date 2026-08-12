package com.sanjay.aisecurity.analyzer.cpp;

import com.sanjay.aisecurity.analyzer.BaseRuleEngine;
import com.sanjay.aisecurity.analyzer.SecurityRule;
import com.sanjay.aisecurity.enums.Severity;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

@Component
public class CppRuleEngine extends BaseRuleEngine {

    private static final List<SecurityRule> RULES = new ArrayList<>();

    static {
        RULES.add(SecurityRule.builder()
                .id("CPP-001")
                .name("Buffer Overflow (strcpy/sprintf/gets)")
                .description("Unbounded copy operations can lead to buffer overflows.")
                .recommendation("Use bounded alternatives like strncpy, snprintf, or C++ std::string.")
                .severity(Severity.CRITICAL)
                .baseConfidence(1.0)
                .owaspCategory("A03:2021-Injection") // Or older memory corruption
                .cweId("CWE-120")
                .detectionType("REGEX")
                .reference("https://cwe.mitre.org/data/definitions/120.html")
                .pattern(Pattern.compile("\\b(strcpy|sprintf|gets|strcat)\\s*\\("))
                .build());

        RULES.add(SecurityRule.builder()
                .id("CPP-002")
                .name("OS Command Injection")
                .description("Usage of system() or popen() with untrusted data.")
                .recommendation("Avoid system(). Use exec() family functions with proper arg arrays.")
                .severity(Severity.CRITICAL)
                .baseConfidence(0.9)
                .owaspCategory("A03:2021-Injection")
                .cweId("CWE-78")
                .detectionType("REGEX")
                .reference("https://owasp.org/www-community/attacks/Command_Injection")
                .pattern(Pattern.compile("\\b(system|popen)\\s*\\("))
                .build());
                
        RULES.add(SecurityRule.builder()
                .id("CPP-003")
                .name("Format String Vulnerability")
                .description("Untrusted input passed as format string.")
                .recommendation("Always hardcode the format string: printf(\"%s\", input).")
                .severity(Severity.HIGH)
                .baseConfidence(0.7)
                .owaspCategory("A03:2021-Injection")
                .cweId("CWE-134")
                .detectionType("CONTEXT")
                .reference("https://owasp.org/www-community/attacks/Format_string_attack")
                .pattern(Pattern.compile("\\b(printf|sprintf|snprintf|fprintf)\\s*\\("))
                .build());

        RULES.add(SecurityRule.builder()
                .id("CPP-004")
                .name("Path Traversal")
                .description("Unvalidated input passed to fopen or open.")
                .recommendation("Validate file paths and restrict to expected directories.")
                .severity(Severity.HIGH)
                .baseConfidence(0.6)
                .owaspCategory("A01:2021-Broken Access Control")
                .cweId("CWE-22")
                .detectionType("CONTEXT")
                .reference("https://owasp.org/www-community/attacks/Path_Traversal")
                .pattern(Pattern.compile("\\b(fopen|open)\\s*\\("))
                .build());
    }

    @Override
    protected List<SecurityRule> getRules() {
        return RULES;
    }

    @Override
    protected boolean passesContextualCheck(SecurityRule rule, String line, String[] allLines, int lineIndex) {
        if ("Format String Vulnerability".equals(rule.getName())) {
            // Flag if there are no quotes inside the parenthesis (e.g. printf(variable))
            return !line.matches(".*printf\\s*\\([^\"']*\"[^\"]*\".*\\).*");
        }
        if ("OS Command Injection".equals(rule.getName())) {
             if (line.matches(".*system\\s*\\(\\s*\"[^\"]+\"\\s*\\).*")) {
                 return false;
             }
             return true;
        }
        if ("Path Traversal".equals(rule.getName())) {
             if (line.matches(".*(fopen|open)\\s*\\(\\s*\"[^\"]+\"\\s*,.*")) {
                 return false;
             }
             return true;
        }
        return true;
    }
}
