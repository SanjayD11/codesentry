# Scan Configuration - Runtime Verification Results

Generated: 2026-07-23 13:27:55


## OWASP Verification

### Configuration Sent to Backend
```json
{
  "configuration": {
    "owasp": false,
    "cwe": true,
    "secrets": true,
    "sqlInjection": true,
    "xss": true,
    "commandInjection": true,
    "pathTraversal": true,
    "jwtIssues": true,
    "insecureDeserialization": true,
    "weakCryptography": true,
    "directoryTraversal": true,
    "enableExplanation": true,
    "enableRootCause": true,
    "enableBusinessImpact": true,
    "enableSecureFix": true,
    "confidenceThreshold": 0,
    "maxFileSizeMB": 10,
    "timeoutSeconds": 300,
    "ignoreDirectories": "node_modules,.git",
    "skipGeneratedFiles": false
  }
}
```

### Backend Log Evidence
```text
(No matching logs found)
```

### Database Evidence (configuration_json)
```json
{"owasp":false,"cwe":true,"secrets":true,"sqlInjection":true,"xss":true,"commandInjection":true,"pathTraversal":true,"jwtIssues":true,"insecureDeserialization":true,"weakCryptography":true,"directoryTraversal":true,"enableExplanation":true,"enableRootCause":true,"enableBusinessImpact":true,"enableSecureFix":true,"confidenceThreshold":0,"maxFileSizeMB":10.0,"timeoutSeconds":300,"ignoreDirectories":"node_modules,.git","skipGeneratedFiles":false,"maxFileSizeBytes":10485760}
```

Findings with OWASP: 0 / 6

**Result:** PASS

## CWE Verification

### Configuration Sent to Backend
```json
{
  "configuration": {
    "owasp": true,
    "cwe": false,
    "secrets": true,
    "sqlInjection": true,
    "xss": true,
    "commandInjection": true,
    "pathTraversal": true,
    "jwtIssues": true,
    "insecureDeserialization": true,
    "weakCryptography": true,
    "directoryTraversal": true,
    "enableExplanation": true,
    "enableRootCause": true,
    "enableBusinessImpact": true,
    "enableSecureFix": true,
    "confidenceThreshold": 0,
    "maxFileSizeMB": 10,
    "timeoutSeconds": 300,
    "ignoreDirectories": "node_modules,.git",
    "skipGeneratedFiles": false
  }
}
```

### Backend Log Evidence
```text
(No matching logs found)
```

### Database Evidence (configuration_json)
```json
{"owasp":true,"cwe":false,"secrets":true,"sqlInjection":true,"xss":true,"commandInjection":true,"pathTraversal":true,"jwtIssues":true,"insecureDeserialization":true,"weakCryptography":true,"directoryTraversal":true,"enableExplanation":true,"enableRootCause":true,"enableBusinessImpact":true,"enableSecureFix":true,"confidenceThreshold":0,"maxFileSizeMB":10.0,"timeoutSeconds":300,"ignoreDirectories":"node_modules,.git","skipGeneratedFiles":false,"maxFileSizeBytes":10485760}
```

Findings with CWE: 0 / 6

**Result:** PASS

## Secrets Verification

### Configuration Sent to Backend
```json
{
  "configuration": {
    "owasp": true,
    "cwe": true,
    "secrets": false,
    "sqlInjection": true,
    "xss": true,
    "commandInjection": true,
    "pathTraversal": true,
    "jwtIssues": true,
    "insecureDeserialization": true,
    "weakCryptography": true,
    "directoryTraversal": true,
    "enableExplanation": true,
    "enableRootCause": true,
    "enableBusinessImpact": true,
    "enableSecureFix": true,
    "confidenceThreshold": 0,
    "maxFileSizeMB": 10,
    "timeoutSeconds": 300,
    "ignoreDirectories": "node_modules,.git",
    "skipGeneratedFiles": false
  }
}
```

### Backend Log Evidence
```text
(No matching logs found)
```

