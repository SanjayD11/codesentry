package com.sanjay.aisecurity.controller;

import com.sanjay.aisecurity.analyzer.LanguageDetector;
import com.sanjay.aisecurity.analyzer.RuleEngine;
import com.sanjay.aisecurity.analyzer.DynamicRuleEngine;
import com.sanjay.aisecurity.analyzer.VulnerabilityResult;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/test")
@RequiredArgsConstructor
public class TestScanController {

    private final DynamicRuleEngine dynamicRuleEngine;

    @PostMapping("/scan")
    public List<VulnerabilityResult> testScan(@RequestBody Map<String, String> payload) {
        String code = payload.get("code");
        String langStr = payload.get("language");
        LanguageDetector.Language lang = LanguageDetector.Language.valueOf(langStr.toUpperCase());
        
        RuleEngine engine = dynamicRuleEngine.forLanguage(lang);
        return engine.scan(code, "TestFile.java");
    }
}
