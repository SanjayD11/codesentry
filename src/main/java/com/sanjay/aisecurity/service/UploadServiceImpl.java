package com.sanjay.aisecurity.service;

import com.sanjay.aisecurity.constants.MessageConstants;
import com.sanjay.aisecurity.dto.response.FileMetadataResponse;
import com.sanjay.aisecurity.dto.response.FileResponse;
import com.sanjay.aisecurity.dto.response.FileSummaryResponse;
import com.sanjay.aisecurity.dto.response.UploadFileResponse;
import com.sanjay.aisecurity.dto.response.UploadProgressResponse;
import com.sanjay.aisecurity.entity.Project;
import com.sanjay.aisecurity.entity.UploadedFile;
import com.sanjay.aisecurity.entity.User;
import com.sanjay.aisecurity.enums.ScanStatus;
import com.sanjay.aisecurity.enums.UploadStatus;
import com.sanjay.aisecurity.exception.ResourceNotFoundException;
import com.sanjay.aisecurity.repository.ProjectRepository;
import com.sanjay.aisecurity.repository.UploadedFileRepository;
import com.sanjay.aisecurity.repository.UserRepository;
import com.sanjay.aisecurity.util.SecurityUtils;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FilenameUtils;
import org.apache.tika.Tika;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Implementation of UploadService.
 *
 * @author Sanjay
 * @version 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UploadServiceImpl implements UploadService {

    private final UploadedFileRepository fileRepository;
    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;

    @Value("${app.upload.directory}")
    private String baseUploadDirectory;

    @Value("${app.upload.allowed-extensions}")
    private List<String> allowedExtensions;

    private final Tika tika = new Tika();

    @PostConstruct
    public void init() {
        try {
            Files.createDirectories(Paths.get(baseUploadDirectory));
            log.info("Initialized upload directory at: {}", baseUploadDirectory);
        } catch (IOException e) {
            log.error("Could not initialize upload directory", e);
            throw new RuntimeException("Could not initialize upload directory", e);
        }
    }

    @Override
    @Transactional
    public List<UploadFileResponse> uploadFiles(Long projectId, MultipartFile[] files) {
        return uploadFiles(projectId, files, false);
    }

    @Override
    @Transactional
    public List<UploadFileResponse> uploadFiles(Long projectId, MultipartFile[] files, boolean overrideDuplicate) {
        String email = SecurityUtils.requireCurrentUserEmail();
        User uploader = loadUserByEmail(email);
        Project project = resolveOwnedProject(projectId, email);

        List<UploadFileResponse> responses = new ArrayList<>();

        for (MultipartFile file : files) {
            UploadFileResponse response = processSingleFile(file, project, uploader, overrideDuplicate);
            responses.add(response);

            if (response.isSuccess()) {
                project.setTotalFiles(project.getTotalFiles() + 1);
            }
        }

        projectRepository.save(project);
        return responses;
    }

    private UploadFileResponse processSingleFile(MultipartFile file, Project project, User uploader) {
        return processSingleFile(file, project, uploader, false);
    }

    private UploadFileResponse processSingleFile(MultipartFile file, Project project, User uploader, boolean overrideDuplicate) {
        String originalFilename = file.getOriginalFilename();
        log.info("Starting upload for file: {} (overrideDuplicate={})", originalFilename, overrideDuplicate);

        try {
            validateFile(file);

            String checksum = calculateSHA256(file);
            List<UploadedFile> existingFiles = fileRepository.findByChecksumSHA256AndProjectId(checksum, project.getId());
            if (!existingFiles.isEmpty() && !overrideDuplicate) {
                // Duplicate detected — signal the frontend to show a confirmation dialog.
                // The existing record is never modified; audit history stays immutable.
                UploadedFile existing = existingFiles.get(0);
                log.info("[Security] Duplicate detected for '{}' in project {} — awaiting user confirmation.",
                        originalFilename, project.getId());
                return UploadFileResponse.builder()
                        .success(false)
                        .message("DUPLICATE_FILE_DETECTED")
                        .fileDetails(toResponse(existing))
                        .build();
            }
            // When overrideDuplicate=true (or no duplicate exists) fall through to create a
            // brand-new UploadedFile record.  Existing records are NEVER mutated, ensuring
            // every upload event is an independent, immutable row in the audit log.
            if (overrideDuplicate && !existingFiles.isEmpty()) {
                log.info("[Audit] Override accepted for '{}' in project {} — creating a new upload record (SHA-256={}).",
                        originalFilename, project.getId(), checksum);
            }

            String extension = FilenameUtils.getExtension(originalFilename);
            if (extension != null && !extension.startsWith(".")) {
                extension = "." + extension;
            }

            String storedFileName = UUID.randomUUID().toString() + extension;
            Path projectDir = Paths.get(baseUploadDirectory, String.valueOf(uploader.getId()), String.valueOf(project.getId())).normalize().toAbsolutePath();
            Files.createDirectories(projectDir);
            
            Path targetLocation = projectDir.resolve(storedFileName).normalize().toAbsolutePath();
            
            // Security check against directory traversal
            if (!targetLocation.getParent().equals(projectDir) || !targetLocation.startsWith(projectDir)) {
                throw new SecurityException("Cannot store file outside current directory.");
            }

            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);

            String mimeType = tika.detect(targetLocation.toFile());

            UploadedFile uploadedFile = UploadedFile.builder()
                    .originalFileName(originalFilename)
                    .storedFileName(storedFileName)
                    .fileExtension(extension)
                    .mimeType(mimeType)
                    .fileSize(file.getSize())
                    .storagePath(targetLocation.toString())
                    .checksumSHA256(checksum)
                    .uploadStatus(UploadStatus.VALIDATED)
                    .scanStatus(ScanStatus.NOT_SCANNED)
                    .isDeleted(false)
                    .uploadedAt(LocalDateTime.now())
                    .project(project)
                    .uploadedBy(uploader)
                    .build();

            UploadedFile saved = fileRepository.save(uploadedFile);
            
            log.info("Successfully uploaded file: {}", storedFileName);
            
            return UploadFileResponse.builder()
                    .success(true)
                    .message("File uploaded successfully")
                    .fileDetails(toResponse(saved))
                    .build();
                    
        } catch (Exception e) {
            log.error("Failed to process file: {}", originalFilename, e);
            return UploadFileResponse.builder()
                    .success(false)
                    .message("Upload failed: " + e.getMessage())
                    .build();
        }
    }

    private void validateFile(MultipartFile file) throws IOException {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("File is empty.");
        }

        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || originalFilename.contains("..")) {
            throw new SecurityException("Invalid filename containing path traversal characters.");
        }

        String extension = "." + FilenameUtils.getExtension(originalFilename).toLowerCase();
        if (!allowedExtensions.contains(extension)) {
            throw new IllegalArgumentException("Unsupported file extension: " + extension);
        }

        // Validate ZIP integrity without extraction.
        // ZipInputStream.getNextEntry() returns null immediately for non-ZIP data
        // without throwing, so we MUST check the magic bytes AND require ≥1 entry.
        if (".zip".equals(extension)) {
            // Step 1: Verify ZIP magic bytes (PK\x03\x04 = local file header)
            byte[] header = new byte[4];
            int bytesRead;
            try (InputStream headerStream = file.getInputStream()) {
                bytesRead = headerStream.read(header);
            }
            if (bytesRead < 4 || header[0] != 0x50 || header[1] != 0x4B ||
                    header[2] != 0x03 || header[3] != 0x04) {
                throw new IllegalArgumentException("Not a valid ZIP archive (invalid magic bytes).");
            }

            // Step 2: Verify structural integrity by reading all entries
            int entryCount = 0;
            try (ZipInputStream zis = new ZipInputStream(file.getInputStream())) {
                ZipEntry entry;
                while ((entry = zis.getNextEntry()) != null) {
                    entryCount++;
                    zis.closeEntry();
                }
            } catch (Exception e) {
                throw new IllegalArgumentException("Corrupted ZIP archive: " + e.getMessage());
            }
            if (entryCount == 0) {
                throw new IllegalArgumentException("ZIP archive is empty (no entries found).");
            }
        }
    }


    private String calculateSHA256(MultipartFile file) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        try (InputStream fis = file.getInputStream()) {
            byte[] byteArray = new byte[8192];
            int bytesCount;
            while ((bytesCount = fis.read(byteArray)) != -1) {
                digest.update(byteArray, 0, bytesCount);
            }
        }
        byte[] bytes = digest.digest();
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    @Override
    @Transactional(readOnly = true)
    public Page<FileMetadataResponse> getProjectFiles(Long projectId, Pageable pageable) {
        String email = SecurityUtils.requireCurrentUserEmail();
        resolveOwnedProject(projectId, email); // Check authorization
        
        return fileRepository.findByProjectIdAndIsDeletedFalse(projectId, pageable)
                .map(this::toMetadataResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public FileMetadataResponse getFileMetadata(Long fileId) {
        String email = SecurityUtils.requireCurrentUserEmail();
        UploadedFile file = resolveOwnedFile(fileId, email, false);
        return toMetadataResponse(file);
    }

    @Override
    @Transactional(readOnly = true)
    public Resource downloadFile(Long fileId) {
        String email = SecurityUtils.requireCurrentUserEmail();
        UploadedFile file = resolveOwnedFile(fileId, email, false);
        
        try {
            Path filePath = Paths.get(file.getStoragePath()).normalize();
            Resource resource = new UrlResource(filePath.toUri());
            if (resource.exists() && resource.isReadable()) {
                return resource;
            } else {
                throw new ResourceNotFoundException("File not found or unreadable on disk.");
            }
        } catch (Exception e) {
            log.error("Could not read file: {}", file.getStoragePath(), e);
            throw new RuntimeException("Could not read file", e);
        }
    }

    @Override
    @Transactional
    public void deleteFile(Long fileId) {
        String email = SecurityUtils.requireCurrentUserEmail();
        UploadedFile file = resolveOwnedFile(fileId, email, false);
        file.setDeleted(true);
        fileRepository.save(file);
        
        Project project = file.getProject();
        project.setTotalFiles(Math.max(0, project.getTotalFiles() - 1));
        projectRepository.save(project);
        
        log.info("Soft deleted file: {}", fileId);
    }

    @Override
    @Transactional
    public void restoreFile(Long fileId) {
        String email = SecurityUtils.requireCurrentUserEmail();
        UploadedFile file = resolveOwnedFile(fileId, email, true);
        if (file.isDeleted()) {
            file.setDeleted(false);
            fileRepository.save(file);
            
            Project project = file.getProject();
            project.setTotalFiles(project.getTotalFiles() + 1);
            projectRepository.save(project);
            
            log.info("Restored file: {}", fileId);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Page<FileMetadataResponse> searchFiles(String filename, Pageable pageable) {
        String email = SecurityUtils.requireCurrentUserEmail();
        return fileRepository.findByProjectUserEmailAndOriginalFileNameContainingIgnoreCaseAndIsDeletedFalse(
                email, filename, pageable).map(this::toMetadataResponse);
    }

    @Override
    @Transactional
    public void securelyDeleteProjectFiles(Long projectId) {
        List<UploadedFile> activeFiles = fileRepository
                .findByProjectIdAndIsDeletedFalse(projectId, org.springframework.data.domain.Pageable.unpaged())
                .getContent();

        int deleted = 0;
        int failed = 0;
        for (UploadedFile f : activeFiles) {
            try {
                Path physical = Paths.get(f.getStoragePath());
                if (Files.exists(physical)) {
                    Files.delete(physical);
                }
                f.setDeleted(true);
                fileRepository.save(f);
                deleted++;
            } catch (Exception e) {
                log.error("[Security] Failed to delete physical file '{}': {}", f.getStoragePath(), e.getMessage());
                failed++;
            }
        }
        log.info("[Security] Securely deleted physical files for project {} — deleted={}, failed={}",
                projectId, deleted, failed);
    }

    @Override
    @Transactional(readOnly = true)
    public UploadProgressResponse getStatistics() {
        String email = SecurityUtils.requireCurrentUserEmail();
        
        long totalFiles = fileRepository.countByProjectUserEmailAndIsDeletedFalse(email);
        Long totalStorage = fileRepository.sumFileSizeByProjectUserEmailAndIsDeletedFalse(email);
        long totalStorageBytes = totalStorage != null ? totalStorage : 0L;
        
        long zipArchivesCount = fileRepository.countByProjectUserEmailAndFileExtensionAndIsDeletedFalse(email, ".zip");
        long sourceCodeFilesCount = totalFiles - zipArchivesCount;
        
        double avgSize = totalFiles > 0 ? (double) totalStorageBytes / totalFiles : 0.0;
        
        List<UploadedFile> largestFiles = fileRepository.findLargestFilesByUserEmail(email, Pageable.ofSize(1));
        FileSummaryResponse largest = largestFiles.isEmpty() ? null : toSummaryResponse(largestFiles.get(0));
        
        // This is an approximation as we don't have a direct query for latest upload globally
        Page<UploadedFile> latestFiles = fileRepository.findByProjectUserEmailAndIsDeletedFalse(
                email, org.springframework.data.domain.PageRequest.of(0, 1, org.springframework.data.domain.Sort.by("uploadedAt").descending()));
        FileSummaryResponse latest = latestFiles.isEmpty() ? null : toSummaryResponse(latestFiles.getContent().get(0));
        
        Map<String, Long> extensionCounts = new HashMap<>();
        for (String ext : allowedExtensions) {
            long count = fileRepository.countByProjectUserEmailAndFileExtensionAndIsDeletedFalse(email, ext);
            if (count > 0) {
                extensionCounts.put(ext, count);
            }
        }
        
        return UploadProgressResponse.builder()
                .totalUploadedFiles(totalFiles)
                .totalStorageBytes(totalStorageBytes)
                .sourceCodeFilesCount(sourceCodeFilesCount)
                .zipArchivesCount(zipArchivesCount)
                .averageFileSizeBytes(avgSize)
                .largestFile(largest)
                .latestUpload(latest)
                .filesByExtension(extensionCounts)
                .build();
    }

    // =========================================================================
    // HELPERS
    // =========================================================================

    private User loadUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Authenticated user not found."));
    }

    private Project resolveOwnedProject(Long projectId, String email) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException(MessageConstants.PROJECT_NOT_FOUND));
                
        if (!project.getUser().getEmail().equals(email)) {
            throw new org.springframework.security.access.AccessDeniedException("Access Denied: You do not own this project.");
        }
        
        if (!project.isActive()) {
            throw new ResourceNotFoundException(MessageConstants.PROJECT_NOT_FOUND);
        }
        return project;
    }
    
    private UploadedFile resolveOwnedFile(Long fileId, String email, boolean allowDeleted) {
        UploadedFile file = fileRepository.findById(fileId)
                .orElseThrow(() -> new ResourceNotFoundException("File not found."));
                
        if (!file.getProject().getUser().getEmail().equals(email)) {
            throw new org.springframework.security.access.AccessDeniedException("Access Denied: You do not own this file.");
        }
        
        if (!allowDeleted && file.isDeleted()) {
            throw new ResourceNotFoundException("File not found or access denied.");
        }
        return file;
    }

    private FileResponse toResponse(UploadedFile file) {
        return FileResponse.builder()
                .id(file.getId())
                .projectId(file.getProject().getId())
                .originalFileName(file.getOriginalFileName())
                .fileExtension(file.getFileExtension())
                .mimeType(file.getMimeType())
                .fileSize(file.getFileSize())
                .checksumSHA256(file.getChecksumSHA256())
                .uploadStatus(file.getUploadStatus().name())
                .scanStatus(file.getScanStatus().name())
                .uploadedAt(file.getUploadedAt())
                .uploadedByEmail(file.getUploadedBy().getEmail())
                .build();
    }

    private FileMetadataResponse toMetadataResponse(UploadedFile file) {
        return FileMetadataResponse.builder()
                .id(file.getId())
                .projectId(file.getProject().getId())
                .originalFileName(file.getOriginalFileName())
                .storedFileName(file.getStoredFileName())
                .fileExtension(file.getFileExtension())
                .mimeType(file.getMimeType())
                .fileSize(file.getFileSize())
                .checksumSHA256(file.getChecksumSHA256())
                .uploadStatus(file.getUploadStatus().name())
                .scanStatus(file.getScanStatus().name())
                .isDeleted(file.isDeleted())
                .uploadedAt(file.getUploadedAt())
                .createdAt(file.getCreatedAt())
                .updatedAt(file.getUpdatedAt())
                .uploadedBy(file.getUploadedBy().getEmail())
                .build();
    }
    
    private FileSummaryResponse toSummaryResponse(UploadedFile file) {
        return FileSummaryResponse.builder()
                .id(file.getId())
                .originalFileName(file.getOriginalFileName())
                .fileExtension(file.getFileExtension())
                .fileSize(file.getFileSize())
                .uploadStatus(file.getUploadStatus().name())
                .scanStatus(file.getScanStatus().name())
                .uploadedAt(file.getUploadedAt())
                .build();
    }
}
