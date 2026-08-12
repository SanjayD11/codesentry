package com.sanjay.aisecurity.analyzer.go;

import com.sanjay.aisecurity.analyzer.BaseRuleEngine;
import com.sanjay.aisecurity.analyzer.SecurityRule;
import com.sanjay.aisecurity.enums.Severity;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

@Component
public class GoRuleEngine extends BaseRuleEngine {

    private static final List<SecurityRule> RULES = new ArrayList<>();

    static {
        RULES.add(SecurityRule.builder()
                .id("GO-001")
                .name("OS Command Injection")
                .description("Untrusted data passed to exec.Command allows command injection.")
                .recommendation("Avoid dynamic command construction or tightly validate arguments.")
                .severity(Severity.CRITICAL)
                .baseConfidence(0.9)
                .owaspCategory("A03:2021-Injection")
                .cweId("CWE-78")
                .detectionType("REGEX")
                .reference("https://owasp.org/www-community/attacks/Command_Injection")
                .pattern(Pattern.compile("exec\\.Command\\s*\\("))
                .build());

        RULES.add(SecurityRule.builder()
                .id("GO-002")
                .name("SQL Injection")
                .description("Potential SQL injection via unparameterized string formatting.")
                .recommendation("Use parameterized queries e.g., db.Query(\"SELECT * FROM users WHERE id=?\", id).")
                .severity(Severity.HIGH)
                .baseConfidence(0.85)
                .owaspCategory("A03:2021-Injection")
                .cweId("CWE-89")
                .detectionType("CONTEXT")
                .reference("https://owasp.org/www-community/attacks/SQL_Injection")
                .pattern(Pattern.compile("(db\\.Query|db\\.Exec|db\\.QueryRow)\\s*\\("))
                .build());
                
        RULES.add(SecurityRule.builder()
                .id("GO-003")
                .name("Path Traversal")
                .description("Unvalidated input passed to os.Open or ReadFile.")
                .recommendation("Validate file paths, restrict to expected directories, use filepath.Clean().")
                .severity(Severity.HIGH)
                .baseConfidence(0.6)
                .owaspCategory("A01:2021-Broken Access Control")
                .cweId("CWE-22")
                .detectionType("CONTEXT")
                .reference("https://owasp.org/www-community/attacks/Path_Traversal")
                .pattern(Pattern.compile("(os\\.Open|ioutil\\.ReadFile|os\\.ReadFile)\\s*\\("))
                .build());

        RULES.add(SecurityRule.builder()
                .id("GO-004")
                .name("Weak Cryptography (MD5/SHA1)")
                .description("Usage of weak hashing algorithms.")
                .recommendation("Use crypto/sha256 or crypto/sha512. Use golang.org/x/crypto/bcrypt for passwords.")
                .severity(Severity.MEDIUM)
                .baseConfidence(1.0)
                .owaspCategory("A02:2021-Cryptographic Failures")
                .cweId("CWE-327")
                .detectionType("REGEX")
                .reference("https://cwe.mitre.org/data/definitions/327.html")
                .pattern(Pattern.compile("(md5\\.New\\(\\)|sha1\\.New\\(\\))"))
                .build());

        RULES.add(SecurityRule.builder()
                .id("GO-005")
                .name("Hardcoded Credentials")
                .description("Potential hardcoded database/API secret detected.")
                .recommendation("Store secrets in environment variables (os.Getenv).")
                .severity(Severity.HIGH)
                .baseConfidence(0.7)
                .owaspCategory("A07:2021-Identification and Authentication Failures")
                .cweId("CWE-798")
                .detectionType("REGEX")
                .reference("https://cwe.mitre.org/data/definitions/798.html")
                .pattern(Pattern.compile("(?i)(password|passwd|pwd|secret|api_key)\\s*:=\\s*\"[^\"]{3,}\""))
                .build());
    }

    @Override
    protected List<SecurityRule> getRules() {
        return RULES;
    }

    @Override
    protected boolean passesContextualCheck(SecurityRule rule, String line, String[] allLines, int lineIndex) {
        if ("SQL Injection".equals(rule.getName())) {
            // High confidence if fmt.Sprintf or string concatenation is used
            return line.contains("fmt.Sprintf") || line.contains("+");
        }
        if ("Path Traversal".equals(rule.getName())) {
             // Safe if hardcoded literal
             if (line.matches(".*(os\\.Open|ioutil\\.ReadFile|os\\.ReadFile)\\s*\\(\\s*\"[^\"]+\"\\s*\\).*")) {
                 return false;
             }
             return true;
        }
        return true;
    }
}
