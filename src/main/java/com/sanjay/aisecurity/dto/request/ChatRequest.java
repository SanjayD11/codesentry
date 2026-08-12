package com.sanjay.aisecurity.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Request DTO for chatbot messages.
 *
 * @author Sanjay
 * @version 1.0.0
 */
@Data
public class ChatRequest {

    @NotBlank(message = "Message cannot be blank")
    @Size(max = 4000, message = "Message must not exceed 4000 characters")
    private String message;

    /**
     * Optional conversation ID for multi-turn thread continuity.
     * If null, a new conversation thread is created.
     */
    private String conversationId;

    /**
     * Optional scan ID to provide scan-specific vulnerability context.
     */
    private Long scanId;
}
