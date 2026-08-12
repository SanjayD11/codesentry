package com.sanjay.aisecurity.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.sanjay.aisecurity.enums.ProjectType;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Project Entity.
 *
 * <p>Represents a target software repository or project container owned by a User.
 * Serves as the parent scope for uploaded source files, vulnerability scans,
 * and security PDF reports.</p>
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
    name = "projects",
    indexes = {
        @Index(name = "idx_project_name", columnList = "name"),
        @Index(name = "idx_project_user", columnList = "user_id")
    }
)
public class Project extends BaseEntity {

    @NotBlank(message = "Project name is required")
    @Size(max = 100, message = "Project name must be less than 100 characters")
    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Size(max = 500, message = "Description must be less than 500 characters")
    @Column(name = "description", length = 500)
    private String description;

    @Size(max = 20, message = "Version must be less than 20 characters")
    @Column(name = "version", length = 20)
    private String version;

    @NotNull(message = "Project type is required")
    @Enumerated(EnumType.STRING)
    @Column(name = "project_type", nullable = false, length = 20)
    private ProjectType projectType;

    @Builder.Default
    @Column(name = "status", nullable = false, length = 20)
    private String status = "ACTIVE";

    @Builder.Default
    @Column(name = "active", nullable = false)
    private boolean active = true;

    @Builder.Default
    @Column(name = "total_files", nullable = false)
    private int totalFiles = 0;

    @Builder.Default
    @Column(name = "security_score", nullable = false)
    private double securityScore = 100.0;

    @Column(name = "last_scan")
    private LocalDateTime lastScan;

    @NotNull(message = "User is required")
    @ManyToOne(fetch = FetchType.EAGER)
    @com.fasterxml.jackson.annotation.JsonIgnoreProperties({"projects", "chatHistories", "notifications", "auditLogs", "password", "handler", "hibernateLazyInitializer"})
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Builder.Default
    @JsonIgnore
    @OneToMany(mappedBy = "project", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    private List<UploadedFile> uploadedFiles = new ArrayList<>();

    @Builder.Default
    @JsonIgnore
    @OneToMany(mappedBy = "project", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ScanHistory> scanHistories = new ArrayList<>();

    @Builder.Default
    @JsonIgnore
    @OneToMany(mappedBy = "project", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Report> reports = new ArrayList<>();
}
