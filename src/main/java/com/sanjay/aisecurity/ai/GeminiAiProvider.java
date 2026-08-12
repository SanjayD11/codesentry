package com.sanjay.aisecurity.ai;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.List;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

/**
 * Google Gemini AI Provider implementation.
 *
 * <p>Uses Spring WebClient (non-blocking HTTP) to call the Gemini generateContent API.
 * Designed as a thin HTTP adapter — all prompt construction is the caller's responsibility.</p>
 *
 * @author Sanjay
 * @version 1.0.0
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "app.ai.provider", havingValue = "gemini")
public class GeminiAiProvider implements AiProvider {

    private final WebClient webClient;
    private final String apiKey;
    private final String model;
    private final int timeoutSeconds;
    private final int maxTokens;
    private final double temperature;

    public GeminiAiProvider(
            WebClient.Builder webClientBuilder,
            @Value("${app.ai.gemini.base-url}") String baseUrl,
            @Value("${app.ai.gemini.api-key:}") String apiKey,
            @Value("${app.ai.gemini.model:gemini-1.5-flash}") String model,
            @Value("${app.ai.gemini.timeout-seconds:30}") int timeoutSeconds,
            @Value("${app.ai.gemini.max-tokens:8192}") int maxTokens,
            @Value("${app.ai.gemini.temperature:0.2}") double temperature) {
        this.webClient = webClientBuilder.baseUrl(baseUrl).build();
        this.apiKey = apiKey;
        this.model = model;
        this.timeoutSeconds = timeoutSeconds;
        this.maxTokens = maxTokens;
        this.temperature = temperature;
    }

    @Override
    public String complete(String prompt) {
        if (apiKey == null || apiKey.isBlank()) {
            log.warn("Gemini API key is not configured. Returning placeholder response.");
            return "AI enrichment unavailable: API key not configured.";
        }

        String uri = "/models/" + model + ":generateContent?key=" + apiKey;

        GeminiRequest request = new GeminiRequest(
                List.of(new GeminiRequest.Content(List.of(new GeminiRequest.Part(prompt)))),
                new GeminiRequest.GenerationConfig(temperature, maxTokens)
        );

        try {
            GeminiResponse response = webClient.post()
                    .uri(uri)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(GeminiResponse.class)
                    .timeout(Duration.ofSeconds(timeoutSeconds))
                    .block();

            if (response != null && response.candidates() != null && !response.candidates().isEmpty()) {
                GeminiResponse.Candidate candidate = response.candidates().get(0);
                if (candidate.content() != null && candidate.content().parts() != null
                        && !candidate.content().parts().isEmpty()) {
                    return candidate.content().parts().get(0).text();
                }
            }
            log.warn("Gemini returned empty response.");
            return "AI enrichment unavailable: Empty response from provider.";

        } catch (Exception e) {
            log.error("Gemini API call failed: {}", e.getMessage());
            return "AI enrichment unavailable: " + e.getMessage();
        }
    }

    @Override
    public String getProviderName() {
        return "Gemini " + model;
    }

    // =========================================================================
    // INNER REQUEST / RESPONSE RECORDS
    // =========================================================================

    record GeminiRequest(
            List<Content> contents,
            @JsonProperty("generationConfig") GenerationConfig generationConfig
    ) {
        record Content(List<Part> parts) {}
        record Part(String text) {}
        record GenerationConfig(double temperature, int maxOutputTokens) {}
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record GeminiResponse(List<Candidate> candidates) {
        @JsonIgnoreProperties(ignoreUnknown = true)
        record Candidate(Content content) {}
        @JsonIgnoreProperties(ignoreUnknown = true)
        record Content(List<Part> parts) {}
        @JsonIgnoreProperties(ignoreUnknown = true)
        record Part(String text) {}
    }
}
