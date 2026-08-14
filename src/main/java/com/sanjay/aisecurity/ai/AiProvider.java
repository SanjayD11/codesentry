package com.sanjay.aisecurity.ai;

/**
 * AI Provider abstraction.
 * Defines the contract for all AI provider integrations (Gemini, OpenAI, etc.).
 *
 * @author Sanjay
 * @version 1.0.0
 */
public interface AiProvider {

    /**
     * Sends a prompt to the AI provider and returns the generated text response.
     *
     * @param prompt the full prompt text to send
     * @return the AI-generated text response
     */
    String complete(String prompt);

    /**
     * Returns the human-readable name of this provider.
     *
     * @return provider identifier string
     */
    String getProviderName();
    
    /**
     * Sends a prompt to the AI provider with a specific feature context.
     * Defaults to the standard complete method if not overridden.
     *
     * @param prompt the full prompt text to send
     * @param feature the feature context of this call
     * @return the AI-generated text response
     */
    default String complete(String prompt, AiFeature feature) {
        return complete(prompt);
    }
}