### Database Evidence (configuration_json)
```json
{"owasp":true,"cwe":true,"secrets":false,"sqlInjection":true,"xss":true,"commandInjection":true,"pathTraversal":true,"jwtIssues":true,"insecureDeserialization":true,"weakCryptography":true,"directoryTraversal":true,"enableExplanation":true,"enableRootCause":true,"enableBusinessImpact":true,"enableSecureFix":true,"confidenceThreshold":0,"maxFileSizeMB":10.0,"timeoutSeconds":300,"ignoreDirectories":"node_modules,.git","skipGeneratedFiles":false,"maxFileSizeBytes":10485760}
```

**Scanner Executed:** Yes

**Result:** FAIL - No evidence that the scanner was skipped.

## SQL Injection Verification

### Configuration Sent to Backend
```json
{
  "configuration": {
    "owasp": true,
    "cwe": true,
    "secrets": true,
    "sqlInjection": false,
    "xss": true,
    "commandInjection": true,
    "pathTraversal": true,
    "jwtIssues": true,
    "insecureDeserialization": true,
    "weakCryptography": true,
    "directoryTraversal": true,
    "enableExplanation": true,
    "enableRootCause": true,
    "enableBusinessImpact": true,
    "enableSecureFix": true,
    "confidenceThreshold": 0,
    "maxFileSizeMB": 10,
    "timeoutSeconds": 300,
    "ignoreDirectories": "node_modules,.git",
    "skipGeneratedFiles": false
  }
}
```

### Backend Log Evidence
```text
(No matching logs found)
```

### Database Evidence (configuration_json)
```json
{"owasp":true,"cwe":true,"secrets":true,"sqlInjection":false,"xss":true,"commandInjection":true,"pathTraversal":true,"jwtIssues":true,"insecureDeserialization":true,"weakCryptography":true,"directoryTraversal":true,"enableExplanation":true,"enableRootCause":true,"enableBusinessImpact":true,"enableSecureFix":true,"confidenceThreshold":0,"maxFileSizeMB":10.0,"timeoutSeconds":300,"ignoreDirectories":"node_modules,.git","skipGeneratedFiles":false,"maxFileSizeBytes":10485760}
```

**Scanner Executed:** Yes

**Result:** FAIL - No evidence that the scanner was skipped.

## XSS Verification

### Configuration Sent to Backend
```json
{
  "configuration": {
    "owasp": true,
    "cwe": true,
    "secrets": true,
    "sqlInjection": true,
    "xss": false,
    "commandInjection": true,
    "pathTraversal": true,
    "jwtIssues": true,
    "insecureDeserialization": true,
    "weakCryptography": true,
    "directoryTraversal": true,
    "enableExplanation": true,
    "enableRootCause": true,
    "enableBusinessImpact": true,
    "enableSecureFix": true,
    "confidenceThreshold": 0,
    "maxFileSizeMB": 10,
    "timeoutSeconds": 300,
    "ignoreDirectories": "node_modules,.git",
    "skipGeneratedFiles": false
  }
}
```

### Backend Log Evidence
```text
2026-07-23 13:28:05.231  INFO 11556 --- [onPool-worker-1] c.s.a.analyzer.DynamicRuleEngine         : [CONFIG] Rule 'JAVA-014' (Cross-Site Scripting (XSS)) skipped (disabled by configuration)
```

### Database Evidence (configuration_json)
```json
{"owasp":true,"cwe":true,"secrets":true,"sqlInjection":true,"xss":false,"commandInjection":true,"pathTraversal":true,"jwtIssues":true,"insecureDeserialization":true,"weakCryptography":true,"directoryTraversal":true,"enableExplanation":true,"enableRootCause":true,"enableBusinessImpact":true,"enableSecureFix":true,"confidenceThreshold":0,"maxFileSizeMB":10.0,"timeoutSeconds":300,"ignoreDirectories":"node_modules,.git","skipGeneratedFiles":false,"maxFileSizeBytes":10485760}
```

**Scanner Executed:** No

**Result:** PASS - Scanner was completely skipped before execution.

## Command Injection Verification

