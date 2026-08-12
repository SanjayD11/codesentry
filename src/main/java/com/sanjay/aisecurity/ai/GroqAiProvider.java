package com.sanjay.aisecurity.ai;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

/**
 * Groq AI Provider — Multi-model with automatic 3-tier failover.
 *
 * <p>Model priority (configured in application.yml):
 * <ol>
 *   <li>Primary  : qwen/qwen3.6-27b</li>
 *   <li>Fallback1: llama-3.3-70b-versatile</li>
 *   <li>Fallback2: openai/gpt-oss-120b</li>
 * </ol>
 *
 * <p>Automatic failover is triggered on:
 * HTTP 429, 500, 502, 503, 504, timeout, network failure, empty/invalid response.
 * The frontend never learns which model responded.
 *
 * @author Sanjay
 * @version 2.0.0
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "app.ai.provider", havingValue = "groq", matchIfMissing = true)
public class GroqAiProvider implements AiProvider {

    private final WebClient webClient;
    private final String apiKey;
    private final String primaryModel;
    private final String fallbackModel;
    private final String fallback2Model;
    private final int timeoutSeconds;
    private final int maxTokens;
    private final double temperature;

    public GroqAiProvider(
            WebClient.Builder webClientBuilder,
            @Value("${app.ai.groq.base-url:https://api.groq.com/openai/v1}") String baseUrl,
            @Value("${app.ai.groq.api-key:}") String apiKey,
            @Value("${app.ai.groq.model.primary:qwen/qwen3.6-27b}") String primaryModel,
            @Value("${app.ai.groq.model.fallback:llama-3.3-70b-versatile}") String fallbackModel,
            @Value("${app.ai.groq.model.fallback2:openai/gpt-oss-120b}") String fallback2Model,
            @Value("${app.ai.groq.timeout-seconds:30}") int timeoutSeconds,
            @Value("${app.ai.groq.max-tokens:2048}") int maxTokens,
            @Value("${app.ai.groq.temperature:0.2}") double temperature) {
        this.webClient = webClientBuilder.baseUrl(baseUrl).build();
        this.apiKey = apiKey;
        this.primaryModel = primaryModel;
        this.fallbackModel = fallbackModel;
        this.fallback2Model = fallback2Model;
        this.timeoutSeconds = timeoutSeconds;
        this.maxTokens = maxTokens;
        this.temperature = temperature;
    }

    @Override
    public String complete(String prompt) {
        if (apiKey == null || apiKey.isBlank()) {
            log.warn("Groq API key is not configured. Returning placeholder response.");
            return "AI enrichment unavailable: API key not configured.";
        }

        String requestId = UUID.randomUUID().toString();
        long startTime = System.currentTimeMillis();

        // ── Tier 1: Primary model ─────────────────────────────────────────────
        log.info("[AI] [{}] Primary model selected: {}", requestId, primaryModel);
        try {
            String result = callGroq(requestId, primaryModel, prompt, startTime, 1);
            log.info("[AI] [{}] Primary model ({}) responded successfully.", requestId, primaryModel);
            return result;
        } catch (Exception e) {
            log.warn("[AI] [{}] Primary model ({}) unavailable: {}. Switching to fallback 1: {}",
                    requestId, primaryModel, summarize(e), fallbackModel);
        }

        // ── Tier 2: First fallback ────────────────────────────────────────────
        try {
            String result = callGroq(requestId, fallbackModel, prompt, startTime, 2);
            log.info("[AI] [{}] Fallback 1 ({}) responded successfully.", requestId, fallbackModel);
            return result;
        } catch (Exception e) {
            log.warn("[AI] [{}] Fallback 1 ({}) unavailable: {}. Switching to fallback 2: {}",
                    requestId, fallbackModel, summarize(e), fallback2Model);
        }

        // ── Tier 3: Second fallback ───────────────────────────────────────────
        try {
            String result = callGroq(requestId, fallback2Model, prompt, startTime, 3);
            log.info("[AI] [{}] Fallback 2 ({}) responded successfully.", requestId, fallback2Model);
            return result;
        } catch (Exception e) {
            log.error("[AI] [{}] All three models failed. Last error ({}): {}",
                    requestId, fallback2Model, e.getMessage());
            return "AI enrichment unavailable: All AI models are currently unavailable. Please try again later.";
        }
    }

    // =========================================================================
    // INTERNAL CALL
    // =========================================================================

    private String callGroq(String requestId, String model, String prompt, long startTime, int tier) {
        GroqRequest request = new GroqRequest(
                model,
                List.of(new Message("user", prompt)),
                temperature,
                maxTokens
        );

        GroqResponse response;
        try {
            response = webClient.post()
                    .uri("/chat/completions")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(GroqResponse.class)
                    .timeout(Duration.ofSeconds(timeoutSeconds))
                    .block();
        } catch (WebClientResponseException e) {
            // HTTP 4xx/5xx — always propagate so the failover logic can catch it
            throw new RuntimeException(
                    String.format("HTTP %d from Groq (model=%s): %s", e.getStatusCode().value(), model, e.getMessage()), e);
        }

        long duration = System.currentTimeMillis() - startTime;

        if (response != null && response.choices() != null && !response.choices().isEmpty()) {
            GroqResponse.Choice choice = response.choices().get(0);
            if (choice.message() != null && choice.message().content() != null
                    && !choice.message().content().isBlank()) {

                int promptTokens      = response.usage() != null ? response.usage().promptTokens()     : 0;
                int completionTokens  = response.usage() != null ? response.usage().completionTokens() : 0;
                int totalTokens       = response.usage() != null ? response.usage().totalTokens()      : 0;

                log.info("[AI] [{}] Tier {} | Model: {} | Time: {}ms | Tokens [prompt={} completion={} total={}]",
                        requestId, tier, model, duration, promptTokens, completionTokens, totalTokens);

                return choice.message().content();
            }
        }

        throw new RuntimeException("Empty or invalid response from Groq API (model=" + model + ").");
    }

    /** Extracts a short, log-safe summary of an exception without leaking prompts or keys. */
    private String summarize(Exception e) {
        if (e.getCause() != null && e.getCause().getClass().getSimpleName().contains("Timeout")) {
            return "timeout";
        }
        String msg = e.getMessage();
        if (msg == null) return e.getClass().getSimpleName();
        return msg.length() > 120 ? msg.substring(0, 120) + "…" : msg;
    }

    @Override
    public String getProviderName() {
        return "Groq";
    }

    // =========================================================================
    // INNER REQUEST / RESPONSE RECORDS
    // =========================================================================

    record Message(String role, String content) {}

    record GroqRequest(
            String model,
            List<Message> messages,
            double temperature,
            @JsonProperty("max_tokens") int maxTokens
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record GroqResponse(
            String id,
            List<Choice> choices,
            Usage usage
    ) {
        @JsonIgnoreProperties(ignoreUnknown = true)
        record Choice(Message message) {}

        @JsonIgnoreProperties(ignoreUnknown = true)
        record Usage(
                @JsonProperty("prompt_tokens")      int promptTokens,
                @JsonProperty("completion_tokens")  int completionTokens,
                @JsonProperty("total_tokens")       int totalTokens
        ) {}
    }
}
