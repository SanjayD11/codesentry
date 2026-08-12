package com.sanjay.aisecurity.repository;

import com.sanjay.aisecurity.entity.ScanHistory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for {@link ScanHistory} entity.
 *
 * <p>Exposes operations to track scans, retrieve paginated history records,
 * and fetch the latest scan details for dashboard aggregates.</p>
 *
 * @author Sanjay
 * @version 1.0.0
 */
@Repository
public interface ScanHistoryRepository extends JpaRepository<ScanHistory, Long> {

    /**
     * Retrieves a page of scan history records for a project, verified by the owner's email.
     *
     * @param projectId parent project ID
     * @param email     owner email
     * @param pageable  pagination parameters
     * @return a page of scans
     */
    Page<ScanHistory> findByProjectIdAndProjectUserEmail(Long projectId, String email, Pageable pageable);

    /**
     * Finds a scan history record by its ID, verified by the project owner's email.
     *
     * @param id    scan history record ID
     * @param email owner email
     * @return an {@link Optional} containing the scan history if found and owned, or empty
     */
    Optional<ScanHistory> findByIdAndProjectUserEmail(Long id, String email);

    /**
     * Returns all scans belonging to a user across all their projects.
     *
     * @param email user email
     * @return list of scans ordered by completion/creation timestamp descending
     */
    List<ScanHistory> findByProjectUserEmailOrderByCreatedAtDesc(String email);

    /**
     * Finds the most recent scan completed or run for a given project.
     *
     * @param projectId project ID
     * @return an {@link Optional} containing the most recent scan, or empty
     */
    Optional<ScanHistory> findFirstByProjectIdOrderByCreatedAtDesc(Long projectId);

    /**
     * Finds all scans for a project, ordered by scan start time descending.
     * 
     * @param projectId project ID
     * @return list of scans
     */
    List<ScanHistory> findByProjectIdOrderByScanStartDesc(Long projectId);

    long countByProjectUserEmail(String email);

    long countByStatus(com.sanjay.aisecurity.enums.ScanStatus status);

    boolean existsByProjectIdAndStatusIn(Long projectId, java.util.Collection<com.sanjay.aisecurity.enums.ScanStatus> statuses);
    
    List<ScanHistory> findByProjectIdAndStatusIn(Long projectId, java.util.Collection<com.sanjay.aisecurity.enums.ScanStatus> statuses);

    List<ScanHistory> findTop5ByProjectUserEmailOrderByScanStartDesc(String email);

    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.data.jpa.repository.Query("UPDATE ScanHistory s SET s.progressPercentage = :pct WHERE s.id = :id")
    void updateProgress(@org.springframework.data.repository.query.Param("id") Long id, @org.springframework.data.repository.query.Param("pct") int pct);
}