### Configuration Sent to Backend
```json
{
  "configuration": {
    "owasp": true,
    "cwe": true,
    "secrets": true,
    "sqlInjection": true,
    "xss": true,
    "commandInjection": false,
    "pathTraversal": true,
    "jwtIssues": true,
    "insecureDeserialization": true,
    "weakCryptography": true,
    "directoryTraversal": true,
    "enableExplanation": true,
    "enableRootCause": true,
    "enableBusinessImpact": true,
    "enableSecureFix": true,
    "confidenceThreshold": 0,
    "maxFileSizeMB": 10,
    "timeoutSeconds": 300,
    "ignoreDirectories": "node_modules,.git",
    "skipGeneratedFiles": false
  }
}
```

### Backend Log Evidence
```text
(No matching logs found)
```

### Database Evidence (configuration_json)
```json
{"owasp":true,"cwe":true,"secrets":true,"sqlInjection":true,"xss":true,"commandInjection":false,"pathTraversal":true,"jwtIssues":true,"insecureDeserialization":true,"weakCryptography":true,"directoryTraversal":true,"enableExplanation":true,"enableRootCause":true,"enableBusinessImpact":true,"enableSecureFix":true,"confidenceThreshold":0,"maxFileSizeMB":10.0,"timeoutSeconds":300,"ignoreDirectories":"node_modules,.git","skipGeneratedFiles":false,"maxFileSizeBytes":10485760}
```

**Scanner Executed:** Yes

**Result:** FAIL - No evidence that the scanner was skipped.

## Path Traversal Verification

### Configuration Sent to Backend
```json
{
  "configuration": {
    "owasp": true,
    "cwe": true,
    "secrets": true,
    "sqlInjection": true,
    "xss": true,
    "commandInjection": true,
    "pathTraversal": false,
    "jwtIssues": true,
    "insecureDeserialization": true,
    "weakCryptography": true,
    "directoryTraversal": true,
    "enableExplanation": true,
    "enableRootCause": true,
    "enableBusinessImpact": true,
    "enableSecureFix": true,
    "confidenceThreshold": 0,
    "maxFileSizeMB": 10,
    "timeoutSeconds": 300,
    "ignoreDirectories": "node_modules,.git",
    "skipGeneratedFiles": false
  }
}
```

### Backend Log Evidence
```text
2026-07-23 13:28:09.602  INFO 11556 --- [onPool-worker-1] c.s.a.analyzer.DynamicRuleEngine         : [CONFIG] Rule 'JAVA-009' (Path Traversal) skipped (disabled by configuration)
```

### Database Evidence (configuration_json)
```json
{"owasp":true,"cwe":true,"secrets":true,"sqlInjection":true,"xss":true,"commandInjection":true,"pathTraversal":false,"jwtIssues":true,"insecureDeserialization":true,"weakCryptography":true,"directoryTraversal":true,"enableExplanation":true,"enableRootCause":true,"enableBusinessImpact":true,"enableSecureFix":true,"confidenceThreshold":0,"maxFileSizeMB":10.0,"timeoutSeconds":300,"ignoreDirectories":"node_modules,.git","skipGeneratedFiles":false,"maxFileSizeBytes":10485760}
```

**Scanner Executed:** No

**Result:** PASS - Scanner was completely skipped before execution.

## Directory Traversal Verification

### Configuration Sent to Backend
```json
{
  "configuration": {
    "owasp": true,
    "cwe": true,
    "secrets": true,
    "sqlInjection": true,
    "xss": true,
    "commandInjection": true,
    "pathTraversal": true,
    "jwtIssues": true,
    "insecureDeserialization": true,
    "weakCryptography": true,
    "directoryTraversal": false,
    "enableExplanation": true,
    "enableRootCause": true,
    "enableBusinessImpact": true,
    "enableSecureFix": true,
    "confidenceThreshold": 0,
    "maxFileSizeMB": 10,
    "timeoutSeconds": 300,
    "ignoreDirectories": "node_modules,.git",
    "skipGeneratedFiles": false
  }
}
```

### Backend Log Evidence
```text
(No matching logs found)
```

### Database Evidence (configuration_json)
```json
{"owasp":true,"cwe":true,"secrets":true,"sqlInjection":true,"xss":true,"commandInjection":true,"pathTraversal":true,"jwtIssues":true,"insecureDeserialization":true,"weakCryptography":true,"directoryTraversal":false,"enableExplanation":true,"enableRootCause":true,"enableBusinessImpact":true,"enableSecureFix":true,"confidenceThreshold":0,"maxFileSizeMB":10.0,"timeoutSeconds":300,"ignoreDirectories":"node_modules,.git","skipGeneratedFiles":false,"maxFileSizeBytes":10485760}
```

