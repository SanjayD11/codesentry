package com.sanjay.aisecurity.analyzer.python;

import com.sanjay.aisecurity.analyzer.BaseRuleEngine;
import com.sanjay.aisecurity.analyzer.SecurityRule;
import com.sanjay.aisecurity.enums.Severity;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

@Component
public class PythonRuleEngine extends BaseRuleEngine {

    private static final List<SecurityRule> RULES = new ArrayList<>();

    static {
        RULES.add(SecurityRule.builder()
                .id("PY-001")
                .name("OS Command Injection")
                .description("Usage of os.system() or subprocess with shell=True allows command injection.")
                .recommendation("Use subprocess.run() with a list of arguments and shell=False.")
                .severity(Severity.CRITICAL)
                .baseConfidence(0.95)
                .owaspCategory("A03:2021-Injection")
                .cweId("CWE-78")
                .detectionType("REGEX")
                .reference("https://owasp.org/www-community/attacks/Command_Injection")
                .pattern(Pattern.compile("(os\\.system\\(|subprocess\\..*\\(.*?shell\\s*=\\s*True)"))
                .build());

        RULES.add(SecurityRule.builder()
                .id("PY-002")
                .name("SQL Injection")
                .description("Potential SQL injection via string formatting or concatenation in execute().")
                .recommendation("Always use parameterized queries (e.g. execute(\"SELECT * FROM users WHERE id=?\", (id,)))")
                .severity(Severity.HIGH)
                .baseConfidence(0.85)
                .owaspCategory("A03:2021-Injection")
                .cweId("CWE-89")
                .detectionType("CONTEXT")
                .reference("https://owasp.org/www-community/attacks/SQL_Injection")
                .pattern(Pattern.compile("\\.execute\\s*\\("))
                .build());

        RULES.add(SecurityRule.builder()
                .id("PY-003")
                .name("Insecure Deserialization")
                .description("pickle.load() is unsafe and can execute arbitrary code.")
                .recommendation("Use safe formats like JSON. Never unpickle untrusted data.")
                .severity(Severity.CRITICAL)
                .baseConfidence(1.0)
                .owaspCategory("A08:2021-Software and Data Integrity Failures")
                .cweId("CWE-502")
                .detectionType("REGEX")
                .reference("https://docs.python.org/3/library/pickle.html")
                .pattern(Pattern.compile("pickle\\.load\\s*\\(|pickle\\.loads\\s*\\("))
                .build());

        RULES.add(SecurityRule.builder()
                .id("PY-004")
                .name("Arbitrary Code Execution")
                .description("Usage of eval() or exec() with untrusted data can lead to arbitrary code execution.")
                .recommendation("Avoid eval() and exec() completely. Use ast.literal_eval() if parsing data.")
                .severity(Severity.CRITICAL)
                .baseConfidence(0.9)
                .owaspCategory("A03:2021-Injection")
                .cweId("CWE-94")
                .detectionType("REGEX")
                .reference("https://cwe.mitre.org/data/definitions/94.html")
                .pattern(Pattern.compile("(eval\\s*\\(|exec\\s*\\()"))
                .build());

        RULES.add(SecurityRule.builder()
                .id("PY-005")
                .name("Weak Cryptography")
                .description("Usage of weak hashing algorithms like MD5 or SHA1.")
                .recommendation("Use hashlib.sha256() or hashlib.sha512(). Use bcrypt/argon2 for passwords.")
                .severity(Severity.MEDIUM)
                .baseConfidence(1.0)
                .owaspCategory("A02:2021-Cryptographic Failures")
                .cweId("CWE-327")
                .detectionType("REGEX")
                .reference("https://cwe.mitre.org/data/definitions/327.html")
                .pattern(Pattern.compile("hashlib\\.md5\\s*\\(|hashlib\\.sha1\\s*\\("))
                .build());

        RULES.add(SecurityRule.builder()
                .id("PY-006")
                .name("Hardcoded Credentials")
                .description("Potential hardcoded password, secret, or API key detected.")
                .recommendation("Use environment variables (os.getenv) or a secrets manager.")
                .severity(Severity.HIGH)
                .baseConfidence(0.7)
                .owaspCategory("A07:2021-Identification and Authentication Failures")
                .cweId("CWE-798")
                .detectionType("REGEX")
                .reference("https://cwe.mitre.org/data/definitions/798.html")
                .pattern(Pattern.compile("(?i)(password|passwd|pwd|secret|api_key|token)\\s*=\\s*['\"][^'\"]{3,}['\"]"))
                .build());

        RULES.add(SecurityRule.builder()
                .id("PY-007")
                .name("Debug Mode Enabled")
                .description("Flask/Django debug mode enabled in production can leak sensitive internals.")
                .recommendation("Ensure debug=False in production configurations.")
                .severity(Severity.MEDIUM)
                .baseConfidence(0.9)
                .owaspCategory("A05:2021-Security Misconfiguration")
                .cweId("CWE-489")
                .detectionType("REGEX")
                .reference("https://cwe.mitre.org/data/definitions/489.html")
                .pattern(Pattern.compile("debug\\s*=\\s*True"))
                .build());

        RULES.add(SecurityRule.builder()
                .id("PY-008")
                .name("Server-Side Request Forgery (SSRF)")
                .description("Unvalidated input passed to requests module can lead to SSRF.")
                .recommendation("Validate URLs against an allowlist and restrict host access.")
                .severity(Severity.HIGH)
                .baseConfidence(0.6)
                .owaspCategory("A10:2021-Server-Side Request Forgery")
                .cweId("CWE-918")
                .detectionType("CONTEXT")
                .reference("https://owasp.org/www-community/attacks/Server_Side_Request_Forgery")
                .pattern(Pattern.compile("requests\\.(get|post|put|delete)\\s*\\("))
                .build());

        RULES.add(SecurityRule.builder()
                .id("PY-009")
                .name("Information Disclosure")
                .description("Printing exceptions exposes internal logic and stack traces.")
                .recommendation("Use Python's logging module at the ERROR level instead of print.")
                .severity(Severity.LOW)
                .baseConfidence(0.8)
                .owaspCategory("A05:2021-Security Misconfiguration")
                .cweId("CWE-209")
                .detectionType("REGEX")
                .reference("https://cwe.mitre.org/data/definitions/209.html")
                .pattern(Pattern.compile("print\\s*\\(\\s*(e|ex|error|exception)\\s*\\)"))
                .build());

        RULES.add(SecurityRule.builder()
                .id("PY-010")
                .name("Weak Randomness")
                .description("The random module produces predictable values, unsuitable for crypto.")
                .recommendation("Use the secrets module (secrets.choice, secrets.randbelow) for security.")
                .severity(Severity.LOW)
                .baseConfidence(1.0)
                .owaspCategory("A02:2021-Cryptographic Failures")
                .cweId("CWE-330")
                .detectionType("REGEX")
                .reference("https://docs.python.org/3/library/secrets.html")
                .pattern(Pattern.compile("random\\.(randint|choice|random|randrange)\\s*\\("))
                .build());

        RULES.add(SecurityRule.builder()
                .id("PY-011")
                .name("Insecure Deserialization via yaml.load")
                .description("Using yaml.load() without SafeLoader can allow arbitrary code execution.")
                .recommendation("Use yaml.safe_load() instead of yaml.load().")
                .severity(Severity.CRITICAL)
                .baseConfidence(0.95)
                .owaspCategory("A08:2021-Software and Data Integrity Failures")
                .cweId("CWE-502")
                .detectionType("REGEX")
                .reference("https://github.com/yaml/pyyaml/wiki/PyYAML-yaml.load(input)-Deprecation")
                .pattern(Pattern.compile("yaml\\.load\\s*\\(\\s*[^,]+(?:(?!Loader).)*\\)"))
                .build());

        RULES.add(SecurityRule.builder()
                .id("PY-012")
                .name("Path Traversal")
                .description("Unsanitized input in file operations can lead to unauthorized file access.")
                .recommendation("Validate file paths using os.path.abspath and os.path.commonprefix.")
                .severity(Severity.HIGH)
                .baseConfidence(0.7)
                .owaspCategory("A01:2021-Broken Access Control")
                .cweId("CWE-22")
                .detectionType("REGEX")
                .reference("https://owasp.org/www-community/attacks/Path_Traversal")
                .pattern(Pattern.compile("open\\s*\\(\\s*[^'\"]*?\\+\\s*[^'\"]*?\\)"))
                .build());

        RULES.add(SecurityRule.builder()
                .id("PY-013")
                .name("Hardcoded JWT Token")
                .description("Hardcoding JWT tokens in source code leads to persistent authentication compromise.")
                .recommendation("Use environment variables to store API tokens and credentials.")
                .severity(Severity.HIGH)
                .baseConfidence(0.9)
                .owaspCategory("A07:2021-Identification and Authentication Failures")
                .cweId("CWE-798")
                .detectionType("REGEX")
                .reference("https://jwt.io/introduction")
                .pattern(Pattern.compile("(?i)(jwt|token)\\s*=\\s*['\"]ey[A-Za-z0-9-_=]+\\.[A-Za-z0-9-_=]+\\.?[A-Za-z0-9-_.+/=]*['\"]"))
                .build());
    }

    @Override
    protected List<SecurityRule> getRules() {
        return RULES;
    }

    @Override
    protected boolean passesContextualCheck(SecurityRule rule, String line, String[] allLines, int lineIndex) {
        if ("SQL Injection".equals(rule.getName())) {
            // Flag if formatting (%, format(), f-string) is used in the execute call
            return line.contains("%") || line.contains(".format") || line.contains("f\"");
        }

        if ("Server-Side Request Forgery (SSRF)".equals(rule.getName())) {
            // If hardcoded URL string (starts with 'http' or "http"), it's safe
            if (line.matches(".*requests\\.(get|post|put|delete)\\s*\\(\\s*[\"']http.*")) {
                return false;
            }
            return true;
        }

        return true;
    }
}
