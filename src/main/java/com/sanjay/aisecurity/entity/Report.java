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

import java.time.LocalDateTime;

/**
 * Report Entity.
 *
 * <p>Represents a security assessment report generated in PDF or another format.
 * Tracks report metadata and file system path where the document is persisted.</p>
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
    name = "reports",
    indexes = {
        @Index(name = "idx_report_project", columnList = "project_id"),
        @Index(name = "idx_report_type", columnList = "report_type")
    }
)
public class Report extends BaseEntity {

    @NotBlank(message = "Report name is required")
    @Size(max = 255, message = "Report name must be less than 255 characters")
    @Column(name = "report_name", nullable = false)
    private String reportName;

    @NotBlank(message = "Report path is required")
    @Size(max = 500, message = "Report path must be less than 500 characters")
    @Column(name = "report_path", nullable = false, length = 500)
    private String reportPath;

    @Builder.Default
    @NotBlank(message = "Report type is required")
    @Size(max = 20, message = "Report type must be less than 20 characters")
    @Column(name = "report_type", nullable = false, length = 20)
    private String reportType = "PDF";

    @Column(name = "report_size", nullable = false)
    private long reportSize;

    @Column(name = "generated_at", nullable = false)
    private LocalDateTime generatedAt;

    @Column(name = "scan_history_id")
    private Long scanHistoryId;

    @Column(name = "generated_by", length = 255)
    private String generatedBy;

    @NotNull(message = "Project is required")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;
}
