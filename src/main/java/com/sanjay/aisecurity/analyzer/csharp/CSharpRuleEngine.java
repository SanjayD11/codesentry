package com.sanjay.aisecurity.analyzer.csharp;

import com.sanjay.aisecurity.analyzer.BaseRuleEngine;
import com.sanjay.aisecurity.analyzer.SecurityRule;
import com.sanjay.aisecurity.enums.Severity;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

@Component
public class CSharpRuleEngine extends BaseRuleEngine {

    private static final List<SecurityRule> RULES = new ArrayList<>();

    static {
        RULES.add(SecurityRule.builder()
                .id("CS-001")
                .name("SQL Injection")
                .description("Unparameterized SQL query execution.")
                .recommendation("Use parameterized queries with SqlCommand.Parameters.AddWithValue().")
                .severity(Severity.HIGH)
                .baseConfidence(0.85)
                .owaspCategory("A03:2021-Injection")
                .cweId("CWE-89")
                .detectionType("CONTEXT")
                .reference("https://owasp.org/www-community/attacks/SQL_Injection")
                .pattern(Pattern.compile("(SqlCommand|OleDbCommand|OdbcCommand)\\s*\\("))
                .build());

        RULES.add(SecurityRule.builder()
                .id("CS-002")
                .name("OS Command Injection")
                .description("Usage of Process.Start() with untrusted data.")
                .recommendation("Avoid starting processes with user input or strictly validate the executable path.")
                .severity(Severity.CRITICAL)
                .baseConfidence(0.9)
                .owaspCategory("A03:2021-Injection")
                .cweId("CWE-78")
                .detectionType("REGEX")
                .reference("https://owasp.org/www-community/attacks/Command_Injection")
                .pattern(Pattern.compile("Process\\.Start\\s*\\("))
                .build());

        RULES.add(SecurityRule.builder()
                .id("CS-003")
                .name("XML External Entity (XXE)")
                .description("Insecure XML parser configuration.")
                .recommendation("Set XmlResolver = null on XmlDocument or XmlReaderSettings.")
                .severity(Severity.HIGH)
                .baseConfidence(0.85)
                .owaspCategory("A05:2021-Security Misconfiguration")
                .cweId("CWE-611")
                .detectionType("CONTEXT")
                .reference("https://owasp.org/www-community/vulnerabilities/XML_External_Entity_(XXE)_Processing")
                .pattern(Pattern.compile("new\\s+XmlDocument\\(\\s*\\)|new\\s+XmlTextReader\\(\\s*\\)"))
                .build());

        RULES.add(SecurityRule.builder()
                .id("CS-004")
                .name("Insecure Deserialization")
                .description("Usage of BinaryFormatter or insecure JSON settings allows RCE.")
                .recommendation("Do not use BinaryFormatter. Use System.Text.Json or Newtonsoft with TypeNameHandling.None.")
                .severity(Severity.CRITICAL)
                .baseConfidence(0.95)
                .owaspCategory("A08:2021-Software and Data Integrity Failures")
                .cweId("CWE-502")
                .detectionType("REGEX")
                .reference("https://learn.microsoft.com/en-us/dotnet/standard/serialization/binaryformatter-security-guide")
                .pattern(Pattern.compile("BinaryFormatter\\.Deserialize\\s*\\(|TypeNameHandling\\.All|TypeNameHandling\\.Auto"))
                .build());
                
        RULES.add(SecurityRule.builder()
                .id("CS-005")
                .name("Path Traversal")
                .description("Unvalidated input passed to File operations.")
                .recommendation("Validate file paths and restrict to expected directories.")
                .severity(Severity.HIGH)
                .baseConfidence(0.6)
                .owaspCategory("A01:2021-Broken Access Control")
                .cweId("CWE-22")
                .detectionType("CONTEXT")
                .reference("https://owasp.org/www-community/attacks/Path_Traversal")
                .pattern(Pattern.compile("File\\.(ReadAllText|ReadAllBytes|WriteAllText|Open|OpenRead)\\s*\\("))
                .build());
                
        RULES.add(SecurityRule.builder()
                .id("CS-006")
                .name("Hardcoded Credentials")
                .description("Potential hardcoded database/API secret detected.")
                .recommendation("Store secrets in environment variables or Azure Key Vault.")
                .severity(Severity.HIGH)
                .baseConfidence(0.7)
                .owaspCategory("A07:2021-Identification and Authentication Failures")
                .cweId("CWE-798")
                .detectionType("REGEX")
                .reference("https://cwe.mitre.org/data/definitions/798.html")
                .pattern(Pattern.compile("(?i)(password|passwd|pwd|secret|api_key)\\s*=\\s*[\"@][^\"]{3,}\""))
                .build());
    }

    @Override
    protected List<SecurityRule> getRules() {
        return RULES;
    }

    @Override
    protected boolean passesContextualCheck(SecurityRule rule, String line, String[] allLines, int lineIndex) {
        if ("SQL Injection".equals(rule.getName())) {
            return line.contains("+") || line.contains("String.Format");
        }
        if ("XML External Entity (XXE)".equals(rule.getName())) {
            // Check if subsequent lines configure the resolver securely
            int limit = Math.min(lineIndex + 5, allLines.length);
            for (int i = lineIndex; i < limit; i++) {
                if (allLines[i].contains("XmlResolver") && allLines[i].contains("null")) {
                    return false; // Secured, false positive
                }
            }
            return true;
        }
        return true;
    }
}
