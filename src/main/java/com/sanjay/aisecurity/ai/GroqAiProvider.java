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
    
    // Feature-specific models
    private final String enrichmentModel;
    private final String enrichmentFallbackModel;
    private final String reportModel;
    private final String reportFallbackModel;
    private final String assistantModel;
    private final String assistantFallbackModel;
    private final String quickScanModel;
    private final String quickScanFallbackModel;
    
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
            @Value("${app.ai.groq.model.enrichment-model:llama-3.3-70b-versatile}") String enrichmentModel,
            @Value("${app.ai.groq.model.enrichment-fallback-model:qwen/qwen3.6-27b}") String enrichmentFallbackModel,
            @Value("${app.ai.groq.model.report-model:qwen/qwen3.6-27b}") String reportModel,
            @Value("${app.ai.groq.model.report-fallback-model:llama-3.3-70b-versatile}") String reportFallbackModel,
            @Value("${app.ai.groq.model.assistant-model:llama-3.1-8b-instant}") String assistantModel,
            @Value("${app.ai.groq.model.assistant-fallback-model:qwen/qwen3.6-27b}") String assistantFallbackModel,
            @Value("${app.ai.groq.model.quick-scan-model:llama-3.1-8b-instant}") String quickScanModel,
            @Value("${app.ai.groq.model.quick-scan-fallback-model:qwen/qwen3.6-27b}") String quickScanFallbackModel,
            @Value("${app.ai.groq.timeout-seconds:30}") int timeoutSeconds,
            @Value("${app.ai.groq.max-tokens:2048}") int maxTokens,
            @Value("${app.ai.groq.temperature:0.2}") double temperature) {
        this.webClient = webClientBuilder.baseUrl(baseUrl).build();
        this.apiKey = apiKey;
        this.primaryModel = primaryModel;
        this.fallbackModel = fallbackModel;
        this.fallback2Model = fallback2Model;
        
        this.enrichmentModel = enrichmentModel;
        this.enrichmentFallbackModel = enrichmentFallbackModel;
        this.reportModel = reportModel;
        this.reportFallbackModel = reportFallbackModel;
        this.assistantModel = assistantModel;
        this.assistantFallbackModel = assistantFallbackModel;
        this.quickScanModel = quickScanModel;
        this.quickScanFallbackModel = quickScanFallbackModel;
        
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

    @Override
    public String complete(String prompt, AiFeature feature) {
        if (apiKey == null || apiKey.isBlank()) {
            log.warn("Groq API key is not configured. Returning placeholder response.");
            return "AI unavailable: API key not configured.";
        }
        
        return switch (feature) {
            case ENRICHMENT -> completeWithFallback(
                prompt,
                enrichmentModel,
                enrichmentFallbackModel
            );
            case REPORT_GENERATION -> completeWithFallback(
                prompt,
                reportModel,
                reportFallbackModel
            );
            case QUICK_SCAN -> completeWithFallback(
                prompt,
                quickScanModel,
                quickScanFallbackModel
            );
            case CHAT -> completeWithFallback(
                prompt,
                assistantModel,
                assistantFallbackModel
            );
        };
    }

    private String completeWithFallback(
            String prompt,
            String primaryModel,
            String fallbackModel) {
        String requestId = UUID.randomUUID().toString();
        long startTime = System.currentTimeMillis();
        
        try {
            log.info("[AI] [{}] Using primary model: {}", requestId, primaryModel);
            return callGroq(requestId, primaryModel, prompt, startTime, 1);
        } catch (Exception ex) {
            log.warn("[AI] [{}] Primary model failed: {}, switching to {}",
                     requestId, primaryModel, fallbackModel, ex);

            try {
                return callGroq(requestId, fallbackModel, prompt, startTime, 2);
            } catch (Exception innerEx) {
                log.error("[AI] [{}] Both models failed. Last error ({}): {}", 
                          requestId, fallbackModel, innerEx.getMessage());
                return "AI unavailable: All selected models failed.";
            }
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

                String content = choice.message().content();
                
                // Strip DeepSeek <think> reasoning tags (including truncated ones)
                content = content.replaceAll("(?s)<think>.*?(?:</think>|$)", "").trim();
                
                // Extremely robust JSON extraction: find first '{' and last '}'
                int firstBrace = content.indexOf('{');
                int lastBrace = content.lastIndexOf('}');
                
                if (firstBrace != -1 && lastBrace != -1 && lastBrace >= firstBrace) {
                    content = content.substring(firstBrace, lastBrace + 1);
                }

                return content;
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
