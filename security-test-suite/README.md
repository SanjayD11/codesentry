# SentinelAI Security Test Suite

This suite contains intentionally vulnerable and safe code samples used to systematically validate the SentinelAI engine.

## Structure

The test suite is organized by language, and then strictly divided into `vulnerable` and `safe` subdirectories.

```
security-test-suite/
├── README.md
├── expected-results.json
├── java/
│   ├── safe/
│   └── vulnerable/
├── python/
│   ├── safe/
│   └── vulnerable/
├── javascript/
│   ├── safe/
│   └── vulnerable/
├── php/
│   ├── safe/
│   └── vulnerable/
├── csharp/
│   ├── safe/
│   └── vulnerable/
└── go/
    ├── safe/
    └── vulnerable/
```

## Validation

The `expected-results.json` acts as the source of truth for the test suite. Every time you run SentinelAI against this directory, you must cross-reference the generated findings with `expected-results.json` to verify:

1. **Detection Accuracy**: Are all expected vulnerabilities found?
2. **Severity Mapping**: Is the severity categorized correctly?
3. **Confidence Scoring**: Does the context-engine assign an appropriate confidence score (checking thresholds)?
4. **False Positive Suppression**: Did the engine correctly ignore the files located in the `safe/` directories?

## Regression Testing

Whenever a new rule is added to `java-rules.json` or any dynamic engine mapping:
1. Add the corresponding vulnerable code to `<language>/vulnerable/`
2. Add the secure alternative to `<language>/safe/`
3. Update `expected-results.json`
4. Run SentinelAI against the entire suite.
5. A completely clean pass ensures no regression was introduced.
