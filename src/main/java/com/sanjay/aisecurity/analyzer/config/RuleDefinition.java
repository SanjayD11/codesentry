package com.sanjay.aisecurity.analyzer.config;

import com.sanjay.aisecurity.analyzer.LanguageDetector.Language;
import com.sanjay.aisecurity.enums.Severity;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
public class RuleDefinition {
    private String id;
    private String name;
    private String description;
    private Severity severity;
    private double confidence;
    private String category;
    private String regex;
    private String fixSuggestion;
    private String cwe;
    private String owasp;
    private Language language;
    
    // False positive reduction / context
    private List<String> ignorePatterns;
    private List<String> requirePatternsNearby;
    private int contextLines = 5;
}
