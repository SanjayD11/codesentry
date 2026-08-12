package com.sanjay.aisecurity.analyzer;

import com.sanjay.aisecurity.analyzer.config.RuleDefinition;
import com.sanjay.aisecurity.analyzer.config.RuleLoader;
import com.sanjay.aisecurity.dto.request.ScanConfigurationDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A generic engine factory that creates RuleEngines for each language dynamically
 * based on rules loaded from JSON.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DynamicRuleEngine {

    private final RuleLoader ruleLoader;

    // ConcurrentHashMap: thread-safe for concurrent reads + writes from the async scan pool.
    // Pattern objects are immutable once compiled — safe to cache and share across threads.
    // Fix D3: was HashMap (not thread-safe under concurrent scan workers).
    private final Map<String, Pattern> compiledPatterns = new ConcurrentHashMap<>();

    /** Returns a rule engine using DEFAULT configuration (all rules enabled). */
    public RuleEngine forLanguage(LanguageDetector.Language language) {
        return (code, fileName) -> performScan(code, fileName, language, ScanConfigurationDto.defaults());
    }

    /** Returns a rule engine that filters rules according to the given config before scanning. */
    public RuleEngine forLanguage(LanguageDetector.Language language, ScanConfigurationDto config) {
        return (code, fileName) -> performScan(code, fileName, language, config);
    }
    
    private List<VulnerabilityResult> performScan(
            String code, String fileName,
            LanguageDetector.Language language,
            ScanConfigurationDto config) {

        List<VulnerabilityResult> results = new ArrayList<>();
        if (code == null || code.trim().isEmpty()) {
            return results;
        }

        String[] lines = code.split("\\r?\\n");
        List<RuleDefinition> activeRules = new ArrayList<>();
        for (RuleDefinition r : ruleLoader.getAllRules()) {
            if (r.getLanguage() == language || r.getLanguage() == LanguageDetector.Language.UNKNOWN) {
                if (isRuleEnabled(r, config)) {
                    activeRules.add(r);
                } else {
                    log.info("[CONFIG] Rule '{}' ({}) skipped (disabled by configuration)", r.getId(), r.getName());
                }
            }
        }

        log.debug("[DynamicRuleEngine] {} of {} rules active for language={} (config-filtered)",
                activeRules.size(),
                ruleLoader.getAllRules().stream()
                        .filter(r -> r.getLanguage() == language || r.getLanguage() == LanguageDetector.Language.UNKNOWN)
                        .count(),
                language);

        // Merge finding state: key = ruleId + fileName
        Map<String, VulnerabilityResult> mergedResults = new HashMap<>();

        for (int i = 0; i < lines.length; i++) {
            if (Thread.currentThread().isInterrupted()) {
                log.warn("[DynamicRuleEngine] Thread interrupted, aborting scan for {}", fileName);
                break;
            }
            String line = lines[i];

            if (isCommentOrDoc(line) || isPureStringLiteral(line)) {
                continue;
            }

            for (RuleDefinition rule : activeRules) {
                Pattern p = getPattern(rule.getRegex());
                if (p == null) continue;

                Matcher matcher = p.matcher(line);
                if (matcher.find()) {
                    if (!passesContextualCheck(rule, line, lines, i)) {
                        continue;
                    }

                    String key = rule.getId() + ":" + fileName;
                    VulnerabilityResult existing = mergedResults.get(key);
                    
                    if (existing == null) {
                        VulnerabilityResult newRes = createResult(rule, fileName, i, lines);
                        mergedResults.put(key, newRes);
                    } else {
                        // Merge duplicate finding: append line number to evidence JSON and snippet
                        if (!existing.getCodeSnippet().contains("Line " + (i + 1))) {
                             String updatedSnippet = existing.getCodeSnippet() + "\n... [Line " + (i + 1) + "] " + line.trim();
                             if (updatedSnippet.length() > 1900) {
                                 updatedSnippet = updatedSnippet.substring(0, 1900) + "...";
                             }
                             existing.setCodeSnippet(updatedSnippet);
                             
                             // Update evidence JSON to include multiple affected lines
                             String evidence = existing.getEvidence();
                             evidence = evidence.replace("],\"confidenceReason\"", ", " + (i + 1) + "],\"confidenceReason\"");
                             existing.setEvidence(evidence);
                        }
                    }
                }
            }
        }
        
        return new ArrayList<>(mergedResults.values());
    }

    // =========================================================================
    // CONFIG-DRIVEN RULE GATE
    // =========================================================================

    /**
     * Returns {@code true} if the given rule should run under the provided configuration.
     *
     * <p>Rules are matched by their JSON {@code category} field and, for broad categories
     * like "Injection" that contain multiple vulnerability types, by rule name prefix.
     * Any category without a dedicated config flag always runs (fail-open / safe default).</p>
     */
    private boolean isRuleEnabled(RuleDefinition rule, ScanConfigurationDto config) {
        if (config == null) return true;
        String id = rule.getId();
        if (id == null) return true;

        if (id.equals("JAVA-004") || id.equals("JAVA-007")) return config.isWeakCryptography();
        if (id.equals("JAVA-003") || id.equals("PY-003")) return config.isInsecureDeserialization();
        if (id.equals("JAVA-002") || id.equals("PY-002")) return config.isSqlInjection();
        if (id.equals("JAVA-014")) return config.isXss();
        if (id.equals("JAVA-001") || id.equals("PY-001")) return config.isCommandInjection();
        if (id.equals("JAVA-017")) return config.isJwtIssues();
        if (id.equals("JAVA-005") || id.equals("JAVA-016")) return config.isSecrets();
        if (id.equals("JAVA-009")) return config.isPathTraversal();
        if (id.equals("JAVA-022")) return config.isDirectoryTraversal();

        // No dedicated toggle — always run
        return true;
    }

    private boolean passesContextualCheck(RuleDefinition rule, String line, String[] allLines, int lineIndex) {
        if (rule.getIgnorePatterns() != null && !rule.getIgnorePatterns().isEmpty()) {
            for (String ignore : rule.getIgnorePatterns()) {
                Pattern ip = getPattern(ignore);
                if (ip != null && ip.matcher(line).find()) {
                    return false;
                }
            }
        }

        if (rule.getRequirePatternsNearby() != null && !rule.getRequirePatternsNearby().isEmpty()) {
            boolean foundRequired = false;
            int start = Math.max(0, lineIndex - rule.getContextLines());
            int end = Math.min(allLines.length - 1, lineIndex + rule.getContextLines());
            
            for (int i = start; i <= end; i++) {
                for (String req : rule.getRequirePatternsNearby()) {
                    Pattern rp = getPattern(req);
                    if (rp != null && rp.matcher(allLines[i]).find()) {
                        foundRequired = true;
                        break;
                    }
                }
                if (foundRequired) break;
            }
            if (!foundRequired) {
                return false;
            }
        }

        return true;
    }

    private Pattern getPattern(String regex) {
        if (regex == null || regex.isEmpty()) return null;
        return compiledPatterns.computeIfAbsent(regex, r -> {
            try {
                return Pattern.compile(r);
            } catch (Exception e) {
                log.error("Invalid regex in rule: {}", r);
                return null;
            }
        });
    }

    private boolean isCommentOrDoc(String line) {
        String trimmed = line.trim();
        return trimmed.startsWith("//") || trimmed.startsWith("/*") || trimmed.startsWith("*") || 
               trimmed.startsWith("#") || trimmed.startsWith("<!--");
    }

    private boolean isPureStringLiteral(String line) {
        String trimmed = line.trim();
        return (trimmed.startsWith("\"") && trimmed.endsWith("\"") && trimmed.length() > 2) ||
               (trimmed.startsWith("'") && trimmed.endsWith("'") && trimmed.length() > 2);
    }

    private VulnerabilityResult createResult(RuleDefinition rule, String fileName, int lineIndex, String[] allLines) {
        int start = Math.max(0, lineIndex - 5);
        int end = Math.min(allLines.length - 1, lineIndex + 5);
        StringBuilder contextBuilder = new StringBuilder();
        for (int i = start; i <= end; i++) {
            if (i == lineIndex) {
                contextBuilder.append(String.format("%d: -> %s\n", i + 1, allLines[i]));
            } else {
                contextBuilder.append(String.format("%d:    %s\n", i + 1, allLines[i]));
            }
        }
        String snippet = contextBuilder.toString().trim();
        int lineNum = lineIndex + 1;
        
        String evidence = generateEvidence(rule, allLines[lineIndex], lineNum);
        double finalConfidence = rule.getConfidence();
        if (rule.getRequirePatternsNearby() != null && !rule.getRequirePatternsNearby().isEmpty()) {
             finalConfidence = Math.min(1.0, finalConfidence + 0.1);
        }

        return VulnerabilityResult.builder()
                .type(rule.getName())
                .severity(rule.getSeverity())
                .description(rule.getDescription())
                .recommendation(rule.getFixSuggestion())
                .fileName(fileName)
                .lineNumber(lineNum)
                .codeSnippet(snippet.length() > 2000 ? snippet.substring(0, 1997) + "..." : snippet)
                .confidenceScore(finalConfidence)
                .evidence(evidence)
                .owaspCategory(rule.getOwasp())
                .cweId(rule.getCwe())
                .ruleId(rule.getId())
                .detectionSource("Dynamic Engine v1.2")
                .build();
    }

    private String generateEvidence(RuleDefinition rule, String matchLine, int lineNum) {
        String safeSnippet = matchLine.trim().replace("\"", "\\\"").replace("\n", "").replace("\\", "\\\\");
        return "{" +
                "\"ruleId\":\"" + rule.getId() + "\"," +
                "\"affectedLines\":[" + lineNum + "]," +
                "\"confidenceReason\":\"Lexical and contextual evaluation passed.\"," +
                "\"sink\":\"" + safeSnippet + "\"" +
                "}";
    }
}