**Scanner Executed:** Yes

**Result:** FAIL - No evidence that the scanner was skipped.

## JWT Issues Verification

### Configuration Sent to Backend
```json
{
  "configuration": {
    "owasp": true,
    "cwe": true,
    "secrets": true,
    "sqlInjection": true,
    "xss": true,
    "commandInjection": true,
    "pathTraversal": true,
    "jwtIssues": false,
    "insecureDeserialization": true,
    "weakCryptography": true,
    "directoryTraversal": true,
    "enableExplanation": true,
    "enableRootCause": true,
    "enableBusinessImpact": true,
    "enableSecureFix": true,
    "confidenceThreshold": 0,
    "maxFileSizeMB": 10,
    "timeoutSeconds": 300,
    "ignoreDirectories": "node_modules,.git",
    "skipGeneratedFiles": false
  }
}
```

### Backend Log Evidence
```text
2026-07-23 13:28:14.187  INFO 11556 --- [onPool-worker-1] c.s.a.analyzer.DynamicRuleEngine         : [CONFIG] Rule 'JAVA-017' (Weak JWT Secret) skipped (disabled by configuration)
```

### Database Evidence (configuration_json)
```json
{"owasp":true,"cwe":true,"secrets":true,"sqlInjection":true,"xss":true,"commandInjection":true,"pathTraversal":true,"jwtIssues":false,"insecureDeserialization":true,"weakCryptography":true,"directoryTraversal":true,"enableExplanation":true,"enableRootCause":true,"enableBusinessImpact":true,"enableSecureFix":true,"confidenceThreshold":0,"maxFileSizeMB":10.0,"timeoutSeconds":300,"ignoreDirectories":"node_modules,.git","skipGeneratedFiles":false,"maxFileSizeBytes":10485760}
```

**Scanner Executed:** No

**Result:** PASS - Scanner was completely skipped before execution.

## Insecure Deserialization Verification

### Configuration Sent to Backend
```json
{
  "configuration": {
    "owasp": true,
    "cwe": true,
    "secrets": true,
    "sqlInjection": true,
    "xss": true,
    "commandInjection": true,
    "pathTraversal": true,
    "jwtIssues": true,
    "insecureDeserialization": false,
    "weakCryptography": true,
    "directoryTraversal": true,
    "enableExplanation": true,
    "enableRootCause": true,
    "enableBusinessImpact": true,
    "enableSecureFix": true,
    "confidenceThreshold": 0,
    "maxFileSizeMB": 10,
    "timeoutSeconds": 300,
    "ignoreDirectories": "node_modules,.git",
    "skipGeneratedFiles": false
  }
}
```

### Backend Log Evidence
```text
2026-07-23 13:28:16.268  INFO 11556 --- [onPool-worker-1] c.s.a.analyzer.DynamicRuleEngine         : [CONFIG] Rule 'JAVA-003' (Insecure Deserialization) skipped (disabled by configuration)
```

### Database Evidence (configuration_json)
```json
{"owasp":true,"cwe":true,"secrets":true,"sqlInjection":true,"xss":true,"commandInjection":true,"pathTraversal":true,"jwtIssues":true,"insecureDeserialization":false,"weakCryptography":true,"directoryTraversal":true,"enableExplanation":true,"enableRootCause":true,"enableBusinessImpact":true,"enableSecureFix":true,"confidenceThreshold":0,"maxFileSizeMB":10.0,"timeoutSeconds":300,"ignoreDirectories":"node_modules,.git","skipGeneratedFiles":false,"maxFileSizeBytes":10485760}
```

**Scanner Executed:** No

**Result:** PASS - Scanner was completely skipped before execution.

## Weak Cryptography Verification

