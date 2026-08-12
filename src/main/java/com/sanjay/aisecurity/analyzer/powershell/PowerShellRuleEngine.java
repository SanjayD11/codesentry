package com.sanjay.aisecurity.analyzer.powershell;

import com.sanjay.aisecurity.analyzer.AbstractScannerEngine;
import com.sanjay.aisecurity.analyzer.SecurityRule;
import com.sanjay.aisecurity.enums.Severity;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Static Analysis Engine for PowerShell (.ps1).
 *
 * <p>Detects PowerShell-specific vulnerabilities such as Invoke-Expression
 * with variables, ExecutionPolicy bypass, and hardcoded credentials.</p>
 *
 * @author Sanjay
 * @version 2.0.0
 */
@Component
public class PowerShellRuleEngine extends AbstractScannerEngine {

    private static final List<SecurityRule> RULES = new ArrayList<>();

    static {
        RULES.add(SecurityRule.builder()
                .id("PS-001")
                .name("Unsafe Invoke-Expression")
                .description("Usage of Invoke-Expression (or iex) with untrusted variables allows arbitrary code execution.")
                .recommendation("Avoid Invoke-Expression. Use the call operator (&) or parameter binding instead.")
                .severity(Severity.CRITICAL)
                .baseConfidence(0.9)
                .owaspCategory("A03:2021-Injection")
                .cweId("CWE-94")
                .detectionType("REGEX")
                .reference("https://cwe.mitre.org/data/definitions/94.html")
                .language("POWERSHELL")
                .subcategory("Injection")
                .pattern(Pattern.compile("(?i)(?:Invoke-Expression|iex)\\s+.*\\$\\w+"))
                .build());

        RULES.add(SecurityRule.builder()
                .id("PS-002")
                .name("Execution Policy Bypass")
                .description("Bypassing the execution policy in a script can indicate malicious intent or insecure configuration.")
                .recommendation("Run scripts with the appropriate execution policy rather than bypassing it globally.")
                .severity(Severity.MEDIUM)
                .baseConfidence(0.95)
                .owaspCategory("A05:2021-Security Misconfiguration")
                .cweId("CWE-16")
                .detectionType("REGEX")
                .reference("https://cwe.mitre.org/data/definitions/16.html")
                .language("POWERSHELL")
                .subcategory("Configuration")
                .pattern(Pattern.compile("(?i)-ExecutionPolicy\\s+Bypass"))
                .build());

        RULES.add(SecurityRule.builder()
                .id("PS-003")
                .name("Insecure Web Request (Ignore Certs)")
                .description("Ignoring SSL/TLS certificates when making web requests allows Man-in-the-Middle (MitM) attacks.")
                .recommendation("Always validate SSL certificates. Do not bypass certificate checks.")
                .severity(Severity.HIGH)
                .baseConfidence(0.9)
                .owaspCategory("A02:2021-Cryptographic Failures")
                .cweId("CWE-295")
                .detectionType("REGEX")
                .reference("https://cwe.mitre.org/data/definitions/295.html")
                .language("POWERSHELL")
                .subcategory("Cryptography")
                .pattern(Pattern.compile("(?i)\\[System\\.Net\\.ServicePointManager\\]::ServerCertificateValidationCallback\\s*=\\s*\\{\\s*\\$true\\s*\\}"))
                .build());

        RULES.add(SecurityRule.builder()
                .id("PS-004")
                .name("Hardcoded Credentials")
                .description("Potential hardcoded password or token in a PowerShell script.")
                .recommendation("Use Get-Credential or a secure vault to handle credentials.")
                .severity(Severity.HIGH)
                .baseConfidence(0.7)
                .owaspCategory("A07:2021-Identification and Authentication Failures")
                .cweId("CWE-798")
                .detectionType("REGEX")
                .reference("https://cwe.mitre.org/data/definitions/798.html")
                .language("POWERSHELL")
                .subcategory("Authentication")
                .pattern(Pattern.compile("(?i)\\$(?:Password|Secret|ApiKey|Token)\\s*=\\s*['\"][^'\"]{3,}['\"]"))
                .build());
    }

    @Override
    public String getEngineId() {
        return "POWERSHELL";
    }

    @Override
    public String getEngineName() {
        return "PowerShell Scanner";
    }

    @Override
    public List<String> getSupportedExtensions() {
        return List.of("ps1");
    }

    @Override
    protected List<SecurityRule> getRules() {
        return RULES;
    }
}
