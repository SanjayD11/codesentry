package com.sanjay.aisecurity.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

/**
 * ChatHistory Entity.
 *
 * <p>Represents a single exchange (question and answer) between a User and the
 * AI chatbot. Grouped into threads by a conversationId.</p>
 *
 * @author Sanjay
 * @version 1.0.0
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(
    name = "chat_history",
    indexes = {
        @Index(name = "idx_chat_user", columnList = "user_id"),
        @Index(name = "idx_chat_conversation", columnList = "conversation_id")
    }
)
public class ChatHistory extends BaseEntity {

    @NotBlank(message = "Conversation ID is required")
    @Size(max = 50, message = "Conversation ID must be less than 50 characters")
    @Column(name = "conversation_id", nullable = false, length = 50)
    private String conversationId;

    @NotBlank(message = "User message is required")
    @Lob
    @Column(name = "user_message", nullable = false, columnDefinition = "LONGTEXT")
    private String userMessage;

    @NotBlank(message = "AI response is required")
    @Lob
    @Column(name = "ai_response", nullable = false, columnDefinition = "LONGTEXT")
    private String aiResponse;

    @Size(max = 50, message = "Provider name must be less than 50 characters")
    @Column(name = "provider", length = 50)
    private String provider;

    @Size(max = 50, message = "Model name must be less than 50 characters")
    @Column(name = "model", length = 50)
    private String model;

    @NotNull(message = "User is required")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
}
