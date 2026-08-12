package com.sanjay.aisecurity.analyzer.php;

import com.sanjay.aisecurity.analyzer.BaseRuleEngine;
import com.sanjay.aisecurity.analyzer.SecurityRule;
import com.sanjay.aisecurity.enums.Severity;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

@Component
public class PhpRuleEngine extends BaseRuleEngine {

    private static final List<SecurityRule> RULES = new ArrayList<>();

    static {
        RULES.add(SecurityRule.builder()
                .id("PHP-001")
                .name("File Inclusion (LFI/RFI)")
                .description("Untrusted data passed to include() or require() can lead to local/remote file inclusion.")
                .recommendation("Avoid dynamic paths. Use an allowlist or basename() before inclusion.")
                .severity(Severity.CRITICAL)
                .baseConfidence(0.9)
                .owaspCategory("A01:2021-Broken Access Control")
                .cweId("CWE-98")
                .detectionType("REGEX")
                .reference("https://owasp.org/www-project-web-security-testing-guide/latest/4-Web_Application_Security_Testing/07-Input_Validation_Testing/11.1-Testing_for_Local_File_Inclusion")
                .pattern(Pattern.compile("(include|require|include_once|require_once)\\s*\\(\\s*\\$_(GET|POST|REQUEST|COOKIE)"))
                .build());

        RULES.add(SecurityRule.builder()
                .id("PHP-002")
                .name("OS Command Injection")
                .description("Usage of system(), exec(), or shell_exec() with untrusted data.")
                .recommendation("Use escapeshellarg() and escapeshellcmd(), or avoid calling the shell entirely.")
                .severity(Severity.CRITICAL)
                .baseConfidence(0.95)
                .owaspCategory("A03:2021-Injection")
                .cweId("CWE-78")
                .detectionType("REGEX")
                .reference("https://owasp.org/www-community/attacks/Command_Injection")
                .pattern(Pattern.compile("(system|exec|shell_exec|passthru)\\s*\\("))
                .build());

        RULES.add(SecurityRule.builder()
                .id("PHP-003")
                .name("SQL Injection")
                .description("Unparameterized SQL query execution.")
                .recommendation("Use PDO with prepared statements.")
                .severity(Severity.HIGH)
                .baseConfidence(0.85)
                .owaspCategory("A03:2021-Injection")
                .cweId("CWE-89")
                .detectionType("CONTEXT")
                .reference("https://owasp.org/www-community/attacks/SQL_Injection")
                .pattern(Pattern.compile("(mysqli_query|mysql_query|->query)\\s*\\("))
                .build());

        RULES.add(SecurityRule.builder()
                .id("PHP-004")
                .name("Cross-Site Scripting (XSS)")
                .description("Directly outputting user input can lead to Reflected XSS.")
                .recommendation("Use htmlspecialchars() before echoing user input.")
                .severity(Severity.HIGH)
                .baseConfidence(0.9)
                .owaspCategory("A03:2021-Injection")
                .cweId("CWE-79")
                .detectionType("REGEX")
                .reference("https://owasp.org/www-community/attacks/xss/")
                .pattern(Pattern.compile("(echo|print)\\s+.*\\$_(GET|POST|REQUEST|COOKIE)"))
                .build());

        RULES.add(SecurityRule.builder()
                .id("PHP-005")
                .name("Arbitrary Code Execution")
                .description("Usage of eval() is extremely dangerous.")
                .recommendation("Avoid eval() completely.")
                .severity(Severity.CRITICAL)
                .baseConfidence(1.0)
                .owaspCategory("A03:2021-Injection")
                .cweId("CWE-94")
                .detectionType("REGEX")
                .reference("https://cwe.mitre.org/data/definitions/94.html")
                .pattern(Pattern.compile("eval\\s*\\("))
                .build());

        RULES.add(SecurityRule.builder()
                .id("PHP-006")
                .name("Hardcoded Credentials")
                .description("Potential hardcoded database/API secret detected.")
                .recommendation("Store secrets in environment variables or configuration files outside the web root.")
                .severity(Severity.HIGH)
                .baseConfidence(0.7)
                .owaspCategory("A07:2021-Identification and Authentication Failures")
                .cweId("CWE-798")
                .detectionType("REGEX")
                .reference("https://cwe.mitre.org/data/definitions/798.html")
                .pattern(Pattern.compile("(?i)(\\$password|\\$passwd|\\$pwd|\\$secret|\\$api_key)\\s*=\\s*['\"][^'\"]{3,}['\"]"))
                .build());
    }

    @Override
    protected List<SecurityRule> getRules() {
        return RULES;
    }

    @Override
    protected boolean passesContextualCheck(SecurityRule rule, String line, String[] allLines, int lineIndex) {
        if ("SQL Injection".equals(rule.getName())) {
            // High confidence if it explicitly concatenates within the statement execution
            return line.contains(".") || line.contains("\"");
        }
        if ("OS Command Injection".equals(rule.getName())) {
             // Safe if it's hardcoded and does not use concatenation or variables
             if (line.matches(".*(system|exec|shell_exec|passthru)\\s*\\(\\s*['\"][^\\$'\"]+['\"]\\s*\\).*")) {
                 return false;
             }
             return true;
        }
        return true;
    }
}
