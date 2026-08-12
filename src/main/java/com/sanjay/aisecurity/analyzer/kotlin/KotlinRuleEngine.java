package com.sanjay.aisecurity.analyzer.kotlin;

import com.sanjay.aisecurity.analyzer.AbstractScannerEngine;
import com.sanjay.aisecurity.analyzer.SecurityRule;
import com.sanjay.aisecurity.enums.Severity;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Static Analysis Engine for Kotlin.
 *
 * <p>Detects Kotlin-specific vulnerabilities such as unsafe string templates
 * in SQL, weak crypto, and hardcoded secrets in val/const val.</p>
 *
 * @author Sanjay
 * @version 2.0.0
 */
@Component
public class KotlinRuleEngine extends AbstractScannerEngine {

    private static final List<SecurityRule> RULES = new ArrayList<>();

    static {
        RULES.add(SecurityRule.builder()
                .id("KT-001")
                .name("Unsafe SQL String Template")
                .description("Using Kotlin string templates ($var) inside SQL queries leads to SQL Injection.")
                .recommendation("Use parameterized queries (e.g., PreparedStatement) instead of string templates for SQL.")
                .severity(Severity.CRITICAL)
                .baseConfidence(0.9)
                .owaspCategory("A03:2021-Injection")
                .cweId("CWE-89")
                .detectionType("REGEX")
                .reference("https://cwe.mitre.org/data/definitions/89.html")
                .language("KOTLIN")
                .subcategory("Injection")
                .pattern(Pattern.compile("(?i)select\\s+.*\\s+from\\s+.*\\s+where\\s+.*=\\s*\"\\$\\{?.*}?\""))
                .build());

        RULES.add(SecurityRule.builder()
                .id("KT-002")
                .name("Hardcoded Secret in val")
                .description("Potential hardcoded secret or credential in a val or const val declaration.")
                .recommendation("Store secrets in environment variables or a secure vault, not in source code.")
                .severity(Severity.HIGH)
                .baseConfidence(0.8)
                .owaspCategory("A07:2021-Identification and Authentication Failures")
                .cweId("CWE-798")
                .detectionType("REGEX")
                .reference("https://cwe.mitre.org/data/definitions/798.html")
                .language("KOTLIN")
                .subcategory("Authentication")
                .pattern(Pattern.compile("(?i)(val|const\\s+val)\\s+(password|secret|api_?key|token)\\s*=\\s*\"[^\"]{3,}\""))
                .build());

        RULES.add(SecurityRule.builder()
                .id("KT-003")
                .name("Insecure File Permission")
                .description("Using java.io.File or Files.write with overly permissive access.")
                .recommendation("Ensure file permissions are restricted (e.g., using PosixFilePermissions).")
                .severity(Severity.MEDIUM)
                .baseConfidence(0.7)
                .owaspCategory("A01:2021-Broken Access Control")
                .cweId("CWE-732")
                .detectionType("REGEX")
                .reference("https://cwe.mitre.org/data/definitions/732.html")
                .language("KOTLIN")
                .subcategory("Access Control")
                .pattern(Pattern.compile("set(?:Readable|Writable|Executable)\\s*\\(\\s*true\\s*,\\s*false\\s*\\)"))
                .build());

        RULES.add(SecurityRule.builder()
                .id("KT-004")
                .name("Command Injection via Runtime")
                .description("Executing OS commands with Runtime.exec() can lead to command injection if input is untrusted.")
                .recommendation("Use ProcessBuilder and avoid passing raw user input to shells.")
                .severity(Severity.HIGH)
                .baseConfidence(0.85)
                .owaspCategory("A03:2021-Injection")
                .cweId("CWE-78")
                .detectionType("HYBRID")
                .reference("https://cwe.mitre.org/data/definitions/78.html")
                .language("KOTLIN")
                .subcategory("Injection")
                .pattern(Pattern.compile("Runtime\\.getRuntime\\(\\)\\.exec\\s*\\("))
                .build());
                
        RULES.add(SecurityRule.builder()
                .id("KT-005")
                .name("Weak Cryptography (Kotlin)")
                .description("Usage of weak hashing algorithms like MD5 or SHA-1.")
                .recommendation("Use SHA-256 or SHA-512 instead.")
                .severity(Severity.MEDIUM)
                .baseConfidence(1.0)
                .owaspCategory("A02:2021-Cryptographic Failures")
                .cweId("CWE-327")
                .detectionType("REGEX")
                .reference("https://cwe.mitre.org/data/definitions/327.html")
                .language("KOTLIN")
                .subcategory("Cryptography")
                .pattern(Pattern.compile("MessageDigest\\.getInstance\\s*\\(\\s*\"(MD5|SHA-1)\"\\s*\\)"))
                .build());
    }

    @Override
    public String getEngineId() {
        return "KOTLIN";
    }

    @Override
    public String getEngineName() {
        return "Kotlin Source Scanner";
    }

    @Override
    public List<String> getSupportedExtensions() {
        return List.of("kt", "kts");
    }

    @Override
    protected List<SecurityRule> getRules() {
        return RULES;
    }
}
