package com.sanjay.aisecurity.analyzer.sql;

import com.sanjay.aisecurity.analyzer.AbstractScannerEngine;
import com.sanjay.aisecurity.analyzer.FileCategory;
import com.sanjay.aisecurity.analyzer.SecurityRule;
import com.sanjay.aisecurity.enums.Severity;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Static Analysis Engine for SQL / DDL / DML.
 *
 * <p>Detects dangerous SQL patterns, insecure database configurations,
 * privilege escalation, and unsafe dynamic execution.</p>
 *
 * @author Sanjay
 * @version 2.0.0
 */
@Component
public class SqlRuleEngine extends AbstractScannerEngine {

    private static final List<SecurityRule> RULES = new ArrayList<>();

    static {
        RULES.add(SecurityRule.builder()
                .id("SQL-001")
                .name("Unsafe Dynamic SQL (EXEC)")
                .description("Using EXEC() or EXECUTE IMMEDIATE with dynamic strings can lead to SQL Injection.")
                .recommendation("Use sp_executesql with parameters instead of EXEC().")
                .severity(Severity.HIGH)
                .baseConfidence(0.85)
                .owaspCategory("A03:2021-Injection")
                .cweId("CWE-89")
                .detectionType("REGEX")
                .reference("https://owasp.org/www-community/attacks/SQL_Injection")
                .language("SQL")
                .subcategory("Injection")
                .pattern(Pattern.compile("(?i)(EXEC|EXECUTE\\s+IMMEDIATE)\\s*\\("))
                .build());

        RULES.add(SecurityRule.builder()
                .id("SQL-002")
                .name("Missing WHERE Clause (Update/Delete)")
                .description("UPDATE or DELETE statement without a WHERE clause will modify the entire table.")
                .recommendation("Always include a WHERE clause for UPDATE/DELETE statements.")
                .severity(Severity.CRITICAL)
                .baseConfidence(0.9)
                .owaspCategory("A05:2021-Security Misconfiguration")
                .cweId("CWE-285")
                .detectionType("CONTEXT")
                .reference("https://cwe.mitre.org/data/definitions/285.html")
                .language("SQL")
                .subcategory("Configuration")
                .requiresContext(true)
                .pattern(Pattern.compile("(?i)^(UPDATE|DELETE\\s+FROM)\\s+\\w+"))
                .build());

        RULES.add(SecurityRule.builder()
                .id("SQL-003")
                .name("Grant All Privileges")
                .description("Granting ALL PRIVILEGES is extremely dangerous and violates the principle of least privilege.")
                .recommendation("Grant only the specific privileges required (e.g., SELECT, INSERT).")
                .severity(Severity.CRITICAL)
                .baseConfidence(0.95)
                .owaspCategory("A01:2021-Broken Access Control")
                .cweId("CWE-266")
                .detectionType("REGEX")
                .reference("https://cwe.mitre.org/data/definitions/266.html")
                .language("SQL")
                .subcategory("Access Control")
                .pattern(Pattern.compile("(?i)GRANT\\s+ALL\\s+PRIVILEGES\\s+ON"))
                .build());

        RULES.add(SecurityRule.builder()
                .id("SQL-004")
                .name("Use of sa or root account")
                .description("Referencing the default superuser accounts ('sa' or 'root') in scripts is a security risk.")
                .recommendation("Use a dedicated service account with restricted privileges.")
                .severity(Severity.HIGH)
                .baseConfidence(0.8)
                .owaspCategory("A01:2021-Broken Access Control")
                .cweId("CWE-250")
                .detectionType("REGEX")
                .reference("https://cwe.mitre.org/data/definitions/250.html")
                .language("SQL")
                .subcategory("Access Control")
                .pattern(Pattern.compile("(?i)\\b(TO|USER)\\s+['\"]?(sa|root)['\"]?\\b"))
                .build());

        RULES.add(SecurityRule.builder()
                .id("SQL-005")
                .name("Insecure Password Storage (Cleartext)")
                .description("Storing passwords in a column named password/pwd without hashing.")
                .recommendation("Ensure passwords are hashed before storage. Avoid cleartext inserts.")
                .severity(Severity.HIGH)
                .baseConfidence(0.75)
                .owaspCategory("A02:2021-Cryptographic Failures")
                .cweId("CWE-256")
                .detectionType("REGEX")
                .reference("https://cwe.mitre.org/data/definitions/256.html")
                .language("SQL")
                .subcategory("Cryptography")
                .pattern(Pattern.compile("(?i)INSERT\\s+INTO\\s+\\w+\\s*\\(.*password.*\\)\\s*VALUES\\s*\\(.*['\"][^'\"]+['\"].*\\)"))
                .build());

        RULES.add(SecurityRule.builder()
                .id("SQL-006")
                .name("Weak Cryptography (MD5/SHA1)")
                .description("Using weak hashing functions like MD5 or SHA1 in SQL.")
                .recommendation("Use SHA256 or stronger hashing algorithms available in the database.")
                .severity(Severity.MEDIUM)
                .baseConfidence(0.9)
                .owaspCategory("A02:2021-Cryptographic Failures")
                .cweId("CWE-327")
                .detectionType("REGEX")
                .reference("https://cwe.mitre.org/data/definitions/327.html")
                .language("SQL")
                .subcategory("Cryptography")
                .pattern(Pattern.compile("(?i)\\b(MD5|SHA1|SHA)\\s*\\("))
                .build());

        RULES.add(SecurityRule.builder()
                .id("SQL-007")
                .name("Cross-Database Queries")
                .description("Queries referencing other databases directly can lead to lateral movement if compromised.")
                .recommendation("Limit cross-database access; use views or linked servers with strict security.")
                .severity(Severity.LOW)
                .baseConfidence(0.6)
                .owaspCategory("A01:2021-Broken Access Control")
                .cweId("CWE-285")
                .detectionType("REGEX")
                .reference("https://cwe.mitre.org/data/definitions/285.html")
                .language("SQL")
                .subcategory("Access Control")
                .pattern(Pattern.compile("(?i)\\bFROM\\s+\\w+\\.\\w+\\.\\w+\\b")) // e.g., FROM DB.Schema.Table
                .build());

        RULES.add(SecurityRule.builder()
                .id("SQL-008")
                .name("Use of xp_cmdshell")
                .description("xp_cmdshell allows execution of OS commands from SQL Server. This is highly dangerous.")
                .recommendation("Disable xp_cmdshell entirely.")
                .severity(Severity.CRITICAL)
                .baseConfidence(0.95)
                .owaspCategory("A03:2021-Injection")
                .cweId("CWE-78")
                .detectionType("REGEX")
                .reference("https://cwe.mitre.org/data/definitions/78.html")
                .language("SQL")
                .subcategory("Injection")
                .pattern(Pattern.compile("(?i)\\bxp_cmdshell\\b"))
                .build());
    }

    @Override
    public FileCategory getCategory() {
        return FileCategory.SQL;
    }

    @Override
    public String getEngineId() {
        return "SQL";
    }

    @Override
    public String getEngineName() {
        return "SQL Rules Engine";
    }

    @Override
    public List<String> getSupportedExtensions() {
        return List.of("sql", "ddl", "dml");
    }

    @Override
    protected List<SecurityRule> getRules() {
        return RULES;
    }

    @Override
    protected boolean passesContextualCheck(SecurityRule rule, String line, String[] allLines, int lineIndex) {
        if ("Missing WHERE Clause (Update/Delete)".equals(rule.getName())) {
            // Check if line or subsequent line contains WHERE
            String statement = line.toUpperCase();
            if (statement.contains("WHERE")) {
                return false; // Has where clause
            }
            int limit = Math.min(lineIndex + 5, allLines.length);
            for (int i = lineIndex + 1; i < limit; i++) {
                if (allLines[i].toUpperCase().contains("WHERE")) {
                    return false;
                }
                if (allLines[i].contains(";")) {
                    break;
                }
            }
            return true;
        }
        return true;
    }
}
