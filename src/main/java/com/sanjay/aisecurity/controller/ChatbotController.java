package com.sanjay.aisecurity.controller;

import com.sanjay.aisecurity.common.ApiResponse;
import com.sanjay.aisecurity.constants.MessageConstants;
import com.sanjay.aisecurity.dto.request.ChatRequest;
import com.sanjay.aisecurity.dto.response.ChatResponse;
import com.sanjay.aisecurity.service.ChatbotService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Controller for the AI Security Chatbot.
 *
 * @author Sanjay
 * @version 1.0.0
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/chat")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Security Chatbot", description = "Context-aware AI chatbot for security guidance and vulnerability remediation.")
public class ChatbotController {

    private final ChatbotService chatbotService;

    @PostMapping(consumes = org.springframework.http.MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Send a chat message", description = "Sends a message to the AI security assistant.")
    public ResponseEntity<ApiResponse<ChatResponse>> chat(@Valid @RequestBody ChatRequest request) {
        log.info("Chat request received (JSON)");
        ChatResponse response = chatbotService.chat(request);
        return ResponseEntity.ok(ApiResponse.success(MessageConstants.CHAT_RESPONSE_SUCCESS, response));
    }

    @PostMapping(consumes = org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Send a chat message with file", description = "Sends a message to the AI security assistant with a PDF.")
    public ResponseEntity<ApiResponse<ChatResponse>> chatWithFile(
            @RequestParam(value = "message", required = false) String message,
            @RequestParam(value = "conversationId", required = false) String conversationId,
            @RequestParam(value = "scanId", required = false) Long scanId,
            @RequestParam(value = "file", required = true) org.springframework.web.multipart.MultipartFile file) {
        
        log.info("Chat request received (Multipart)");
        ChatRequest request = new ChatRequest();
        request.setMessage(message);
        request.setConversationId(conversationId);
        request.setScanId(scanId);
        
        ChatResponse response = chatbotService.chat(request, file);
        return ResponseEntity.ok(ApiResponse.success(MessageConstants.CHAT_RESPONSE_SUCCESS, response));
    }

    @GetMapping("/conversation/{conversationId}")
    @Operation(summary = "Get conversation history", description = "Retrieves the full ordered message history of a conversation thread.")
    public ResponseEntity<ApiResponse<List<ChatResponse>>> getConversation(
            @PathVariable String conversationId) {
        List<ChatResponse> responses = chatbotService.getConversation(conversationId);
        return ResponseEntity.ok(ApiResponse.success(MessageConstants.SUCCESS, responses));
    }

    @GetMapping("/conversations")
    @Operation(summary = "List my conversations", description = "Returns all conversation thread IDs for the authenticated user.")
    public ResponseEntity<ApiResponse<List<String>>> listConversations() {
        List<String> conversations = chatbotService.listConversations();
        return ResponseEntity.ok(ApiResponse.success(MessageConstants.SUCCESS, conversations));
    }

    @DeleteMapping("/conversation/{conversationId}")
    @Operation(summary = "Delete conversation", description = "Deletes all messages in a conversation thread.")
    public ResponseEntity<ApiResponse<Void>> deleteConversation(@PathVariable String conversationId) {
        chatbotService.deleteConversation(conversationId);
        return ResponseEntity.ok(ApiResponse.success(MessageConstants.CONVERSATION_DELETED));
    }
}
