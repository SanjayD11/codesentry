package com.sanjay.aisecurity.repository;

import com.sanjay.aisecurity.entity.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Repository interface for {@link AuditLog} entity.
 *
 * <p>Provides data access to security and transaction log trails.
 * Supports retrieval by user email for standard users and global retrieval for admins.</p>
 *
 * @author Sanjay
 * @version 1.0.0
 */
@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    Page<AuditLog> findByUserEmail(String email, Pageable pageable);

    Page<AuditLog> findByUserEmailOrUserIsNull(String email, Pageable pageable);

    /**
     * Admin search across all audit logs with optional filtering by action, email, and date range.
     */
    @EntityGraph(attributePaths = {"user"})
    @Query("SELECT a FROM AuditLog a LEFT JOIN a.user u WHERE " +
           "(:action IS NULL OR LOWER(a.action) LIKE LOWER(CONCAT('%', :action, '%'))) AND " +
           "(:userEmail IS NULL OR (u.email IS NOT NULL AND LOWER(u.email) LIKE LOWER(CONCAT('%', :userEmail, '%')))) AND " +
           "(:resource IS NULL OR LOWER(a.resource) LIKE LOWER(CONCAT('%', :resource, '%'))) AND " +
           "(cast(:fromDate as timestamp) IS NULL OR a.createdAt >= :fromDate) AND " +
           "(cast(:toDate as timestamp) IS NULL OR a.createdAt <= :toDate)")
    Page<AuditLog> searchAuditLogs(
            @Param("action") String action,
            @Param("userEmail") String userEmail,
            @Param("resource") String resource,
            @Param("fromDate") LocalDateTime fromDate,
            @Param("toDate") LocalDateTime toDate,
            Pageable pageable);

    /**
     * Finds all audit logs for CSV export with optional filters (no pagination).
     */
    @EntityGraph(attributePaths = {"user"})
    @Query("SELECT a FROM AuditLog a LEFT JOIN a.user u WHERE " +
           "(:action IS NULL OR LOWER(a.action) LIKE LOWER(CONCAT('%', :action, '%'))) AND " +
           "(:userEmail IS NULL OR (u.email IS NOT NULL AND LOWER(u.email) LIKE LOWER(CONCAT('%', :userEmail, '%')))) AND " +
           "(cast(:fromDate as timestamp) IS NULL OR a.createdAt >= :fromDate) AND " +
           "(cast(:toDate as timestamp) IS NULL OR a.createdAt <= :toDate) " +
           "ORDER BY a.createdAt DESC")
    List<AuditLog> findForExport(
            @Param("action") String action,
            @Param("userEmail") String userEmail,
            @Param("fromDate") LocalDateTime fromDate,
            @Param("toDate") LocalDateTime toDate);
}
