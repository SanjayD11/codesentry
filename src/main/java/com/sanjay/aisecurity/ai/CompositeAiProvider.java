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
        // Do not fall back for malformed application requests, context length issues, authentication, etc.
        if (msg.contains("400") || msg.contains("401") || msg.contains("403") || msg.contains("404")) {
            return false;
        }
        // Fallback for transient provider errors
        return msg.contains("timeout") 
            || msg.contains("500")
            || msg.contains("502") 
            || msg.contains("503") 
            || msg.contains("504") 
            || msg.contains("408")
            || msg.contains("429")
            || msg.contains("connection")
            || msg.contains("reset")
            || msg.contains("socket")
            || msg.contains("unavailable");
    }
}
