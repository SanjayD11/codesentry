package com.sanjay.aisecurity.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
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
 * Notification Entity.
 *
 * <p>Represents a system or security alert generated for a specific User.
 * Tracks if the notification has been read by the user.</p>
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
    name = "notifications",
    indexes = {
        @Index(name = "idx_notification_user", columnList = "user_id"),
        @Index(name = "idx_notification_read", columnList = "is_read")
    }
)
public class Notification extends BaseEntity {

    @NotBlank(message = "Notification title is required")
    @Size(max = 150, message = "Title must be less than 150 characters")
    @Column(name = "title", nullable = false, length = 150)
    private String title;

    @NotBlank(message = "Notification message is required")
    @Size(max = 1000, message = "Message must be less than 1000 characters")
    @Column(name = "message", nullable = false, length = 1000)
    private String message;

    @NotBlank(message = "Notification type is required")
    @Size(max = 50, message = "Type must be less than 50 characters")
    @Column(name = "type", nullable = false, length = 50)
    private String type;

    @Builder.Default
    @Column(name = "is_read", nullable = false)
    private boolean isRead = false;

    @NotNull(message = "User is required")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
}
