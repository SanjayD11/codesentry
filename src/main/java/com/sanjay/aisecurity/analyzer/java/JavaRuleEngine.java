package com.sanjay.aisecurity.analyzer.java;

import com.sanjay.aisecurity.analyzer.BaseRuleEngine;
import com.sanjay.aisecurity.analyzer.SecurityRule;
import com.sanjay.aisecurity.enums.Severity;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Static Analysis Engine for Java.
 * Uses SecurityRules and contextual checks to detect common security flaws.
 */
@Component
public class JavaRuleEngine extends BaseRuleEngine {

    private static final List<SecurityRule> RULES = new ArrayList<>();

    static {
        RULES.add(SecurityRule.builder()
                .id("JAVA-001")
                .name("OS Command Injection")
                .description("Usage of Runtime.exec() or ProcessBuilder with untrusted data can lead to OS command injection.")
                .recommendation("Use parameterized APIs and validate input strictly. Avoid passing user input to OS shells.")
                .severity(Severity.CRITICAL)
                .baseConfidence(0.9)
                .owaspCategory("A03:2021-Injection")
                .cweId("CWE-78")
                .detectionType("HYBRID")
                .reference("https://owasp.org/www-community/attacks/Command_Injection")
                .pattern(Pattern.compile("(Runtime\\.getRuntime\\(\\)\\.exec\\s*\\(|new\\s+ProcessBuilder\\s*\\()"))
                .build());

        RULES.add(SecurityRule.builder()
                .id("JAVA-002")
                .name("SQL Injection")
                .description("Potential SQL Injection via unparameterized query execution.")
                .recommendation("Always use PreparedStatement instead of Statement for parameterized SQL.")
                .severity(Severity.HIGH)
                .baseConfidence(0.85)
                .owaspCategory("A03:2021-Injection")
                .cweId("CWE-89")
                .detectionType("CONTEXT")
                .reference("https://owasp.org/www-community/attacks/SQL_Injection")
                .pattern(Pattern.compile("(executeQuery\\s*\\(|executeUpdate\\s*\\(|execute\\s*\\()"))
                .build());

        RULES.add(SecurityRule.builder()
                .id("JAVA-003")
                .name("Insecure Deserialization")
                .description("Use of ObjectInputStream can lead to arbitrary code execution.")
                .recommendation("Avoid native Java serialization. Use secure formats like JSON.")
                .severity(Severity.HIGH)
                .baseConfidence(0.95)
                .owaspCategory("A08:2021-Software and Data Integrity Failures")
                .cweId("CWE-502")
                .detectionType("REGEX")
                .reference("https://owasp.org/www-project-top-ten/2017/A8_2017-Insecure_Deserialization")
                .pattern(Pattern.compile("new\\s+ObjectInputStream\\s*\\("))
                .build());

        RULES.add(SecurityRule.builder()
                .id("JAVA-004")
                .name("Weak Cryptography")
                .description("Usage of weak hashing algorithm (MD5/SHA1) or encryption mode (DES/ECB).")
                .recommendation("Use SHA-256/SHA-512 for crypto, bcrypt/Argon2 for passwords, and AES/GCM for encryption.")
                .severity(Severity.MEDIUM)
                .baseConfidence(1.0)
                .owaspCategory("A02:2021-Cryptographic Failures")
                .cweId("CWE-327")
                .detectionType("REGEX")
                .reference("https://owasp.org/www-community/vulnerabilities/Insecure_Cryptographic_Storage")
                .pattern(Pattern.compile("MessageDigest\\.getInstance\\s*\\(\\s*\"(MD5|SHA-1)\"\\s*\\)|Cipher\\.getInstance\\s*\\(\\s*\"(DES|AES/ECB/PKCS5Padding)\"\\s*\\)"))
                .build());

        RULES.add(SecurityRule.builder()
                .id("JAVA-005")
                .name("Hardcoded Credentials")
                .description("Potential hardcoded password/secret detected.")
                .recommendation("Use environment variables, HashiCorp Vault, or AWS Secrets Manager.")
                .severity(Severity.HIGH)
                .baseConfidence(0.7)
                .owaspCategory("A07:2021-Identification and Authentication Failures")
                .cweId("CWE-798")
                .detectionType("REGEX")
                .reference("https://cwe.mitre.org/data/definitions/798.html")
                .pattern(Pattern.compile("(?i)(password|passwd|pwd|secret|api_key|token)\\s*=\\s*\"[^\"]{3,}\""))
                .build());

        RULES.add(SecurityRule.builder()
                .id("JAVA-006")
                .name("Information Disclosure")
                .description("Calling printStackTrace() or printing sensitive info exposes internal application details.")
                .recommendation("Use a logging framework (e.g., SLF4J, Logback) and log at ERROR level instead.")
                .severity(Severity.LOW)
                .baseConfidence(0.8)
                .owaspCategory("A05:2021-Security Misconfiguration")
                .cweId("CWE-209")
                .detectionType("REGEX")
                .reference("https://cwe.mitre.org/data/definitions/209.html")
                .pattern(Pattern.compile("\\.printStackTrace\\(\\s*\\)|System\\.out\\.print.*(?i)(password|secret|key|token|path|file|exception|error)"))
                .build());
                
        RULES.add(SecurityRule.builder()
                .id("JAVA-007")
                .name("Weak Randomness")
                .description("java.util.Random produces predictable values.")
                .recommendation("Use java.security.SecureRandom for cryptographic or security-sensitive RNG.")
                .severity(Severity.LOW)
                .baseConfidence(0.9)
                .owaspCategory("A02:2021-Cryptographic Failures")
                .cweId("CWE-330")
                .detectionType("REGEX")
                .reference("https://cwe.mitre.org/data/definitions/330.html")
                .pattern(Pattern.compile("new\\s+java\\.util\\.Random\\(\\s*\\)"))
                .build());

        RULES.add(SecurityRule.builder()
                .id("JAVA-008")
                .name("XML External Entity (XXE)")
                .description("Insecure XML parser configuration can lead to XXE.")
                .recommendation("Disable DOCTYPE processing or disallow external entities on DocumentBuilderFactory.")
                .severity(Severity.HIGH)
                .baseConfidence(0.85)
                .owaspCategory("A05:2021-Security Misconfiguration")
                .cweId("CWE-611")
                .detectionType("CONTEXT")
                .reference("https://owasp.org/www-community/vulnerabilities/XML_External_Entity_(XXE)_Processing")
                .pattern(Pattern.compile("DocumentBuilderFactory\\.newInstance\\(\\s*\\)|XMLReaderFactory\\.createXMLReader\\(\\s*\\)"))
                .build());
                
        RULES.add(SecurityRule.builder()
                .id("JAVA-009")
                .name("Path Traversal")
                .description("Unvalidated input passed to File constructors may allow path traversal.")
                .recommendation("Validate file paths, restrict to expected directories, or use Path.normalize().")
                .severity(Severity.HIGH)
                .baseConfidence(0.6)
                .owaspCategory("A01:2021-Broken Access Control")
                .cweId("CWE-22")
                .detectionType("CONTEXT")
                .reference("https://owasp.org/www-community/attacks/Path_Traversal")
                .pattern(Pattern.compile("new\\s+java\\.io\\.File\\s*\\(|Paths\\.get\\s*\\("))
                .build());

        RULES.add(SecurityRule.builder()
                .id("JAVA-010")
                .name("Server-Side Request Forgery (SSRF)")
                .description("Constructing a URL with user input can lead to SSRF.")
                .recommendation("Validate URLs against a strict allowlist. Do not allow users to specify arbitrary hosts.")
                .severity(Severity.HIGH)
                .baseConfidence(0.7)
                .owaspCategory("A10:2021-Server-Side Request Forgery")
                .cweId("CWE-918")
                .detectionType("CONTEXT")
                .reference("https://owasp.org/www-community/attacks/Server_Side_Request_Forgery")
                .pattern(Pattern.compile("new\\s+java\\.net\\.URL\\s*\\(|\\.openConnection\\(\\s*\\)"))
                .build());

        RULES.add(SecurityRule.builder()
                .id("JAVA-011")
                .name("Missing Secure Cookie")
                .description("Cookie is created without setting the Secure flag.")
                .recommendation("Always set cookie.setSecure(true) and cookie.setHttpOnly(true).")
                .severity(Severity.MEDIUM)
                .baseConfidence(0.8)
                .owaspCategory("A05:2021-Security Misconfiguration")
                .cweId("CWE-614")
                .detectionType("REGEX")
                .reference("https://owasp.org/www-community/controls/SecureCookieAttribute")
                .pattern(Pattern.compile("new\\s+Cookie\\s*\\(|\\.setSecure\\(false\\)"))
                .build());

        RULES.add(SecurityRule.builder()
                .id("JAVA-012")
                .name("LDAP Injection")
                .description("Unsanitized input in LDAP search query can lead to LDAP Injection.")
                .recommendation("Use parameterized queries for LDAP or properly escape LDAP specific characters.")
                .severity(Severity.HIGH)
                .baseConfidence(0.85)
                .owaspCategory("A03:2021-Injection")
                .cweId("CWE-90")
                .detectionType("CONTEXT")
                .reference("https://owasp.org/www-community/attacks/LDAP_Injection")
                .pattern(Pattern.compile("\\.search\\(|\\.lookup\\("))
                .build());

        RULES.add(SecurityRule.builder()
                .id("JAVA-013")
                .name("Unsafe Reflection")
                .description("Instantiating classes by name via reflection can lead to RCE.")
                .recommendation("Avoid reflection with user-supplied data or validate against a strict whitelist.")
                .severity(Severity.HIGH)
                .baseConfidence(0.75)
                .owaspCategory("A03:2021-Injection")
                .cweId("CWE-470")
                .detectionType("HYBRID")
                .reference("https://owasp.org/www-project-web-security-testing-guide/latest/4-Web_Application_Security_Testing/07-Input_Validation_Testing/05-Testing_for_SQL_Injection")
                .pattern(Pattern.compile("Class\\.forName\\s*\\("))
                .build());

        // ── JAVA-014: Cross-Site Scripting (XSS) ────────────────────────────
        RULES.add(SecurityRule.builder()
                .id("JAVA-014")
                .name("Cross-Site Scripting (XSS)")
                .description("User-controlled data written directly to HTTP response without encoding leads to XSS.")
                .recommendation("HTML-encode all user input before writing to response. Use OWASP Java Encoder or Thymeleaf th:text.")
                .severity(Severity.HIGH)
                .baseConfidence(0.80)
                .owaspCategory("A03:2021-Injection")
                .cweId("CWE-79")
                .detectionType("HYBRID")
                .reference("https://owasp.org/www-community/attacks/xss/")
                .pattern(Pattern.compile(
                        "(?:getWriter|PrintWriter).*\\bprintln?\\s*\\(|" +
                        "return\\s+\"<[a-zA-Z].*\\+"))
                .build());

        // ── JAVA-015: Open Redirect ──────────────────────────────────────────
        RULES.add(SecurityRule.builder()
                .id("JAVA-015")
                .name("Open Redirect")
                .description("Redirect destination built from user input enables phishing via unvalidated redirect.")
                .recommendation("Use an allowlist of permitted redirect targets; never redirect to a user-supplied URL.")
                .severity(Severity.MEDIUM)
                .baseConfidence(0.75)
                .owaspCategory("A01:2021-Broken Access Control")
                .cweId("CWE-601")
                .detectionType("HYBRID")
                .reference("https://cheatsheetseries.owasp.org/cheatsheets/Unvalidated_Redirects_and_Forwards_Cheat_Sheet.html")
                .pattern(Pattern.compile(
                        "return\\s+\"redirect:\\s*\"\\s*\\+|" +
                        "sendRedirect\\s*\\(|" +
                        "new\\s+RedirectView\\s*\\("))
                .build());

        // ── JAVA-016: Sensitive Data in Logs ────────────────────────────────
        RULES.add(SecurityRule.builder()
                .id("JAVA-016")
                .name("Sensitive Data in Logs")
                .description("Logging sensitive fields (password, token, secret) exposes credentials in log files.")
                .recommendation("Mask or redact sensitive values before logging. Never log passwords, tokens, or API keys.")
                .severity(Severity.MEDIUM)
                .baseConfidence(0.70)
                .owaspCategory("A09:2021-Security Logging and Monitoring Failures")
                .cweId("CWE-532")
                .detectionType("CONTEXT")
                .reference("https://cheatsheetseries.owasp.org/cheatsheets/Logging_Cheat_Sheet.html")
                .pattern(Pattern.compile(
                        "(?:log\\.|logger\\.|Logger\\.|System\\.out\\.|System\\.err\\.)" +
                        "(?:info|debug|warn|error|trace|println?)\\s*\\("))
                .build());

        // ── JAVA-017: Weak JWT Secret ────────────────────────────────────────
        RULES.add(SecurityRule.builder()
                .id("JAVA-017")
                .name("Weak JWT Secret")
                .description("Short or hardcoded JWT signing secret makes tokens trivially forgeable.")
                .recommendation("Use a cryptographically random secret of at least 256 bits stored in environment variables.")
                .severity(Severity.HIGH)
                .baseConfidence(0.85)
                .owaspCategory("A02:2021-Cryptographic Failures")
                .cweId("CWE-330")
                .detectionType("REGEX")
                .reference("https://owasp.org/www-project-json-web-token/")
                .pattern(Pattern.compile(
                        "(?i)(?:jwtSecret|jwt_secret|signingKey)\\s*=\\s*\"[^\"]{1,31}\""))
                .build());

        // ── JAVA-018: Weak CORS Configuration ───────────────────────────────
        RULES.add(SecurityRule.builder()
                .id("JAVA-018")
                .name("Weak CORS Configuration")
                .description("Wildcard CORS origin (*) allows any external site to make cross-origin requests.")
                .recommendation("Replace wildcard origins with an explicit allowlist of trusted domains.")
                .severity(Severity.MEDIUM)
                .baseConfidence(0.90)
                .owaspCategory("A05:2021-Security Misconfiguration")
                .cweId("CWE-942")
                .detectionType("REGEX")
                .reference("https://owasp.org/www-community/attacks/CORS_OriginHeaderScrutiny")
                .pattern(Pattern.compile(
                        "\\.allowedOrigins\\s*\\(\\s*\"\\*\"\\s*\\)|" +
                        "AllowedOrigin\\s*\\(\\s*\"\\*\"\\s*\\)"))
                .build());

        // ── JAVA-019: Unsafe File Upload ─────────────────────────────────────
        RULES.add(SecurityRule.builder()
                .id("JAVA-019")
                .name("Unsafe File Upload")
                .description("File saved using original client-supplied filename without normalization enables path traversal.")
                .recommendation("Sanitize filenames, validate MIME type and extension, store under server-generated names.")
                .severity(Severity.HIGH)
                .baseConfidence(0.70)
                .owaspCategory("A04:2021-Insecure Design")
                .cweId("CWE-434")
                .detectionType("CONTEXT")
                .reference("https://owasp.org/www-community/vulnerabilities/Unrestricted_File_Upload")
                .pattern(Pattern.compile(
                        "getOriginalFilename\\s*\\(\\s*\\)|" +
                        "transferTo\\s*\\(\\s*new\\s+(?:java\\.io\\.)?File"))
                .build());

        // ── JAVA-020: HTTP Response Splitting ───────────────────────────────
        RULES.add(SecurityRule.builder()
                .id("JAVA-020")
                .name("HTTP Response Splitting")
                .description("User-controlled values in HTTP response headers can inject additional headers or responses.")
                .recommendation("Strip CR (\\r) and LF (\\n) from any user input placed into response headers.")
                .severity(Severity.MEDIUM)
                .baseConfidence(0.75)
                .owaspCategory("A03:2021-Injection")
                .cweId("CWE-113")
                .detectionType("CONTEXT")
                .reference("https://owasp.org/www-community/attacks/HTTP_Response_Splitting")
                .pattern(Pattern.compile(
                        "response\\.(?:addHeader|setHeader)\\s*\\("))
                .build());

        // ── JAVA-021: Insecure Cookie Attributes ────────────────────────────
        RULES.add(SecurityRule.builder()
                .id("JAVA-021")
                .name("Insecure Cookie Attributes")
                .description("Cookie created without Secure and HttpOnly flags is vulnerable to theft and session hijacking.")
                .recommendation("Call setSecure(true) and setHttpOnly(true) on every Cookie before adding to response.")
                .severity(Severity.MEDIUM)
                .baseConfidence(0.75)
                .owaspCategory("A05:2021-Security Misconfiguration")
                .cweId("CWE-1004")
                .detectionType("CONTEXT")
                .reference("https://owasp.org/www-community/controls/SecureCookieAttribute")
                .pattern(Pattern.compile(
                        "new\\s+Cookie\\s*\\("))
                .build());

        // ── JAVA-022: Insecure Random (unqualified new Random()) ─────────────
        RULES.add(SecurityRule.builder()
                .id("JAVA-022")
                .name("Insecure Random — Unqualified")
                .description("Unqualified new Random() produces cryptographically weak values unsuitable for security tokens.")
                .recommendation("Use java.security.SecureRandom for tokens, session IDs, and security-sensitive values.")
                .severity(Severity.LOW)
                .baseConfidence(0.75)
                .owaspCategory("A02:2021-Cryptographic Failures")
                .cweId("CWE-330")
                .detectionType("REGEX")
                .reference("https://cwe.mitre.org/data/definitions/330.html")
                .pattern(Pattern.compile("(?<![a-zA-Z.])new\\s+Random\\s*\\(\\s*\\)"))
                .build());

        // ── JAVA-023: Debug / Development Config in Code ─────────────────────
        RULES.add(SecurityRule.builder()
                .id("JAVA-023")
                .name("Debug Configuration Enabled")
                .description("Debug flag enabled in source code may expose internal details in production environments.")
                .recommendation("Remove debug flags before production. Use Spring profiles to isolate development configuration.")
                .severity(Severity.LOW)
                .baseConfidence(0.65)
                .owaspCategory("A05:2021-Security Misconfiguration")
                .cweId("CWE-489")
                .detectionType("REGEX")
                .reference("https://cwe.mitre.org/data/definitions/489.html")
                .pattern(Pattern.compile(
                        "\\.setDebug\\s*\\(\\s*true\\s*\\)|" +
                        "System\\.setProperty\\s*\\([^)]*debug[^)]*true"))
                .build());
    }

