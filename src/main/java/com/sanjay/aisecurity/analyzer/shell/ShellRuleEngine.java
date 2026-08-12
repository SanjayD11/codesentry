package com.sanjay.aisecurity.analyzer.shell;

import com.sanjay.aisecurity.analyzer.AbstractScannerEngine;
import com.sanjay.aisecurity.analyzer.SecurityRule;
import com.sanjay.aisecurity.enums.Severity;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Static Analysis Engine for Shell Scripts (.sh, .bash, .zsh).
 *
 * <p>Detects shell-specific vulnerabilities such as unquoted variables in exec,
 * unsafe curl | bash patterns, and overly permissive chmod commands.</p>
 *
 * @author Sanjay
 * @version 2.0.0
 */
@Component
public class ShellRuleEngine extends AbstractScannerEngine {

    private static final List<SecurityRule> RULES = new ArrayList<>();

    static {
        RULES.add(SecurityRule.builder()
                .id("SH-001")
                .name("Unquoted Variables in Eval/Exec")
                .description("Unquoted variables passed to eval or exec can lead to command injection.")
                .recommendation("Always quote variables (e.g., \"$VAR\") when passing them to commands.")
                .severity(Severity.HIGH)
                .baseConfidence(0.85)
                .owaspCategory("A03:2021-Injection")
                .cweId("CWE-78")
                .detectionType("REGEX")
                .reference("https://cwe.mitre.org/data/definitions/78.html")
                .language("SHELL")
                .subcategory("Injection")
                .pattern(Pattern.compile("(?:eval|exec)\\s+.*\\$\\w+"))
                .build());

        RULES.add(SecurityRule.builder()
                .id("SH-002")
                .name("Unsafe curl | bash Pattern")
                .description("Piping curl or wget directly to bash is extremely dangerous and executes untrusted code.")
                .recommendation("Download the script, inspect it, and then execute it.")
                .severity(Severity.CRITICAL)
                .baseConfidence(0.95)
                .owaspCategory("A08:2021-Software and Data Integrity Failures")
                .cweId("CWE-494")
                .detectionType("REGEX")
                .reference("https://cwe.mitre.org/data/definitions/494.html")
                .language("SHELL")
                .subcategory("Integrity")
                .pattern(Pattern.compile("(?:curl|wget).*\\|\\s*(?:bash|sh|zsh)"))
                .build());

        RULES.add(SecurityRule.builder()
                .id("SH-003")
                .name("Overly Permissive chmod (777)")
                .description("Setting file permissions to 777 grants read, write, and execute access to everyone.")
                .recommendation("Use the principle of least privilege (e.g., chmod 755 or 644).")
                .severity(Severity.HIGH)
                .baseConfidence(0.9)
                .owaspCategory("A01:2021-Broken Access Control")
                .cweId("CWE-732")
                .detectionType("REGEX")
                .reference("https://cwe.mitre.org/data/definitions/732.html")
                .language("SHELL")
                .subcategory("Access Control")
                .pattern(Pattern.compile("chmod\\s+(?:-R\\s+)?777\\b"))
                .build());

        RULES.add(SecurityRule.builder()
                .id("SH-004")
                .name("Hardcoded Credentials")
                .description("Potential hardcoded password or token in a shell script.")
                .recommendation("Use environment variables instead of hardcoding credentials.")
                .severity(Severity.HIGH)
                .baseConfidence(0.7)
                .owaspCategory("A07:2021-Identification and Authentication Failures")
                .cweId("CWE-798")
                .detectionType("REGEX")
                .reference("https://cwe.mitre.org/data/definitions/798.html")
                .language("SHELL")
                .subcategory("Authentication")
                .pattern(Pattern.compile("(?i)(?:PASSWORD|SECRET|API_?KEY|TOKEN)=[\"'][^\"']{3,}[\"']"))
                .build());
    }

    @Override
    public String getEngineId() {
        return "SHELL";
    }

    @Override
    public String getEngineName() {
        return "Shell Script Scanner";
    }

    @Override
    public List<String> getSupportedExtensions() {
        return List.of("sh", "bash", "zsh");
    }

    @Override
    protected List<SecurityRule> getRules() {
        return RULES;
    }
}
