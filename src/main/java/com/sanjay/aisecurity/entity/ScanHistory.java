package com.sanjay.aisecurity.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.sanjay.aisecurity.enums.ScanStatus;
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
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * ScanHistory Entity.
 *
 * <p>Represents a security scan instance on a project. Tracks processing status,
 * scan duration, number of scanned files, and resulting calculated security score.</p>
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
    name = "scan_history",
    indexes = {
        @Index(name = "idx_scan_project", columnList = "project_id"),
        @Index(name = "idx_scan_status", columnList = "status")
    }
)
public class ScanHistory extends BaseEntity {

    @Column(name = "scan_start")
    private LocalDateTime scanStart;

    @Column(name = "scan_end")
    private LocalDateTime scanEnd;

    @Column(name = "duration")
    private long duration; // in milliseconds

    @Builder.Default
    @NotNull(message = "Scan type is required")
    @Enumerated(EnumType.STRING)
    @Column(name = "scan_type", nullable = false, length = 20)
    private com.sanjay.aisecurity.enums.ScanType scanType = com.sanjay.aisecurity.enums.ScanType.PROJECT;

    @Column(name = "snippet_language", length = 50)
    private String snippetLanguage;

    @Column(name = "snippet_filename", length = 255)
    private String snippetFilename;

    @Column(name = "snippet_lines")
    private Integer snippetLines;

    @Builder.Default
    @Column(name = "progress_percentage", nullable = false)
    private int progressPercentage = 0;

    @Builder.Default
    @NotNull(message = "Scan status is required")
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private ScanStatus status = ScanStatus.PENDING;

    @Builder.Default
    @Column(name = "scanned_files", nullable = false)
    private int scannedFiles = 0;

    @Builder.Default
    @Column(name = "total_files", nullable = false)
    private int totalFiles = 0;

    @Builder.Default
    @Column(name = "total_vulnerabilities", nullable = false)
    private int totalVulnerabilities = 0;

    @Builder.Default
    @Column(name = "security_score", nullable = false)
    private double securityScore = 100.0;

    @Column(name = "ai_summary", columnDefinition = "TEXT")
    private String aiSummary;

    /**
     * JSON snapshot of the {@code ScanConfigurationDto} used for this scan.
     * Stored as TEXT to allow full audit, rescan, and configuration comparison.
     * Populated by {@code ScanServiceImpl} before the async pipeline starts.
     */
    @Column(name = "configuration_json", columnDefinition = "TEXT")
    private String configurationJson;

    @NotNull(message = "Project is required")
    @ManyToOne(fetch = FetchType.EAGER)
    @com.fasterxml.jackson.annotation.JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "uploadedFiles", "scanHistories", "reports"})
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @Builder.Default
    @JsonIgnore
    @OneToMany(mappedBy = "scanHistory", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Vulnerability> vulnerabilities = new ArrayList<>();
}
