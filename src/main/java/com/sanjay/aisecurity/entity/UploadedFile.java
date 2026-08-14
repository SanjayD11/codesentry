package com.sanjay.aisecurity.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
 * UploadedFile Entity.
 *
 * <p>Represents an individual source file or config file uploaded as part
 * of a project. Tracks file integrity via SHA-256 and records internal storage paths.</p>
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
    name = "uploaded_files",
    indexes = {
        @Index(name = "idx_uploaded_file_project", columnList = "project_id"),
        @Index(name = "idx_uploaded_file_hash", columnList = "checksum_sha256")
    }
)
public class UploadedFile extends BaseEntity {

    @NotBlank(message = "Original filename is required")
    @Size(max = 255, message = "Original filename must be less than 255 characters")
    @Column(name = "original_filename", nullable = false)
    private String originalFileName;

    @NotBlank(message = "Stored filename is required")
    @Size(max = 255, message = "Stored filename must be less than 255 characters")
    @Column(name = "stored_filename", nullable = false, unique = true)
    private String storedFileName;

    @NotBlank(message = "Extension is required")
    @Size(max = 20, message = "Extension must be less than 20 characters")
    @Column(name = "file_extension", nullable = false, length = 20)
    private String fileExtension;

    @NotBlank(message = "MIME type is required")
    @Size(max = 100, message = "MIME type must be less than 100 characters")
    @Column(name = "mime_type", nullable = false, length = 100)
    private String mimeType;

    @Column(name = "file_size", nullable = false)
    private long fileSize;

    @NotBlank(message = "Storage path is required")
    @Size(max = 500, message = "Storage path must be less than 500 characters")
    @Column(name = "storage_path", nullable = false, length = 500)
    private String storagePath;

    @NotBlank(message = "Absolute path is required")
    @Size(max = 500, message = "Absolute path must be less than 500 characters")
    @Column(name = "absolute_path", nullable = false, length = 500)
    private String absolutePath;

    @NotBlank(message = "SHA-256 hash is required")
    @Size(min = 64, max = 64, message = "SHA-256 hash must be exactly 64 characters")
    @Column(name = "checksum_sha256", nullable = false, length = 64)
    private String checksumSHA256;

    @NotNull(message = "Upload status is required")
    @Enumerated(EnumType.STRING)
    @Column(name = "upload_status", nullable = false, length = 20)
    private com.sanjay.aisecurity.enums.UploadStatus uploadStatus;

    @NotNull(message = "Scan status is required")
    @Enumerated(EnumType.STRING)
    @Column(name = "scan_status", nullable = false, length = 20)
    private com.sanjay.aisecurity.enums.ScanStatus scanStatus;

    @Builder.Default
    @Column(name = "is_deleted", nullable = false)
    private boolean isDeleted = false;

    @Column(name = "uploaded_at", nullable = false)
    private LocalDateTime uploadedAt;

    @NotNull(message = "Project is required")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @NotNull(message = "Uploader is required")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "uploaded_by_id", nullable = false)
    private User uploadedBy;
}
