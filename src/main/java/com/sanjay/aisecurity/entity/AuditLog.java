package com.sanjay.aisecurity.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Setter;

/**
 * AuditLog Entity.
 *
 * <p>Persists a secure audit trail of security-sensitive operations
 * performed within the platform (e.g. log in, file uploads, scan starts,
 * user status updates).</p>
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
    name = "audit_logs",
    indexes = {
        @Index(name = "idx_audit_user", columnList = "user_id"),
        @Index(name = "idx_audit_action", columnList = "action"),
        @Index(name = "idx_audit_created", columnList = "created_at")
    }
)
public class AuditLog extends BaseEntity {

    @NotBlank(message = "Audit action is required")
    @Size(max = 100, message = "Action must be less than 100 characters")
    @Column(name = "action", nullable = false, length = 100)
    private String action;

    @Size(max = 100, message = "Resource must be less than 100 characters")
    @Column(name = "resource", length = 100)
    private String resource;

    @Size(max = 2000, message = "Details must be less than 2000 characters")
    @Column(name = "details", length = 2000)
    private String details;

    @Size(max = 45, message = "IP Address must be less than 45 characters (supports IPv6)")
    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    /** SUCCESS or FAILURE — outcome of the audited action. */
    @Builder.Default
    @Size(max = 20)
    @Column(name = "status", length = 20)
    private String status = "SUCCESS";

    /** Browser/client user-agent string (sanitized, no personal data). */
    @Size(max = 500)
    @Column(name = "user_agent", length = 500)
    private String userAgent;

    /** ID of the resource/user that was targeted by the action. */
    @Column(name = "target_user_id")
    private Long targetUserId;

    /** Email of the resource/user that was targeted by the action. */
    @Size(max = 100)
    @Column(name = "target_user_email", length = 100)
    private String targetUserEmail;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = true)
    @com.fasterxml.jackson.annotation.JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "password", "projects", "scans", "reports", "auditLogs"})
    private User user;
}
