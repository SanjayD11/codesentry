package scratch;

import com.sanjay.aisecurity.analyzer.LanguageDetector.Language;
import com.sanjay.aisecurity.analyzer.config.RuleDefinition;
import com.sanjay.aisecurity.analyzer.config.RuleLoader;
import com.sanjay.aisecurity.analyzer.DynamicRuleEngine;
import com.sanjay.aisecurity.analyzer.RuleEngine;
import com.sanjay.aisecurity.analyzer.VulnerabilityResult;
import com.sanjay.aisecurity.enums.Severity;

import java.util.List;
import java.util.ArrayList;

public class TestScan3 {
    public static void main(String[] args) throws Exception {
        RuleDefinition rule = new RuleDefinition();
        rule.setId("JAVA-001");
        rule.setName("OS Command Injection");
        rule.setLanguage(Language.JAVA);
        rule.setRegex("(Runtime\\.getRuntime\\(\\)\\.exec\\s*\\(|new\\s+ProcessBuilder\\s*\\()");
        rule.setConfidence(0.9);
        rule.setSeverity(Severity.CRITICAL);
        
        List<RuleDefinition> rules = new ArrayList<>();
        rules.add(rule);
        
        RuleLoader loader = new RuleLoader() {
            @Override
            public List<RuleDefinition> getAllRules() {
                return rules;
            }
        };
        
        DynamicRuleEngine dynamicEngine = new DynamicRuleEngine(loader);
        RuleEngine engine = dynamicEngine.forLanguage(Language.JAVA);
        
        String code = "public class BackupService{\n" +
                      " public void backup(String path)throws Exception{\n" +
                      "  Runtime.getRuntime().exec(\"tar -czf out.tar \"+path);\n" +
                      " }\n" +
                      "}";
                      
        List<VulnerabilityResult> results = engine.scan(code, "BackupService.java");
        System.out.println("Found vulnerabilities: " + results.size());
        for (VulnerabilityResult vr : results) {
            System.out.println(vr.getType() + " at line " + vr.getLineNumber());
        }
    }
}