    @Override
    protected List<SecurityRule> getRules() {
        return RULES;
    }

    @Override
    protected boolean passesContextualCheck(SecurityRule rule, String line, String[] allLines, int lineIndex) {
        if ("SQL Injection".equals(rule.getName())) {
            // Check current line and up to 5 previous lines for string concatenation (+)
            int start = Math.max(0, lineIndex - 5);
            for (int i = lineIndex; i >= start; i--) {
                if (allLines[i].contains("+")) {
                    return true;
                }
            }
            // If it's executing a hardcoded string directly, it's safe.
            if (line.matches(".*execute(Query|Update)?\\s*\\(\\s*\"[^\"]+\"\\s*\\).*")) {
                return false;
            }
            // Otherwise, we flag it because it's executing a variable that might be unparameterized
            return true;
        }
        
        if ("XML External Entity (XXE)".equals(rule.getName())) {
            // Check if subsequent lines configure the factory securely
            int limit = Math.min(lineIndex + 5, allLines.length);
            for (int i = lineIndex; i < limit; i++) {
                if (allLines[i].contains("setFeature") && allLines[i].contains("disallow-doctype-decl")) {
                    return false; // Secured, false positive
                }
            }
            return true;
        }

        if ("Path Traversal".equals(rule.getName())) {
            // If the file path is a hardcoded literal, it's safe
            if (line.matches(".*new\\s+java\\.io\\.File\\s*\\(\\s*\"[^\"]+\"\\s*\\).*")) {
                return false;
            }
            return true;
        }

        if ("OS Command Injection".equals(rule.getName())) {
            // If hardcoded string (no variables), it's safe
            if (line.matches(".*Runtime\\.getRuntime\\(\\)\\.exec\\s*\\(\\s*\"[^\"]+\"\\s*\\).*")) {
                return false;
            }
            return true;
        }

        if ("Server-Side Request Forgery (SSRF)".equals(rule.getName())) {
            // If hardcoded URL string, it's safe
            if (line.matches(".*new\\s+java\\.net\\.URL\\s*\\(\\s*\"[^\"]+\"\\s*\\).*")) {
                return false;
            }
            return true;
        }

        if ("LDAP Injection".equals(rule.getName())) {
            return line.contains("DirContext") || line.contains("ctx.search");
        }

        // XSS: only flag when there is concatenation (+) suggesting user input is injected
        if ("Cross-Site Scripting (XSS)".equals(rule.getName())) {
            return line.contains("+");
        }

        // Open Redirect: sendRedirect is only dangerous with a variable argument (not a literal)
        if ("Open Redirect".equals(rule.getName())) {
            if (line.matches(".*sendRedirect\\s*\\(\\s*\"[^\"]+\"\\s*\\).*")) return false;
            return true;
        }

        // Sensitive Data in Logs: scan next 3 lines for sensitive keywords being logged
        if ("Sensitive Data in Logs".equals(rule.getName())) {
            int limit = Math.min(lineIndex + 3, allLines.length);
            for (int j = lineIndex; j < limit; j++) {
                String ctx = allLines[j].toLowerCase();
                if (ctx.matches(".*\\b(?:password|passwd|secret|token|jwt|apikey|api_key|authorization|cookie|sessionid)\\b.*")) {
                    return true;
                }
            }
            return false;
        }

        // Unsafe File Upload: getOriginalFilename is only dangerous when the result is used in a File path
        if ("Unsafe File Upload".equals(rule.getName())) {
            if (line.contains("getOriginalFilename")) {
                // Check next 10 lines for File or Path usage with the filename
                int limit = Math.min(lineIndex + 10, allLines.length);
                for (int j = lineIndex; j < limit; j++) {
                    String ctx = allLines[j];
                    if (ctx.contains("new File") || ctx.contains("Paths.get") || ctx.contains("transferTo")) {
                        return true;
                    }
                }
                return false;
            }
            return true; // transferTo with new File always flagged
        }

        // HTTP Response Splitting: only flag when argument contains a variable (not a string literal)
        if ("HTTP Response Splitting".equals(rule.getName())) {
            return !line.matches(".*response\\.(?:addHeader|setHeader)\\s*\\(\\s*\"[^\"]+\"\\s*,\\s*\"[^\"]+\"\\s*\\).*");
        }

        // Insecure Cookie: only report if setHttpOnly(true) or setSecure(true) NOT found nearby
        if ("Insecure Cookie Attributes".equals(rule.getName())) {
            int limit = Math.min(lineIndex + 8, allLines.length);
            boolean hasHttpOnly = false, hasSecure = false;
            for (int j = lineIndex; j < limit; j++) {
                if (allLines[j].contains("setHttpOnly")) hasHttpOnly = true;
                if (allLines[j].contains("setSecure")) hasSecure = true;
            }
            return !(hasHttpOnly && hasSecure); // flag only when both are missing
        }

        return true;
    }
}