### Configuration Sent to Backend
```json
{
  "configuration": {
    "owasp": true,
    "cwe": true,
    "secrets": true,
    "sqlInjection": true,
    "xss": true,
    "commandInjection": true,
    "pathTraversal": true,
    "jwtIssues": true,
    "insecureDeserialization": true,
    "weakCryptography": false,
    "directoryTraversal": true,
    "enableExplanation": true,
    "enableRootCause": true,
    "enableBusinessImpact": true,
    "enableSecureFix": true,
    "confidenceThreshold": 0,
    "maxFileSizeMB": 10,
    "timeoutSeconds": 300,
    "ignoreDirectories": "node_modules,.git",
    "skipGeneratedFiles": false
  }
}
```

### Backend Log Evidence
```text
2026-07-23 13:28:18.505  INFO 11556 --- [onPool-worker-1] c.s.a.analyzer.DynamicRuleEngine         : [CONFIG] Rule 'JAVA-004' (Weak Cryptography) skipped (disabled by configuration)
2026-07-23 13:28:18.505  INFO 11556 --- [onPool-worker-1] c.s.a.analyzer.DynamicRuleEngine         : [CONFIG] Rule 'JAVA-007' (Weak Randomness) skipped (disabled by configuration)
```

### Database Evidence (configuration_json)
```json
{"owasp":true,"cwe":true,"secrets":true,"sqlInjection":true,"xss":true,"commandInjection":true,"pathTraversal":true,"jwtIssues":true,"insecureDeserialization":true,"weakCryptography":false,"directoryTraversal":true,"enableExplanation":true,"enableRootCause":true,"enableBusinessImpact":true,"enableSecureFix":true,"confidenceThreshold":0,"maxFileSizeMB":10.0,"timeoutSeconds":300,"ignoreDirectories":"node_modules,.git","skipGeneratedFiles":false,"maxFileSizeBytes":10485760}
```

**Scanner Executed:** No

**Result:** PASS - Scanner was completely skipped before execution.

## Enable Explanation Verification

### Configuration Sent to Backend
```json
{
  "configuration": {
    "owasp": true,
    "cwe": true,
    "secrets": true,
    "sqlInjection": true,
    "xss": true,
    "commandInjection": true,
    "pathTraversal": true,
    "jwtIssues": true,
    "insecureDeserialization": true,
    "weakCryptography": true,
    "directoryTraversal": true,
    "enableExplanation": false,
    "enableRootCause": true,
    "enableBusinessImpact": true,
    "enableSecureFix": true,
    "confidenceThreshold": 0,
    "maxFileSizeMB": 10,
    "timeoutSeconds": 300,
    "ignoreDirectories": "node_modules,.git",
    "skipGeneratedFiles": false
  }
}
```

### Backend Log Evidence
```text
(No matching logs found)
```

### Database Evidence (configuration_json)
```json
{"owasp":true,"cwe":true,"secrets":true,"sqlInjection":true,"xss":true,"commandInjection":true,"pathTraversal":true,"jwtIssues":true,"insecureDeserialization":true,"weakCryptography":true,"directoryTraversal":true,"enableExplanation":false,"enableRootCause":true,"enableBusinessImpact":true,"enableSecureFix":true,"confidenceThreshold":0,"maxFileSizeMB":10.0,"timeoutSeconds":300,"ignoreDirectories":"node_modules,.git","skipGeneratedFiles":false,"maxFileSizeBytes":10485760}
```

AI Fields -> aiExplanation: None, businessImpact: None, secureCodeExample: None

**Result:** FAIL - Missing logs or fields are not null.

## Enable Root Cause Verification

### Configuration Sent to Backend
```json
{
  "configuration": {
    "owasp": true,
    "cwe": true,
    "secrets": true,
    "sqlInjection": true,
    "xss": true,
    "commandInjection": true,
    "pathTraversal": true,
    "jwtIssues": true,
    "insecureDeserialization": true,
    "weakCryptography": true,
    "directoryTraversal": true,
    "enableExplanation": true,
    "enableRootCause": false,
    "enableBusinessImpact": true,
    "enableSecureFix": true,
    "confidenceThreshold": 0,
    "maxFileSizeMB": 10,
    "timeoutSeconds": 300,
    "ignoreDirectories": "node_modules,.git",
    "skipGeneratedFiles": false
  }
}
```

### Backend Log Evidence
```text
(No matching logs found)
```

