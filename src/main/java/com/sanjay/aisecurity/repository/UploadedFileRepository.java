package com.sanjay.aisecurity.repository;

import com.sanjay.aisecurity.entity.UploadedFile;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for {@link UploadedFile} entity.
 *
 * <p>Manages persisted metadata for uploaded files, securing access using
 * project boundaries, user verification, and soft-delete filters.</p>
 *
 * @author Sanjay
 * @version 1.0.0
 */
@Repository
public interface UploadedFileRepository extends JpaRepository<UploadedFile, Long> {

    /**
     * Retrieves a paginated list of active files uploaded under a specific project.
     *
     * @param projectId parent project ID
     * @param pageable  pagination and sorting details
     * @return page of uploaded files
     */
    Page<UploadedFile> findByProjectIdAndIsDeletedFalse(Long projectId, Pageable pageable);

    /**
     * Retrieves all active files belonging to any project owned by the user email.
     *
     * @param email    owner email
     * @param pageable pagination and sorting details
     * @return page of uploaded files
     */
    Page<UploadedFile> findByProjectUserEmailAndIsDeletedFalse(String email, Pageable pageable);

    /**
     * Finds an active uploaded file by its ID, verified against the project owner's email.
     *
     * @param id    uploaded file ID
     * @param email owner email of the parent project
     * @return an {@link Optional} containing the file if found and authorized, or empty
     */
    Optional<UploadedFile> findByIdAndProjectUserEmailAndIsDeletedFalse(Long id, String email);

    /**
     * Finds an uploaded file by its ID, verified against the project owner's email, regardless of deleted status.
     * Useful for restore operations.
     *
     * @param id    uploaded file ID
     * @param email owner email of the parent project
     * @return an {@link Optional} containing the file if found and authorized, or empty
     */
    Optional<UploadedFile> findByIdAndProjectUserEmail(Long id, String email);

    /**
     * Finds a file by its SHA-256 hash within a specific project.
     * Prevents duplicate uploads of identical files in the same project.
     *
     * @param checksumSHA256 SHA-256 integrity hash
     * @param projectId parent project ID
     * @return an {@link Optional} containing the file if found
     */
    List<UploadedFile> findByChecksumSHA256AndProjectId(String checksumSHA256, Long projectId);

    /**
     * Checks if a file with the given SHA-256 hash already exists within a project.
     *
     * @param projectId parent project ID
     * @param checksumSHA256 SHA-256 integrity hash
     * @return {@code true} if an identical file exists, {@code false} otherwise
     */
    boolean existsByProjectIdAndChecksumSHA256(Long projectId, String checksumSHA256);

    /**
     * Searches for active files by original filename (partial match, case-insensitive)
     * owned by the given user.
     *
     * @param email    owner email
     * @param name     partial filename
     * @param pageable pagination details
     * @return page of uploaded files
     */
    Page<UploadedFile> findByProjectUserEmailAndOriginalFileNameContainingIgnoreCaseAndIsDeletedFalse(
            String email, String name, Pageable pageable);

    // =========================================================================
    // STATISTICS / COUNTS
    // =========================================================================

    long countByProjectUserEmailAndIsDeletedFalse(String email);

    @Query("SELECT SUM(f.fileSize) FROM UploadedFile f WHERE f.project.user.email = :email AND f.isDeleted = false")
    Long sumFileSizeByProjectUserEmailAndIsDeletedFalse(@Param("email") String email);

    @Query("SELECT COUNT(f) FROM UploadedFile f WHERE f.project.user.email = :email AND f.fileExtension = :extension AND f.isDeleted = false")
    long countByProjectUserEmailAndFileExtensionAndIsDeletedFalse(@Param("email") String email, @Param("extension") String extension);

    @Query("SELECT f FROM UploadedFile f WHERE f.project.user.email = :email AND f.isDeleted = false ORDER BY f.fileSize DESC")
    List<UploadedFile> findLargestFilesByUserEmail(@Param("email") String email, Pageable pageable);
}
