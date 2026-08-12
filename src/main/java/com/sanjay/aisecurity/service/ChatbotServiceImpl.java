package com.sanjay.aisecurity.service;

import com.sanjay.aisecurity.ai.AiProvider;
import com.sanjay.aisecurity.ai.PromptBuilder;
import com.sanjay.aisecurity.ai.SimpleRateLimiter;
import com.sanjay.aisecurity.dto.request.ChatRequest;
import com.sanjay.aisecurity.dto.response.ChatResponse;
import com.sanjay.aisecurity.entity.ChatHistory;
import com.sanjay.aisecurity.entity.ScanHistory;
import com.sanjay.aisecurity.entity.User;
import com.sanjay.aisecurity.entity.Vulnerability;
import com.sanjay.aisecurity.exception.ResourceNotFoundException;
import com.sanjay.aisecurity.repository.ChatHistoryRepository;
import com.sanjay.aisecurity.repository.ScanHistoryRepository;
import com.sanjay.aisecurity.repository.UserRepository;
import com.sanjay.aisecurity.repository.VulnerabilityRepository;
import com.sanjay.aisecurity.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Security Chatbot Service Implementation.
 *
 * <p>Provides context-aware answers by injecting vulnerability findings from the
 * user's scans into the AI prompt. Maintains conversation history for multi-turn sessions.</p>
 *
 * @author Sanjay
 * @version 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChatbotServiceImpl implements ChatbotService {

    private final AiProvider aiProvider;
    private final ChatHistoryRepository chatHistoryRepository;
    private final ScanHistoryRepository scanHistoryRepository;
    private final VulnerabilityRepository vulnerabilityRepository;
    private final UserRepository userRepository;
    private final SimpleRateLimiter rateLimiter;
    private final PdfContextStore pdfContextStore;

    /** Injected from app.ai.groq.model.primary — avoids hardcoding model names in business logic. */
    @org.springframework.beans.factory.annotation.Value("${app.ai.groq.model.primary:qwen/qwen3.6-27b}")
    private String primaryModelName;

    @Override
    @Transactional
    public ChatResponse chat(ChatRequest request) {
        String email = SecurityUtils.requireCurrentUserEmail();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found."));

        // Enforce rate limiting
        rateLimiter.checkLimit(email);

        // Resolve or create conversation ID
        String conversationId = (request.getConversationId() != null && !request.getConversationId().isBlank())
                ? request.getConversationId()
                : UUID.randomUUID().toString();

        // Inject scan history (last N turns)
        List<ChatHistory> history = chatHistoryRepository
                .findByConversationIdAndUserEmailOrderByCreatedAtAsc(conversationId, email);

        // Get scan context if scanId is provided
        String scanContext = "";
        if (request.getScanId() != null) {
            scanContext = buildScanContext(request.getScanId(), email);
        }

        // Re-inject persisted PDF context for this conversation (if any)
        String storedPdfContext = pdfContextStore.get(conversationId);

        // Build the prompt using PromptBuilder — include PDF context for every turn
        String fullPrompt = PromptBuilder.buildChatPrompt(request.getMessage(), scanContext, storedPdfContext, history);

        log.info("Chatbot request from user {} in conversation {}", email, conversationId);
        String aiResponse = aiProvider.complete(fullPrompt);

        // Persist the exchange
        ChatHistory entry = ChatHistory.builder()
                .conversationId(conversationId)
                .userMessage(request.getMessage())
                .aiResponse(aiResponse)
                .provider(aiProvider.getProviderName())
                .model(primaryModelName)
                .user(user)
                .build();

        entry = chatHistoryRepository.save(entry);
        log.info("Saved chat entry ID {} in conversation {}", entry.getId(), conversationId);

        return toResponse(entry);
    }

    // Max characters of PDF text to include in prompt (approx 3000 tokens)
    private static final int MAX_PDF_CHARS = 12000;

    @Override
    @Transactional
    public ChatResponse chat(ChatRequest request, org.springframework.web.multipart.MultipartFile file) {
        String email = SecurityUtils.requireCurrentUserEmail();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found."));

        rateLimiter.checkLimit(email);

        String conversationId = (request.getConversationId() != null && !request.getConversationId().isBlank())
                ? request.getConversationId()
                : UUID.randomUUID().toString();

        List<ChatHistory> history = chatHistoryRepository
                .findByConversationIdAndUserEmailOrderByCreatedAtAsc(conversationId, email);

        String scanContext = "";
        if (request.getScanId() != null) {
            scanContext = buildScanContext(request.getScanId(), email);
        }

        // Extract and truncate PDF text
        String pdfContext = "";
        if (file != null && !file.isEmpty()) {
            try (org.apache.pdfbox.pdmodel.PDDocument document = org.apache.pdfbox.pdmodel.PDDocument.load(file.getInputStream())) {
                org.apache.pdfbox.text.PDFTextStripper stripper = new org.apache.pdfbox.text.PDFTextStripper();
                String rawText = stripper.getText(document);
                if (rawText != null && !rawText.isBlank()) {
                    // Truncate to prevent 413 Payload Too Large from the AI provider
                    if (rawText.length() > MAX_PDF_CHARS) {
                        rawText = rawText.substring(0, MAX_PDF_CHARS) +
                                "\n\n[Note: PDF content was truncated to fit within AI context limits. The above represents the first ~" +
                                MAX_PDF_CHARS + " characters of the document.]";
                    }
                    pdfContext = "\n\n[Context from attached PDF document]:\n" + rawText;
                }
            } catch (java.io.IOException e) {
                log.error("Failed to parse PDF", e);
                throw new RuntimeException("Failed to read PDF file.", e);
            }
        }

        // The original user message (clean, for DB storage and display)
        String originalMessage = request.getMessage() != null ? request.getMessage() : "";

        // Persist PDF context so every follow-up message in this conversation can use it
        if (!pdfContext.isEmpty()) {
            pdfContextStore.save(conversationId, pdfContext);
        }

        // Build prompt with PDF context injected into this first message as well
        String messageForPrompt = originalMessage + pdfContext;
        String fullPrompt = PromptBuilder.buildChatPrompt(messageForPrompt, scanContext, pdfContext, history);

        log.info("Chatbot request from user {} in conversation {} (PDF attached: {})",
                email, conversationId, !pdfContext.isEmpty());
        String aiResponse = aiProvider.complete(fullPrompt);

        // Save the clean original message to DB, not the PDF-stuffed version
        ChatHistory entry = ChatHistory.builder()
                .conversationId(conversationId)
                .userMessage(originalMessage)
                .aiResponse(aiResponse)
                .provider(aiProvider.getProviderName())
                .model(primaryModelName)
                .user(user)
                .build();

        entry = chatHistoryRepository.save(entry);
        log.info("Saved chat entry ID {} in conversation {}", entry.getId(), conversationId);

        return toResponse(entry);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ChatResponse> getConversation(String conversationId) {
        String email = SecurityUtils.requireCurrentUserEmail();
        return chatHistoryRepository
                .findByConversationIdAndUserEmailOrderByCreatedAtAsc(conversationId, email)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<String> listConversations() {
        String email = SecurityUtils.requireCurrentUserEmail();
        return chatHistoryRepository.findDistinctConversationIdsByUserEmail(email);
    }

    @Override
    @Transactional
    public void deleteConversation(String conversationId) {
        String email = SecurityUtils.requireCurrentUserEmail();
        chatHistoryRepository.deleteByConversationIdAndUserEmail(conversationId, email);
        // Clean up any stored PDF context for this conversation
        pdfContextStore.remove(conversationId);
        log.info("Deleted conversation {} for user {}", conversationId, email);
    }

    // =========================================================================
    // HELPER METHODS
    // =========================================================================

    private String buildScanContext(Long scanId, String email) {
        ScanHistory scan = scanHistoryRepository.findById(scanId).orElse(null);
        if (scan == null || !scan.getProject().getUser().getEmail().equals(email)) {
            return "";
        }

        List<Vulnerability> vulns = vulnerabilityRepository.findByScanHistoryId(scanId);
        if (vulns.isEmpty()) {
            return "--- Scan Context: No vulnerabilities found in scan #" + scanId + " ---\n";
        }

        StringBuilder ctx = new StringBuilder();
        ctx.append("--- Security Scan Context (Scan #").append(scanId).append(") ---\n");
        ctx.append("Project: ").append(scan.getProject().getName()).append("\n");
        ctx.append("Security Score: ").append(scan.getSecurityScore()).append(" / 100\n");
        ctx.append("Total Vulnerabilities: ").append(vulns.size()).append("\n");
        ctx.append("Vulnerabilities Found:\n");

        // Include up to 10 vulnerabilities to avoid prompt explosion
        vulns.stream().limit(10).forEach(v ->
                ctx.append("  - [").append(v.getSeverity().name()).append("] ")
                        .append(v.getVulnerabilityType())
                        .append(" in ").append(v.getFileName())
                        .append(" at line ").append(v.getLineNumber())
                        .append(": ").append(v.getDescription())
                        .append("\n")
        );
        if (vulns.size() > 10) {
            ctx.append("  ... and ").append(vulns.size() - 10).append(" more.\n");
        }
        ctx.append("---\n\n");
        return ctx.toString();
    }



    private ChatResponse toResponse(ChatHistory h) {
        return ChatResponse.builder()
                .id(h.getId())
                .conversationId(h.getConversationId())
                .userMessage(h.getUserMessage())
                .aiResponse(h.getAiResponse())
                .provider(h.getProvider())
                .model(h.getModel())
                .createdAt(h.getCreatedAt())
                .build();
    }
}