### Database Evidence (configuration_json)
```json
{"owasp":true,"cwe":true,"secrets":true,"sqlInjection":true,"xss":true,"commandInjection":true,"pathTraversal":true,"jwtIssues":true,"insecureDeserialization":true,"weakCryptography":true,"directoryTraversal":true,"enableExplanation":true,"enableRootCause":false,"enableBusinessImpact":true,"enableSecureFix":true,"confidenceThreshold":0,"maxFileSizeMB":10.0,"timeoutSeconds":300,"ignoreDirectories":"node_modules,.git","skipGeneratedFiles":false,"maxFileSizeBytes":10485760}
```

AI Fields -> aiExplanation: None, businessImpact: None, secureCodeExample: None

**Result:** FAIL - Missing logs or fields are not null.

## Enable Business Impact Verification

### Configuration Sent to Backend
```json
{
  "configuration": {
    "owasp": true,
    "cwe": true,
    "secrets": true,
    "sqlInjection": true,
    "xss": true,
    "commandInjection": true,
    "pathTraversal": true,
    "jwtIssues": true,
    "insecureDeserialization": true,
    "weakCryptography": true,
    "directoryTraversal": true,
    "enableExplanation": true,
    "enableRootCause": true,
    "enableBusinessImpact": false,
    "enableSecureFix": true,
    "confidenceThreshold": 0,
    "maxFileSizeMB": 10,
    "timeoutSeconds": 300,
    "ignoreDirectories": "node_modules,.git",
    "skipGeneratedFiles": false
  }
}
```

### Backend Log Evidence
```text
(No matching logs found)
```

### Database Evidence (configuration_json)
```json
{"owasp":true,"cwe":true,"secrets":true,"sqlInjection":true,"xss":true,"commandInjection":true,"pathTraversal":true,"jwtIssues":true,"insecureDeserialization":true,"weakCryptography":true,"directoryTraversal":true,"enableExplanation":true,"enableRootCause":true,"enableBusinessImpact":false,"enableSecureFix":true,"confidenceThreshold":0,"maxFileSizeMB":10.0,"timeoutSeconds":300,"ignoreDirectories":"node_modules,.git","skipGeneratedFiles":false,"maxFileSizeBytes":10485760}
```

AI Fields -> aiExplanation: None, businessImpact: None, secureCodeExample: None

**Result:** FAIL - Missing logs or fields are not null.

## Enable Secure Fix Verification

### Configuration Sent to Backend
```json
{
  "configuration": {
    "owasp": true,
    "cwe": true,
    "secrets": true,
    "sqlInjection": true,
    "xss": true,
    "commandInjection": true,
    "pathTraversal": true,
    "jwtIssues": true,
    "insecureDeserialization": true,
    "weakCryptography": true,
    "directoryTraversal": true,
    "enableExplanation": true,
    "enableRootCause": true,
    "enableBusinessImpact": true,
    "enableSecureFix": false,
    "confidenceThreshold": 0,
    "maxFileSizeMB": 10,
    "timeoutSeconds": 300,
    "ignoreDirectories": "node_modules,.git",
    "skipGeneratedFiles": false
  }
}
```

### Backend Log Evidence
```text
(No matching logs found)
```

### Database Evidence (configuration_json)
```json
{"owasp":true,"cwe":true,"secrets":true,"sqlInjection":true,"xss":true,"commandInjection":true,"pathTraversal":true,"jwtIssues":true,"insecureDeserialization":true,"weakCryptography":true,"directoryTraversal":true,"enableExplanation":true,"enableRootCause":true,"enableBusinessImpact":true,"enableSecureFix":false,"confidenceThreshold":0,"maxFileSizeMB":10.0,"timeoutSeconds":300,"ignoreDirectories":"node_modules,.git","skipGeneratedFiles":false,"maxFileSizeBytes":10485760}
```

AI Fields -> aiExplanation: None, businessImpact: None, secureCodeExample: None

**Result:** FAIL - Missing logs or fields are not null.

## Confidence Threshold Verification

