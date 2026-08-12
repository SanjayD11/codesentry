package com.sanjay.aisecurity.service;

import com.sanjay.aisecurity.dto.response.FileMetadataResponse;
import com.sanjay.aisecurity.dto.response.UploadFileResponse;
import com.sanjay.aisecurity.dto.response.UploadProgressResponse;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * Service interface for handling file uploads, validation, and metadata extraction.
 *
 * @author Sanjay
 * @version 1.0.0
 */
public interface UploadService {

    /**
     * Uploads and stores multiple files for a project.
     *
     * @param projectId the target project ID
     * @param files     array of multipart files
     * @return list of upload responses
     */
    List<UploadFileResponse> uploadFiles(Long projectId, MultipartFile[] files);

    /**
     * Uploads and stores multiple files for a project, with duplicate override control.
     *
     * <p><b>Duplicate detection</b>: if a file with an identical SHA-256 already exists in the
     * project and {@code overrideDuplicate} is {@code false}, a response with
     * {@code success=false} and {@code message="DUPLICATE_FILE_DETECTED"} is returned so the
     * caller can surface a confirmation prompt to the user.</p>
     *
     * <p><b>Immutable audit log</b>: when {@code overrideDuplicate} is {@code true}, a
     * <em>brand-new</em> {@code UploadedFile} record is always created with its own ID,
     * timestamp, and storage path — even if the SHA-256 is identical to an earlier upload.
     * Existing records are <strong>never</strong> modified, ensuring every upload event is an
     * independent, immutable row in the audit trail.</p>
     *
     * @param projectId         the target project ID
     * @param files             array of multipart files
     * @param overrideDuplicate when {@code true}, bypass the duplicate gate and always create
     *                          a new upload record
     * @return list of upload responses
     */
    List<UploadFileResponse> uploadFiles(Long projectId, MultipartFile[] files, boolean overrideDuplicate);

    /**
     * Retrieves paginated active files for a specific project.
     *
     * @param projectId the project ID
     * @param pageable  pagination details
     * @return page of file metadata
     */
    Page<FileMetadataResponse> getProjectFiles(Long projectId, Pageable pageable);

    /**
     * Retrieves metadata for a specific file.
     *
     * @param fileId the file ID
     * @return file metadata
     */
    FileMetadataResponse getFileMetadata(Long fileId);

    /**
     * Downloads the physical file.
     *
     * @param fileId the file ID
     * @return resource representation of the file
     */
    Resource downloadFile(Long fileId);

    /**
     * Soft deletes an uploaded file.
     *
     * @param fileId the file ID
     */
    void deleteFile(Long fileId);

    /**
     * Restores a soft-deleted file.
     *
     * @param fileId the file ID
     */
    void restoreFile(Long fileId);

    /**
     * Searches for files by original filename.
     *
     * @param filename the search query
     * @param pageable pagination details
     * @return page of matching file metadata
     */
    Page<FileMetadataResponse> searchFiles(String filename, Pageable pageable);

    /**
     * Retrieves aggregated statistics for all files owned by the authenticated user.
     *
     * @return statistics response
     */
    UploadProgressResponse getStatistics();

    /**
     * Securely deletes all physical files belonging to a project from the filesystem and marks
     * their database records as deleted.
     *
     * <p>This method is called automatically at the end of every scan (success, failure, or
     * timeout) to ensure that user source code is never persisted beyond the duration needed for
     * scanning. Database metadata (filename, checksum, upload time) is intentionally preserved for
     * audit purposes; only the physical file content is wiped.</p>
     *
     * @param projectId the project whose uploaded files should be securely deleted
     */
    void securelyDeleteProjectFiles(Long projectId);
}

