package com.sanjay.aisecurity.analyzer.config;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
public class RuleLoader {

    private final List<RuleDefinition> allRules = new ArrayList<>();

    @PostConstruct
    public void loadRules() {
        try {
            ObjectMapper mapper = new ObjectMapper();
            PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
            Resource[] resources = resolver.getResources("classpath:rules/*.json");

            for (Resource resource : resources) {
                try (InputStream is = resource.getInputStream()) {
                    List<RuleDefinition> rules = mapper.readValue(is, new TypeReference<List<RuleDefinition>>() {});
                    allRules.addAll(rules);
                    log.info("Loaded {} rules from {}", rules.size(), resource.getFilename());
                }
            }
            log.info("Total rules loaded independently: {}", allRules.size());
        } catch (Exception e) {
            log.error("Failed to load rules from JSON", e);
        }
    }

    public List<RuleDefinition> getAllRules() {
        return allRules;
    }
}
