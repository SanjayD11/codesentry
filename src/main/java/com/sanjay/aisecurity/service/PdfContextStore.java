package com.sanjay.aisecurity.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Application-scoped store for PDF context per conversation.
 *
 * <p>When a user uploads a PDF in a conversation, the extracted (and truncated)
 * text is stored here keyed by conversationId. Every subsequent message in the
 * same conversation retrieves this context and re-injects it into the AI prompt,
 * ensuring the AI retains full awareness of the document throughout the session.</p>
 *
 * @author Sanjay
 * @version 1.0.0
 */
@Slf4j
@Component
public class PdfContextStore {

    private final ConcurrentHashMap<String, String> store = new ConcurrentHashMap<>();

    /**
     * Save PDF context for the given conversation.
     */
    public void save(String conversationId, String pdfContext) {
        store.put(conversationId, pdfContext);
        log.info("[PdfContextStore] Stored PDF context for conversation {} ({} chars)",
                conversationId, pdfContext.length());
    }

    /**
     * Retrieve the stored PDF context for a conversation, or empty string if none.
     */
    public String get(String conversationId) {
        return store.getOrDefault(conversationId, "");
    }

    /**
     * Remove the PDF context (e.g. when conversation is deleted).
     */
    public void remove(String conversationId) {
        store.remove(conversationId);
    }

    /**
     * Returns true if PDF context exists for this conversation.
     */
    public boolean has(String conversationId) {
        return store.containsKey(conversationId);
    }
}
