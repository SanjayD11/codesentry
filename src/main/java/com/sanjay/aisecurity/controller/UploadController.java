package com.sanjay.aisecurity.controller;

import com.sanjay.aisecurity.common.ApiResponse;
import com.sanjay.aisecurity.constants.MessageConstants;
import com.sanjay.aisecurity.dto.response.FileMetadataResponse;
import com.sanjay.aisecurity.dto.response.UploadFileResponse;
import com.sanjay.aisecurity.dto.response.UploadProgressResponse;
import com.sanjay.aisecurity.service.UploadService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

/**
 * Controller for file upload and storage management.
 *
 * @author Sanjay
 * @version 1.0.0
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/uploads")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "File Uploads", description = "Endpoints for uploading and managing source code and configuration files.")
public class UploadController {

    private final UploadService uploadService;

    @PostMapping(value = "/{projectId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(
            summary = "Upload files to project",
            description = "Uploads one or more files to the specified project. " +
                    "If a file with identical content already exists, a DUPLICATE_FILE_DETECTED message is returned " +
                    "unless overrideDuplicate=true, in which case the file is re-uploaded and treated as a fresh entry.")
    public ResponseEntity<ApiResponse<List<UploadFileResponse>>> uploadFiles(
            @PathVariable Long projectId,
            @RequestParam(value = "files", required = false) MultipartFile[] files,
            @RequestParam(value = "overrideDuplicate", defaultValue = "false") boolean overrideDuplicate) {
        try {
            if (files == null || files.length == 0) {
                log.warn("Upload request to project {} contained no files", projectId);
                return ResponseEntity.badRequest()
                        .body(ApiResponse.badRequest("No files provided. Please select at least one file to upload."));
            }
            log.info("Received request to upload {} file(s) to project {} (overrideDuplicate={})",
                    files.length, projectId, overrideDuplicate);
            List<UploadFileResponse> responses = uploadService.uploadFiles(projectId, files, overrideDuplicate);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.created("Files processed", responses));
        } catch (Exception e) {
            log.error("Upload failed for project {}: {}", projectId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.internalError("Upload failed: " + e.getMessage()));
        }
    }

    @GetMapping("/project/{projectId}")
    @Operation(summary = "Get project files", description = "Retrieves a paginated list of active files in a project.")
    public ResponseEntity<ApiResponse<Page<FileMetadataResponse>>> getProjectFiles(
            @PathVariable Long projectId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "uploadedAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {
        
        Pageable pageable = buildPageable(page, size, sortBy, sortDir);
        Page<FileMetadataResponse> response = uploadService.getProjectFiles(projectId, pageable);
        return ResponseEntity.ok(ApiResponse.success(MessageConstants.SUCCESS, response));
    }

    @GetMapping("/{fileId}")
    @Operation(summary = "Get file metadata", description = "Retrieves detailed metadata for a specific file.")
    public ResponseEntity<ApiResponse<FileMetadataResponse>> getFileMetadata(
            @PathVariable Long fileId) {
        FileMetadataResponse response = uploadService.getFileMetadata(fileId);
        return ResponseEntity.ok(ApiResponse.success(MessageConstants.SUCCESS, response));
    }

    @GetMapping("/download/{fileId}")
    @Operation(summary = "Download file", description = "Securely downloads the physical file content.")
    public ResponseEntity<Resource> downloadFile(@PathVariable Long fileId) {
        FileMetadataResponse metadata = uploadService.getFileMetadata(fileId);
        Resource resource = uploadService.downloadFile(fileId);

        String contentType = metadata.getMimeType();
        if (contentType == null) {
            contentType = MediaType.APPLICATION_OCTET_STREAM_VALUE;
        }

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + metadata.getOriginalFileName() + "\"")
                .body(resource);
    }

    @DeleteMapping("/{fileId}")
    @Operation(summary = "Soft delete file", description = "Marks a file as deleted without physically removing it.")
    public ResponseEntity<ApiResponse<Void>> deleteFile(@PathVariable Long fileId) {
        uploadService.deleteFile(fileId);
        return ResponseEntity.ok(ApiResponse.success("File soft deleted successfully"));
    }

    @PutMapping("/restore/{fileId}")
    @Operation(summary = "Restore file", description = "Restores a soft-deleted file.")
    public ResponseEntity<ApiResponse<Void>> restoreFile(@PathVariable Long fileId) {
        uploadService.restoreFile(fileId);
        return ResponseEntity.ok(ApiResponse.success("File restored successfully"));
    }

    @GetMapping("/search")
    @Operation(summary = "Search files", description = "Searches active files by original filename.")
    public ResponseEntity<ApiResponse<Page<FileMetadataResponse>>> searchFiles(
            @RequestParam String filename,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "uploadedAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {
        
        Pageable pageable = buildPageable(page, size, sortBy, sortDir);
        Page<FileMetadataResponse> response = uploadService.searchFiles(filename, pageable);
        return ResponseEntity.ok(ApiResponse.success(MessageConstants.SUCCESS, response));
    }

    @GetMapping("/statistics")
    @Operation(summary = "Get upload statistics", description = "Retrieves storage usage and file count statistics.")
    public ResponseEntity<ApiResponse<UploadProgressResponse>> getStatistics() {
        UploadProgressResponse response = uploadService.getStatistics();
        return ResponseEntity.ok(ApiResponse.success(MessageConstants.SUCCESS, response));
    }

    // =========================================================================
    // PRIVATE HELPERS
    // =========================================================================

    private Pageable buildPageable(int page, int size, String sortBy, String sortDir) {
        int clampedSize = Math.min(size, 100);
        Sort.Direction direction = "asc".equalsIgnoreCase(sortDir) ? Sort.Direction.ASC : Sort.Direction.DESC;
        return PageRequest.of(Math.max(page, 0), clampedSize, Sort.by(direction, sortBy));
    }
}
