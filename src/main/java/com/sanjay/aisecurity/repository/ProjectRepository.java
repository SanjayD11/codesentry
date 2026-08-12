package com.sanjay.aisecurity.repository;

import com.sanjay.aisecurity.entity.Project;
import com.sanjay.aisecurity.entity.User;
import com.sanjay.aisecurity.enums.ProjectType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository interface for {@link Project} entity.
 *
 * <p>Supports soft-deleted checks where only active = true projects are returned
 * by default for normal operations. Extends query capabilities with search,
 * filtering, and DB aggregations.</p>
 *
 * @author Sanjay
 * @version 1.0.0
 */
@Repository
public interface ProjectRepository extends JpaRepository<Project, Long> {

    // =========================================================================
    // ACTIVE FINDERS
    // =========================================================================

    Page<Project> findByUserEmailAndActiveTrueAndNameNot(String email, String name, Pageable pageable);

    List<Project> findByUserEmailAndActiveTrueAndNameNot(String email, String name);

    Optional<Project> findByUserEmailAndNameAndActiveTrue(String email, String name);

    Optional<Project> findByIdAndUserEmailAndActiveTrue(Long id, String email);

    // =========================================================================
    // SEARCH & FILTER
    // =========================================================================

    @Query("SELECT p FROM Project p WHERE p.user.email = :email " +
           "AND p.name <> '[Direct Scans]' " +
           "AND (:projectName IS NULL OR LOWER(p.name) LIKE LOWER(CONCAT('%', :projectName, '%'))) " +
           "AND (:projectType IS NULL OR p.projectType = :projectType) " +
           "AND (:active IS NULL OR p.active = :active) " +
           "AND (:createdAfter IS NULL OR p.createdAt >= :createdAfter) " +
           "AND (:createdBefore IS NULL OR p.createdAt <= :createdBefore)")
    Page<Project> searchProjects(
            @Param("email") String email,
            @Param("projectName") String projectName,
            @Param("projectType") ProjectType projectType,
            @Param("active") Boolean active,
            @Param("createdAfter") LocalDateTime createdAfter,
            @Param("createdBefore") LocalDateTime createdBefore,
            Pageable pageable);

    // =========================================================================
    // COLLISION CHECKS
    // =========================================================================

    boolean existsByNameAndUserEmailAndActiveTrue(String name, String email);

    @Query("SELECT COUNT(p) > 0 FROM Project p WHERE p.name = :name AND p.user.email = :email AND p.id <> :excludeId AND p.active = true")
    boolean existsByNameAndUserEmailAndActiveTrueExcludingId(
            @Param("name") String name,
            @Param("email") String email,
            @Param("excludeId") Long excludeId);

    // =========================================================================
    // COUNTS & AGGREGATES
    // =========================================================================

    long countByUserAndActiveTrue(User user);

    long countByUserEmailAndActiveTrueAndNameNot(String email, String name);

    long countByUserEmailAndActiveFalseAndNameNot(String email, String name);

    long countByUserEmailAndActiveTrueAndStatus(String email, String status);

    long countByUserEmailAndActiveTrueAndProjectType(String email, ProjectType projectType);

    @Query("SELECT SUM(p.totalFiles) FROM Project p WHERE p.user.email = :email AND p.active = true AND p.name <> '[Direct Scans]'")
    Long sumTotalFilesByUserEmailAndActiveTrueAndNameNot(@Param("email") String email);

    @Query("SELECT AVG(p.securityScore) FROM Project p WHERE p.user.email = :email AND p.active = true AND p.name <> '[Direct Scans]'")
    Double averageSecurityScoreByUserEmailAndActiveTrueAndNameNot(@Param("email") String email);

    // =========================================================================
    // ADMIN ONLY FINDERS (Allows listing inactive/deleted projects)
    // =========================================================================

    Page<Project> findAll(Pageable pageable);

    Page<Project> findByNameContainingIgnoreCase(String name, Pageable pageable);

    /** Counts all active projects globally (for admin dashboard). */
    long countByActiveTrue();

    /** Counts all archived/inactive projects globally. */
    long countByActiveFalse();

    /** Counts projects created after a given timestamp. */
    long countByCreatedAtAfter(java.time.LocalDateTime since);

    /** Admin search with optional name/status filters. */
    @Query("SELECT p FROM Project p WHERE " +
           "(:search IS NULL OR LOWER(p.name) LIKE LOWER(CONCAT('%', :search, '%')) " +
           " OR LOWER(p.user.email) LIKE LOWER(CONCAT('%', :search, '%')) " +
           " OR LOWER(p.user.firstName) LIKE LOWER(CONCAT('%', :search, '%'))) " +
           "AND (:status IS NULL OR p.status = :status) " +
           "AND (:active IS NULL OR p.active = :active)")
    Page<Project> findProjectsWithFilters(
            @Param("search") String search,
            @Param("status") String status,
            @Param("active") Boolean active,
            Pageable pageable);

    /** Returns count of projects per user for admin user listing. */
    @Query("SELECT p.user.id, COUNT(p) FROM Project p GROUP BY p.user.id")
    List<Object[]> countProjectsGroupedByUser();

    /** Returns count of projects for a specific user. */
    @Query("SELECT COUNT(p) FROM Project p WHERE p.user.id = :userId")
    long countByUserId(@Param("userId") Long userId);
}
