package com.sanjay.aisecurity.service;

import com.sanjay.aisecurity.dto.request.ChatRequest;
import com.sanjay.aisecurity.dto.response.ChatResponse;

import java.util.List;

/**
 * Service for managing AI chatbot conversations.
 *
 * @author Sanjay
 * @version 1.0.0
 */
public interface ChatbotService {

    /**
     * Processes a user chat message and returns an AI response.
     * Injects scan/vulnerability context when a scanId is provided.
     *
     * @param request the chat request
     * @return the chat response with AI answer
     */
    ChatResponse chat(ChatRequest request, org.springframework.web.multipart.MultipartFile file);
    
    // Kept for backward compatibility if needed, or remove it
    ChatResponse chat(ChatRequest request);

    /**
     * Retrieves the full message history of a conversation thread.
     *
     * @param conversationId the conversation thread ID
     * @return ordered list of chat exchanges
     */
    List<ChatResponse> getConversation(String conversationId);

    /**
     * Lists all distinct conversation IDs for the authenticated user.
     *
     * @return list of conversation IDs
     */
    List<String> listConversations();

    /**
     * Deletes all messages in a conversation thread.
     *
     * @param conversationId the conversation thread ID
     */
    void deleteConversation(String conversationId);
}
