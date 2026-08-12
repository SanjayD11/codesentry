package com.sanjay.aisecurity.analyzer;

import com.sanjay.aisecurity.enums.Severity;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;

/**
 * Base implementation of a RuleEngine to handle common parsing logic
 * and False Positive Reduction (ignoring comments, basic string detection).
 */
public abstract class BaseRuleEngine implements RuleEngine {

    protected abstract List<SecurityRule> getRules();

    /**
     * Optional method for language-specific comment detection.
     * By default, it ignores //, /*, and #
     */
    protected boolean isCommentOrDoc(String line) {
        String trimmed = line.trim();
        return trimmed.startsWith("//") || 
               trimmed.startsWith("/*") || 
               trimmed.startsWith("*") || 
               trimmed.startsWith("#") ||
               trimmed.startsWith("<!--");
    }

    /**
     * A very basic heuristic check to see if we might be in a string literal.
     * This simply ignores lines that appear to just be plain strings or text.
     */
    protected boolean isPureStringLiteral(String line) {
        String trimmed = line.trim();
        return (trimmed.startsWith("\"") && trimmed.endsWith("\"") && trimmed.length() > 2) ||
               (trimmed.startsWith("'") && trimmed.endsWith("'") && trimmed.length() > 2);
    }

    @Override
    public List<VulnerabilityResult> scan(String code, String fileName) {
        List<VulnerabilityResult> results = new ArrayList<>();
        if (code == null || code.trim().isEmpty()) {
            return results;
        }

        String[] lines = code.split("\\r?\\n");
        List<SecurityRule> rules = getRules();
        // Deduplication: never report the same rule on the same line twice
        java.util.Set<String> seen = new java.util.HashSet<>();

        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];

            if (isCommentOrDoc(line) || isPureStringLiteral(line)) {
                continue; // False positive reduction
            }

            for (SecurityRule rule : rules) {
                if (rule.getPattern() != null) {
                    Matcher matcher = rule.getPattern().matcher(line);
                    if (matcher.find()) {

                        // Perform contextual checks if supported by the engine
                        if (!passesContextualCheck(rule, line, lines, i)) {
                            continue;
                        }

                        // Deduplicate: skip if same rule already fired on this line
                        String dedupeKey = rule.getId() + ":" + (i + 1);
                        if (seen.contains(dedupeKey)) {
                            continue;
                        }
                        seen.add(dedupeKey);

                        results.add(createResult(rule, fileName, i, lines));
                    }
                }
            }
        }

        // Deduplication phase: Merge duplicate findings that occur close to each other
        return mergeDuplicates(results);
    }

    private List<VulnerabilityResult> mergeDuplicates(List<VulnerabilityResult> results) {
        List<VulnerabilityResult> merged = new ArrayList<>();
        for (VulnerabilityResult res : results) {
            boolean found = false;
            for (VulnerabilityResult existing : merged) {
                if (existing.getRuleId().equals(res.getRuleId()) && existing.getFileName().equals(res.getFileName())) {
                    if (Math.abs(existing.getLineNumber() - res.getLineNumber()) <= 5) {
                        // Merge them
                        found = true;
                        existing.setCodeSnippet(existing.getCodeSnippet() + "\n... [Merged Line " + res.getLineNumber() + "]\n" + res.getCodeSnippet());
                        if (existing.getCodeSnippet().length() > 2000) {
                            existing.setCodeSnippet(existing.getCodeSnippet().substring(0, 1997) + "...");
                        }
                        break;
                    }
                }
            }
            if (!found) {
                merged.add(res);
            }
        }
        return merged;
    }

    /**
     * Common sanitization check to reduce false positives
     */
    protected boolean isSanitized(String line, String[] allLines, int lineIndex) {
        String lower = line.toLowerCase();
        // Common sanitizers
        if (lower.contains("shlex.quote") || lower.contains("escape") || lower.contains("dompurify") || lower.contains("sanitize")) {
            return true;
        }
        return false;
    }

    protected double calculateConfidence(SecurityRule rule, String line, String[] allLines, int lineIndex) {
        double score = rule.getBaseConfidence();
        
        // Bonus for user input
        if (line.contains("req.") || line.contains("request.") || line.contains("params") || line.contains("body") || line.contains("input")) {
            score += 0.1;
        }

        // Penalty for sanitization
        if (isSanitized(line, allLines, lineIndex)) {
            score -= 0.5;
        }

        // Inside comments heavily penalized or skipped, but we already skip pure comments.
        
        return Math.max(0.1, Math.min(1.0, score));
    }


    /**
     * Hook method to allow child classes to perform contextual analysis around a match.
     * E.g., for SQLi, check if Statement.executeQuery is accompanied by string concatenation.
     */
    protected boolean passesContextualCheck(SecurityRule rule, String line, String[] allLines, int lineIndex) {
        return true; // Default to true, child classes can override
    }

    protected VulnerabilityResult createResult(SecurityRule rule, String fileName, int lineIndex, String[] allLines) {
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
        
        double finalConfidence = calculateConfidence(rule, allLines[lineIndex], allLines, lineIndex);
        String evidence = generateEvidence(rule, allLines[lineIndex], lineNum, finalConfidence);
        
        return VulnerabilityResult.builder()
                .type(rule.getName())
                .severity(rule.getSeverity())
                .description(rule.getDescription())
                .recommendation(rule.getRecommendation())
                .fileName(fileName)
                .lineNumber(lineNum)
                .codeSnippet(snippet.length() > 2000 ? snippet.substring(0, 1997) + "..." : snippet)
                .confidenceScore(finalConfidence)
                .evidence(evidence)
                .owaspCategory(rule.getOwaspCategory())
                .cweId(rule.getCweId())
                .ruleId(rule.getId())
                .detectionSource(this.getClass().getSimpleName())
                .build();
    }

    private String generateEvidence(SecurityRule rule, String matchLine, int lineNum, double confidence) {
        String safeSnippet = matchLine.trim().replace("\"", "\\\"").replace("\n", "").replace("\\", "\\\\");
        String detectionType = rule.getDetectionType() != null ? rule.getDetectionType() : "REGEX";
        
        // Evidence-first reporting structure
        return "{" +
                "\"ruleId\":\"" + (rule.getId() != null ? rule.getId() : "RULE-000") + "\"," +
                "\"matchedApi\":\"" + rule.getPattern().pattern().replace("\"", "\\\"").replace("\\", "\\\\") + "\"," +
                "\"line\":" + lineNum + "," +
                "\"sink\":\"" + safeSnippet + "\"," +
                "\"confidence\":" + Math.round(confidence * 100) + "," +
                "\"detectionType\":\"" + detectionType + "\"" +
                "}";
    }
}
