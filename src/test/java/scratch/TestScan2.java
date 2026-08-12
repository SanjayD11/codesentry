package scratch;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sanjay.aisecurity.analyzer.LanguageDetector.Language;
import com.sanjay.aisecurity.analyzer.config.RuleDefinition;
import com.sanjay.aisecurity.analyzer.config.RuleLoader;
import com.sanjay.aisecurity.analyzer.DynamicRuleEngine;
import com.sanjay.aisecurity.analyzer.RuleEngine;
import com.sanjay.aisecurity.analyzer.VulnerabilityResult;

import java.io.File;
import java.util.List;
import java.util.Collections;

public class TestScan2 {
    public static void main(String[] args) throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        List<RuleDefinition> rules = mapper.readValue(
            new File("D:/Micro Project/src/main/resources/rules/java-rules.json"),
            new TypeReference<List<RuleDefinition>>() {}
        );
        
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
