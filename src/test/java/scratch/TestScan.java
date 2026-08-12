package scratch;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sanjay.aisecurity.analyzer.LanguageDetector.Language;
import com.sanjay.aisecurity.analyzer.config.RuleDefinition;
import com.sanjay.aisecurity.analyzer.DynamicRuleEngine;
import com.sanjay.aisecurity.analyzer.config.RuleLoader;
import com.sanjay.aisecurity.analyzer.RuleEngine;

import java.io.File;
import java.util.List;

public class TestScan {
    public static void main(String[] args) throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        List<RuleDefinition> rules = mapper.readValue(
            new File("src/main/resources/rules/java-rules.json"),
            new TypeReference<List<RuleDefinition>>() {}
        );
        
        System.out.println("Loaded rules: " + rules.size());
        for(RuleDefinition r : rules) {
            System.out.println(r.getId() + " - lang: " + r.getLanguage());
        }
    }
}