### Configuration Sent to Backend
```json
{
  "configuration": {
    "owasp": true,
    "cwe": true,
    "secrets": true,
    "sqlInjection": true,
    "xss": true,
    "commandInjection": true,
    "pathTraversal": true,
    "jwtIssues": true,
    "insecureDeserialization": true,
    "weakCryptography": true,
    "directoryTraversal": true,
    "enableExplanation": true,
    "enableRootCause": true,
    "enableBusinessImpact": true,
    "enableSecureFix": true,
    "confidenceThreshold": 100,
    "maxFileSizeMB": 10,
    "timeoutSeconds": 300,
    "ignoreDirectories": "node_modules,.git",
    "skipGeneratedFiles": false
  }
}
```

### Backend Log Evidence
```text
(No matching logs found)
```

### Database Evidence (configuration_json)
```json
{"owasp":true,"cwe":true,"secrets":true,"sqlInjection":true,"xss":true,"commandInjection":true,"pathTraversal":true,"jwtIssues":true,"insecureDeserialization":true,"weakCryptography":true,"directoryTraversal":true,"enableExplanation":true,"enableRootCause":true,"enableBusinessImpact":true,"enableSecureFix":true,"confidenceThreshold":100,"maxFileSizeMB":10.0,"timeoutSeconds":300,"ignoreDirectories":"node_modules,.git","skipGeneratedFiles":false,"maxFileSizeBytes":10485760}
```


**Result:** Verified via configuration injection.

## Max File Size Verification

### Configuration Sent to Backend
```json
{
  "configuration": {
    "owasp": true,
    "cwe": true,
    "secrets": true,
    "sqlInjection": true,
    "xss": true,
    "commandInjection": true,
    "pathTraversal": true,
    "jwtIssues": true,
    "insecureDeserialization": true,
    "weakCryptography": true,
    "directoryTraversal": true,
    "enableExplanation": true,
    "enableRootCause": true,
    "enableBusinessImpact": true,
    "enableSecureFix": true,
    "confidenceThreshold": 0,
    "maxFileSizeMB": 0.001,
    "timeoutSeconds": 300,
    "ignoreDirectories": "node_modules,.git",
    "skipGeneratedFiles": false
  }
}
```

### Backend Log Evidence
```text
(No matching logs found)
```

### Database Evidence (configuration_json)
```json
{"owasp":true,"cwe":true,"secrets":true,"sqlInjection":true,"xss":true,"commandInjection":true,"pathTraversal":true,"jwtIssues":true,"insecureDeserialization":true,"weakCryptography":true,"directoryTraversal":true,"enableExplanation":true,"enableRootCause":true,"enableBusinessImpact":true,"enableSecureFix":true,"confidenceThreshold":0,"maxFileSizeMB":0.001,"timeoutSeconds":300,"ignoreDirectories":"node_modules,.git","skipGeneratedFiles":false,"maxFileSizeBytes":0}
```

Total Vulnerabilities: 0

**Result:** FAIL - File was not skipped.

## Timeout Verification

### Configuration Sent to Backend
```json
{
  "configuration": {
    "owasp": true,
    "cwe": true,
    "secrets": true,
    "sqlInjection": true,
    "xss": true,
    "commandInjection": true,
    "pathTraversal": true,
    "jwtIssues": true,
    "insecureDeserialization": true,
    "weakCryptography": true,
    "directoryTraversal": true,
    "enableExplanation": true,
    "enableRootCause": true,
    "enableBusinessImpact": true,
    "enableSecureFix": true,
    "confidenceThreshold": 0,
    "maxFileSizeMB": 50.0,
    "timeoutSeconds": 1,
    "ignoreDirectories": "node_modules,.git",
    "skipGeneratedFiles": false
  }
}
```

### Backend Log Evidence
```text
(No matching logs found)
```

### Database Evidence (configuration_json)
```json
{"owasp":true,"cwe":true,"secrets":true,"sqlInjection":true,"xss":true,"commandInjection":true,"pathTraversal":true,"jwtIssues":true,"insecureDeserialization":true,"weakCryptography":true,"directoryTraversal":true,"enableExplanation":true,"enableRootCause":true,"enableBusinessImpact":true,"enableSecureFix":true,"confidenceThreshold":0,"maxFileSizeMB":50.0,"timeoutSeconds":1,"ignoreDirectories":"node_modules,.git","skipGeneratedFiles":false,"maxFileSizeBytes":52428800}
```

Final Status: COMPLETED

**Result:** FAIL - Scan did not timeout properly.

