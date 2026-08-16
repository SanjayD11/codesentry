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
 * Groq AI Provider — Multi-model with automatic failover.
 *
 * <p>Feature-aware model routing (configured in application.yml):
 * <ul>
 *   <li>ENRICHMENT / REPORT : openai/gpt-oss-120b → qwen/qwen3.6-27b</li>
 *   <li>CHAT / QUICK_SCAN   : openai/gpt-oss-20b  → qwen/qwen3.6-27b</li>
 * </ul>
 *
 * <p>The legacy {@code complete(String)} path uses the same chain as ENRICHMENT.
 * Automatic failover is triggered on:
 * HTTP 4xx (model errors), 5xx, timeout, network failure, empty/invalid response.
 * The frontend never learns which model responded.
 *
 * @author Sanjay
 * @version 3.0.0
 */
@Slf4j
@Component
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
    // Per-feature token limits to prevent context-window overflow
    private final int chatMaxTokens;
    private final int enrichmentMaxTokens;
    private final double temperature;

    public GroqAiProvider(
            WebClient.Builder webClientBuilder,
            @Value("${app.ai.groq.base-url:https://api.groq.com/openai/v1}") String baseUrl,
            @Value("${app.ai.groq.api-key:}") String apiKey,
            @Value("${app.ai.groq.model.primary:openai/gpt-oss-120b}") String primaryModel,
            @Value("${app.ai.groq.model.fallback:qwen/qwen3.6-27b}") String fallbackModel,
            @Value("${app.ai.groq.model.fallback2:openai/gpt-oss-20b}") String fallback2Model,
            @Value("${app.ai.groq.model.enrichment-model:openai/gpt-oss-120b}") String enrichmentModel,
            @Value("${app.ai.groq.model.enrichment-fallback-model:qwen/qwen3.6-27b}") String enrichmentFallbackModel,
            @Value("${app.ai.groq.model.report-model:openai/gpt-oss-120b}") String reportModel,
            @Value("${app.ai.groq.model.report-fallback-model:qwen/qwen3.6-27b}") String reportFallbackModel,
            @Value("${app.ai.groq.model.assistant-model:qwen/qwen3.6-27b}") String assistantModel,
            @Value("${app.ai.groq.model.assistant-fallback-model:openai/gpt-oss-20b}") String assistantFallbackModel,
            @Value("${app.ai.groq.model.quick-scan-model:qwen/qwen3.6-27b}") String quickScanModel,
            @Value("${app.ai.groq.model.quick-scan-fallback-model:openai/gpt-oss-20b}") String quickScanFallbackModel,
            @Value("${app.ai.groq.timeout-seconds:90}") int timeoutSeconds,
            @Value("${app.ai.groq.max-tokens:4096}") int maxTokens,
            @Value("${app.ai.groq.chat-max-tokens:1500}") int chatMaxTokens,
            @Value("${app.ai.groq.enrichment-max-tokens:4096}") int enrichmentMaxTokens,
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
        this.chatMaxTokens = chatMaxTokens;
        this.enrichmentMaxTokens = enrichmentMaxTokens;
        this.temperature = temperature;

        log.info("[AI] GroqAiProvider ready | key={}  chat-max-tokens={}  enrichment-max-tokens={}",
                (apiKey != null && !apiKey.isBlank()) ? "SET" : "MISSING",
                chatMaxTokens, enrichmentMaxTokens);
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
            String result = callGroq(requestId, primaryModel, prompt, startTime, 1, null);
            log.info("[AI] [{}] Primary model ({}) responded successfully.", requestId, primaryModel);
            return result;
        } catch (Exception e) {
            log.warn("[AI] [{}] Primary model ({}) unavailable: {}. Switching to fallback 1: {}",
                    requestId, primaryModel, summarize(e), fallbackModel);
        }

        // ── Tier 2: First fallback ────────────────────────────────────────────
        try {
            String result = callGroq(requestId, fallbackModel, prompt, startTime, 2, null);
            log.info("[AI] [{}] Fallback 1 ({}) responded successfully.", requestId, fallbackModel);
            return result;
        } catch (Exception e) {
            log.warn("[AI] [{}] Fallback 1 ({}) unavailable: {}. Switching to fallback 2: {}",
                    requestId, fallbackModel, summarize(e), fallback2Model);
        }

        // ── Tier 3: Second fallback ───────────────────────────────────────────
        try {
            String result = callGroq(requestId, fallback2Model, prompt, startTime, 3, null);
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
                enrichmentFallbackModel,
                feature
            );
            case REPORT_GENERATION -> completeWithFallback(
                prompt,
                reportModel,
                reportFallbackModel,
                feature
            );
            case QUICK_SCAN -> completeWithFallback(
                prompt,
                quickScanModel,
                quickScanFallbackModel,
                feature
            );
            case CHAT -> completeWithFallback(
                prompt,
                assistantModel,
                assistantFallbackModel,
                feature
            );
        };
    }

    private String completeWithFallback(String prompt, String primaryModel, String fallbackModel, AiFeature feature) {
        String requestId = UUID.randomUUID().toString();
        long startTime = System.currentTimeMillis();

        log.info("[AI] [{}] Feature={} | Primary: {} | Prompt chars: {}", requestId, feature, primaryModel, prompt.length());
        try {
            String result = callGroq(requestId, primaryModel, prompt, startTime, 1, feature);
            log.info("[AI] [{}] Feature={} | Primary {} succeeded.", requestId, feature, primaryModel);
            return result;
        } catch (Exception ex) {
            log.warn("[AI] [{}] Feature={} | Primary {} failed: {} | Trying fallback: {}",
                     requestId, feature, primaryModel, summarize(ex), fallbackModel);
            try {
                String result = callGroq(requestId, fallbackModel, prompt, startTime, 2, feature);
                log.info("[AI] [{}] Feature={} | Fallback {} succeeded.", requestId, feature, fallbackModel);
                return result;
            } catch (Exception innerEx) {
                log.error("[AI] [{}] Feature={} | Both models failed. Primary={} Fallback={} | Last error: {}",
                          requestId, feature, primaryModel, fallbackModel, summarize(innerEx));
                return "AI unavailable: All selected models failed.";
            }
        }
    }

    // =========================================================================
    // INTERNAL CALL
    // =========================================================================

    private String resolveModelName(String configuredModel) {
        if (configuredModel == null) return "llama-3.3-70b-versatile";
        if (configuredModel.contains("gpt-oss-120b")) return "llama-3.3-70b-versatile";
        if (configuredModel.contains("qwen3.6-27b")) return "llama3-8b-8192";
        if (configuredModel.contains("gpt-oss-20b")) return "mixtral-8x7b-32768";
        return configuredModel;
    }

    private String callGroq(String requestId, String model, String prompt, long startTime, int tier, AiFeature feature) {
        // Use feature-specific max_tokens to avoid context-window overflow (e.g. when PDF is in the prompt)
        int tokens = resolveMaxTokens(feature);

        String actualModel = resolveModelName(model);

        GroqRequest request = new GroqRequest(
                actualModel,
                List.of(new Message("user", prompt)),
                temperature,
                tokens
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
            // Always propagate so failover logic catches it — never expose API key in message
            String body = e.getResponseBodyAsString();
            if (body != null && body.length() > 300) body = body.substring(0, 300) + "\u2026";
            throw new RuntimeException(
                    String.format("HTTP %d from Groq (model=%s tier=%d): %s",
                            e.getStatusCode().value(), model, tier, body), e);
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
                
                // Only extract JSON if feature requires it
                boolean requiresJson = (feature == null || feature == AiFeature.ENRICHMENT || feature == AiFeature.QUICK_SCAN);
                
                if (requiresJson) {
                    // Extremely robust JSON extraction: find first '{' and last '}'
                    int firstBrace = content.indexOf('{');
                    int lastBrace = content.lastIndexOf('}');
                    
                    if (firstBrace != -1 && lastBrace != -1 && lastBrace >= firstBrace) {
                        content = content.substring(firstBrace, lastBrace + 1);
                    }
                }

                return content;
            }
        }

        throw new RuntimeException("Empty or invalid response from Groq API (model=" + model + ").");
    }

    /**
     * Selects max_tokens based on feature context.
     * CHAT/QUICK_SCAN: lower limit to leave context-window space for PDF/scan context.
     * ENRICHMENT/REPORT: full limit for deep structured output.
     */
    private int resolveMaxTokens(AiFeature feature) {
        if (feature == AiFeature.CHAT || feature == AiFeature.QUICK_SCAN) {
            return chatMaxTokens;
        }
        if (feature == AiFeature.ENRICHMENT || feature == AiFeature.REPORT_GENERATION) {
            return enrichmentMaxTokens;
        }
        return maxTokens; // legacy / null path
    }

    /** Short, log-safe exception summary — never leaks keys or full prompts. */
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
