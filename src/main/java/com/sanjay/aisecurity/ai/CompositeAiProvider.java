package com.sanjay.aisecurity.ai;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

/**
 * Composite AI Provider that orchestrates failover between primary (OpenRouter)
 * and secondary (Groq) providers to ensure maximum reliability.
 */
@Slf4j
@Primary
@Component
public class CompositeAiProvider implements AiProvider {

    private final OpenRouterAiProvider openRouterAiProvider;
    private final GroqAiProvider groqAiProvider;

    public CompositeAiProvider(OpenRouterAiProvider openRouterAiProvider, GroqAiProvider groqAiProvider) {
        this.openRouterAiProvider = openRouterAiProvider;
        this.groqAiProvider = groqAiProvider;
    }

    @Override
    public String complete(String prompt) {
        try {
            return openRouterAiProvider.complete(prompt);
        } catch (Exception e) {
            if (isTransientError(e)) {
                log.warn("[CompositeAI] OpenRouter complete() failed transiently: {}. Falling back to Groq.", e.getMessage());
                return groqAiProvider.complete(prompt);
            }
            log.error("[CompositeAI] OpenRouter complete() failed with application error: {}. Not falling back.", e.getMessage());
            throw e;
        }
    }

    @Override
    public String complete(String prompt, AiFeature feature) {
        try {
            return openRouterAiProvider.complete(prompt, feature);
        } catch (Exception e) {
            if (isTransientError(e)) {
                log.warn("[CompositeAI] OpenRouter complete(len={}, feature={}) failed transiently: {}. Falling back to Groq.", 
                         prompt.length(), feature, e.getMessage());
                return groqAiProvider.complete(prompt, feature);
            }
            log.error("[CompositeAI] OpenRouter complete(len={}, feature={}) failed with application error: {}. Not falling back.", 
                      prompt.length(), feature, e.getMessage());
            throw e;
        }
    }

    @Override
    public String getProviderName() {
        return "Composite(OpenRouter->Groq)";
    }

    private boolean isTransientError(Exception e) {
        String msg = e.getMessage() != null ? e.getMessage().toLowerCase() : "";
        
        // 400 Bad Request and 404 Not Found mean the payload itself is invalid or model doesn't exist.
        // It will fail on Groq too, so do not fallback.
        if (msg.contains("400 ") || msg.contains("404 ")) {
            return false;
        }
        
        // Always fallback for everything else:
        // - "not configured" (missing API key)
        // - "failed" (generic OpenRouter failed)
        // - 401/403 (Invalid API key / Out of credits)
        // - 5xx / timeouts / networking issues
        return true;
    }
}
