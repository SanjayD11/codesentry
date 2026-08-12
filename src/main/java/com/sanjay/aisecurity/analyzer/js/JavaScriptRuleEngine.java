package com.sanjay.aisecurity.analyzer.js;

import com.sanjay.aisecurity.analyzer.BaseRuleEngine;
import com.sanjay.aisecurity.analyzer.SecurityRule;
import com.sanjay.aisecurity.enums.Severity;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

@Component
public class JavaScriptRuleEngine extends BaseRuleEngine {

    private static final List<SecurityRule> RULES = new ArrayList<>();

    static {
        RULES.add(SecurityRule.builder()
                .id("JS-001")
                .name("DOM Cross-Site Scripting (XSS)")
                .description("Use of dangerouslySetInnerHTML or innerHTML can lead to XSS if input is unescaped.")
                .recommendation("Avoid using innerHTML. Use textContent, or sanitize HTML using DOMPurify.")
                .severity(Severity.HIGH)
                .baseConfidence(0.9)
                .owaspCategory("A03:2021-Injection")
                .cweId("CWE-79")
                .detectionType("REGEX")
                .reference("https://owasp.org/www-community/attacks/xss/")
                .pattern(Pattern.compile("(dangerouslySetInnerHTML|\\.innerHTML\\s*=|document\\.write\\s*\\()"))
                .build());

        RULES.add(SecurityRule.builder()
                .id("JS-002")
                .name("Arbitrary Code Execution (Eval)")
                .description("Usage of eval() or new Function() allows execution of arbitrary code.")
                .recommendation("Never use eval(). Parse JSON securely with JSON.parse().")
                .severity(Severity.CRITICAL)
                .baseConfidence(1.0)
                .owaspCategory("A03:2021-Injection")
                .cweId("CWE-94")
                .detectionType("REGEX")
                .reference("https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/eval#never_use_eval!")
                .pattern(Pattern.compile("(eval\\s*\\(|new\\s+Function\\s*\\()"))
                .build());

        RULES.add(SecurityRule.builder()
                .id("JS-003")
                .name("OS Command Injection")
                .description("Usage of child_process.exec() without sanitization can lead to command injection.")
                .recommendation("Use child_process.execFile() or spawn() with an array of arguments.")
                .severity(Severity.CRITICAL)
                .baseConfidence(0.9)
                .owaspCategory("A03:2021-Injection")
                .cweId("CWE-78")
                .detectionType("REGEX")
                .reference("https://owasp.org/www-community/attacks/Command_Injection")
                .pattern(Pattern.compile("(child_process\\.exec\\s*\\(|exec\\s*\\()"))
                .build());

        RULES.add(SecurityRule.builder()
                .id("JS-004")
                .name("SQL Injection")
                .description("Potential SQL Injection in Node.js database drivers.")
                .recommendation("Always use parameterized queries or an ORM like Prisma / TypeORM.")
                .severity(Severity.HIGH)
                .baseConfidence(0.8)
                .owaspCategory("A03:2021-Injection")
                .cweId("CWE-89")
                .detectionType("CONTEXT")
                .reference("https://owasp.org/www-community/attacks/SQL_Injection")
                .pattern(Pattern.compile("(\\.query\\s*\\(|\\.execute\\s*\\()"))
                .build());

        RULES.add(SecurityRule.builder()
                .id("JS-005")
                .name("Insecure Cookies")
                .description("Cookies are being set without the Secure or HttpOnly flags.")
                .recommendation("Ensure secure: true and httpOnly: true are set in cookie options.")
                .severity(Severity.MEDIUM)
                .baseConfidence(0.85)
                .owaspCategory("A05:2021-Security Misconfiguration")
                .cweId("CWE-614")
                .detectionType("REGEX")
                .reference("https://developer.mozilla.org/en-US/docs/Web/HTTP/Cookies#security")
                .pattern(Pattern.compile("secure\\s*:\\s*false|httpOnly\\s*:\\s*false"))
                .build());

        RULES.add(SecurityRule.builder()
                .id("JS-006")
                .name("Weak JWT Secrets / Hardcoded Tokens")
                .description("Hardcoded secret key used for JWT signing/verification.")
                .recommendation("Store JWT secrets securely in environment variables (e.g. process.env.JWT_SECRET).")
                .severity(Severity.HIGH)
                .baseConfidence(0.9)
                .owaspCategory("A02:2021-Cryptographic Failures")
                .cweId("CWE-798")
                .detectionType("CONTEXT")
                .reference("https://owasp.org/www-project-json-web-token/")
                .pattern(Pattern.compile("jwt\\.(sign|verify)\\s*\\("))
                .build());

        RULES.add(SecurityRule.builder()
                .id("JS-007")
                .name("Server-Side Request Forgery (SSRF)")
                .description("Unvalidated input passed to fetch or axios can lead to SSRF.")
                .recommendation("Validate URLs against a strict allowlist. Do not allow users to specify arbitrary hosts.")
                .severity(Severity.HIGH)
                .baseConfidence(0.7)
                .owaspCategory("A10:2021-Server-Side Request Forgery")
                .cweId("CWE-918")
                .detectionType("CONTEXT")
                .reference("https://owasp.org/www-community/attacks/Server_Side_Request_Forgery")
                .pattern(Pattern.compile("(fetch\\s*\\(|axios\\.(get|post|put|delete)\\s*\\()"))
                .build());

        RULES.add(SecurityRule.builder()
                .id("JS-008")
                .name("Prototype Pollution")
                .description("Unsafe merging or cloning of objects can lead to prototype pollution.")
                .recommendation("Validate JSON schema, freeze prototypes using Object.freeze(), or use Map instead of Object.")
                .severity(Severity.MEDIUM)
                .baseConfidence(0.6)
                .owaspCategory("A03:2021-Injection")
                .cweId("CWE-1321")
                .detectionType("REGEX")
                .reference("https://portswigger.net/web-security/prototype-pollution")
                .pattern(Pattern.compile("(__proto__|\\[\"__proto__\"\\]|\\.constructor\\.prototype)"))
                .build());
                
        RULES.add(SecurityRule.builder()
                .id("JS-009")
                .name("Hardcoded Credentials")
                .description("Potential hardcoded password/secret detected.")
                .recommendation("Use environment variables.")
                .severity(Severity.HIGH)
                .baseConfidence(0.7)
                .owaspCategory("A07:2021-Identification and Authentication Failures")
                .cweId("CWE-798")
                .detectionType("REGEX")
                .reference("https://cwe.mitre.org/data/definitions/798.html")
                .pattern(Pattern.compile("(?i)(password|passwd|pwd|secret|api_key|token)\\s*:\\s*['\"][^'\"]{3,}['\"]"))
                .build());

        RULES.add(SecurityRule.builder()
                .id("JS-010")
                .name("Path Traversal")
                .description("Unsanitized user input passed to fs operations can allow path traversal.")
                .recommendation("Sanitize input using path.basename(), and validate the final path using path.resolve() against a known safe directory.")
                .severity(Severity.HIGH)
                .baseConfidence(0.75)
                .owaspCategory("A01:2021-Broken Access Control")
                .cweId("CWE-22")
                .detectionType("REGEX")
                .reference("https://owasp.org/www-community/attacks/Path_Traversal")
                .pattern(Pattern.compile("fs\\.(readFile|readFileSync|createReadStream|writeFile|writeFileSync)\\s*\\(\\s*[^'\"]*?\\+\\s*[^'\"]*?\\)"))
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
            return line.contains("+") || line.contains("${");
        }

        if ("Weak JWT Secrets / Hardcoded Tokens".equals(rule.getName())) {
            // Flag if the secret argument is a hardcoded string
            return line.matches(".*jwt\\.(sign|verify)\\s*\\(.*?,\\s*[\"'][^\"']+[\"'].*");
        }
        
        if ("Server-Side Request Forgery (SSRF)".equals(rule.getName())) {
            // If hardcoded URL string (starts with 'http' or "http"), it's safe
            if (line.matches(".*(fetch\\s*\\(|axios\\.(get|post|put|delete)\\s*\\()\\s*[\"']http.*")) {
                return false;
            }
            return true;
        }

        return true;
    }
}