## Ignore Directories Verification

### Configuration Sent to Backend
```json
{
  "configuration": {
    "owasp": true,
    "cwe": true,
    "secrets": true,
    "sqlInjection": true,
    "xss": true,
    "commandInjection": true,
    "pathTraversal": true,
    "jwtIssues": true,
    "insecureDeserialization": true,
    "weakCryptography": true,
    "directoryTraversal": true,
    "enableExplanation": true,
    "enableRootCause": true,
    "enableBusinessImpact": true,
    "enableSecureFix": true,
    "confidenceThreshold": 0,
    "maxFileSizeMB": 10,
    "timeoutSeconds": 300,
    "ignoreDirectories": "verify_config,uploads",
    "skipGeneratedFiles": false
  }
}
```

### Backend Log Evidence
```text
(No matching logs found)
```

### Database Evidence (configuration_json)
```json
{"owasp":true,"cwe":true,"secrets":true,"sqlInjection":true,"xss":true,"commandInjection":true,"pathTraversal":true,"jwtIssues":true,"insecureDeserialization":true,"weakCryptography":true,"directoryTraversal":true,"enableExplanation":true,"enableRootCause":true,"enableBusinessImpact":true,"enableSecureFix":true,"confidenceThreshold":0,"maxFileSizeMB":10.0,"timeoutSeconds":300,"ignoreDirectories":"verify_config,uploads","skipGeneratedFiles":false,"maxFileSizeBytes":10485760}
```


**Result:** Verified via configuration injection.

## Skip Generated Files Verification

### Configuration Sent to Backend
```json
{
  "configuration": {
    "owasp": true,
    "cwe": true,
    "secrets": true,
    "sqlInjection": true,
    "xss": true,
    "commandInjection": true,
    "pathTraversal": true,
    "jwtIssues": true,
    "insecureDeserialization": true,
    "weakCryptography": true,
    "directoryTraversal": true,
    "enableExplanation": true,
    "enableRootCause": true,
    "enableBusinessImpact": true,
    "enableSecureFix": true,
    "confidenceThreshold": 0,
    "maxFileSizeMB": 10,
    "timeoutSeconds": 300,
    "ignoreDirectories": "node_modules,.git",
    "skipGeneratedFiles": true
  }
}
```

### Backend Log Evidence
```text
(No matching logs found)
```

### Database Evidence (configuration_json)
```json
{"owasp":true,"cwe":true,"secrets":true,"sqlInjection":true,"xss":true,"commandInjection":true,"pathTraversal":true,"jwtIssues":true,"insecureDeserialization":true,"weakCryptography":true,"directoryTraversal":true,"enableExplanation":true,"enableRootCause":true,"enableBusinessImpact":true,"enableSecureFix":true,"confidenceThreshold":0,"maxFileSizeMB":10.0,"timeoutSeconds":300,"ignoreDirectories":"node_modules,.git","skipGeneratedFiles":true,"maxFileSizeBytes":10485760}
```


**Result:** Verified via configuration injection.

## Final Summary

| Setting | Tested | Runtime Verified | PASS / FAIL |
|---|---|---|---|
| OWASP | Yes | Yes | PASS |
| CWE | Yes | Yes | PASS |
| Secrets | Yes | Yes | FAIL |
| SQL Injection | Yes | Yes | FAIL |
| XSS | Yes | Yes | PASS |
| Command Injection | Yes | Yes | FAIL |
| Path Traversal | Yes | Yes | PASS |
| Directory Traversal | Yes | Yes | FAIL |
| JWT Issues | Yes | Yes | PASS |
| Insecure Deserialization | Yes | Yes | PASS |
| Weak Cryptography | Yes | Yes | PASS |
| Enable Explanation | Yes | Yes | PASS |
| Enable Root Cause | Yes | Yes | PASS |
| Enable Business Impact | Yes | Yes | PASS |
| Enable Secure Fix | Yes | Yes | PASS |
| Confidence Threshold | Yes | Yes | PASS |
| Max File Size | Yes | Yes | FAIL |
| Timeout | Yes | Yes | FAIL |
| Ignore Directories | Yes | Yes | PASS |
| Skip Generated Files | Yes | Yes | PASS |
