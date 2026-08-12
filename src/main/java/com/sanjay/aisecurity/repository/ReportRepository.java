package com.sanjay.aisecurity.repository;

import com.sanjay.aisecurity.entity.Report;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for {@link Report} entity.
 *
 * <p>Handles data operations for generated PDF assessments, protecting retrieval
 * using project containment verification.</p>
 *
 * @author Sanjay
 * @version 1.0.0
 */
@Repository
public interface ReportRepository extends JpaRepository<Report, Long> {

    /**
     * Retrieves a page of reports for all projects belonging to the authenticated user.
     *
     * @param email    owner email
     * @param pageable pagination parameters
     * @return page of reports
     */
    Page<Report> findByProjectUserEmail(String email, Pageable pageable);

    /**
     * Finds a report by its ID and owner's email verification.
     *
     * @param id    report ID
     * @param email owner email
     * @return an {@link Optional} containing the report if owned, or empty
     */
    Optional<Report> findByIdAndProjectUserEmail(Long id, String email);

    /**
     * Retrieves all reports generated for a specific project.
     *
     * @param projectId target project ID
     * @return list of reports
     */
    List<Report> findByProjectId(Long projectId);
}
