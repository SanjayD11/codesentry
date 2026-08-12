package com.sanjay.aisecurity.service;

import com.sanjay.aisecurity.dto.AdminCreateUserRequest;
import com.sanjay.aisecurity.dto.AdminDashboardStatsResponse;
import com.sanjay.aisecurity.dto.AdminUpdateUserRequest;
import com.sanjay.aisecurity.dto.SystemHealthResponse;
import com.sanjay.aisecurity.dto.UserResponse;
import com.sanjay.aisecurity.entity.AuditLog;
import com.sanjay.aisecurity.entity.Project;
import com.sanjay.aisecurity.entity.ScanHistory;
import com.sanjay.aisecurity.enums.Role;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Admin service interface defining all platform administration operations.
 *
 * <p>Every method requires ADMIN role authorization.
 * Privacy constraints are strictly enforced at service layer.</p>
 *
 * @author Sanjay
 * @version 1.0.0
 */
public interface AdminService {

    // ── Dashboard ─────────────────────────────────────────────────────────────

    AdminDashboardStatsResponse getDashboardStats();

    SystemHealthResponse getSystemHealth();

    // ── User Management ───────────────────────────────────────────────────────

    Page<UserResponse> getAllUsers(String search, Role role, Boolean active, Pageable pageable);

    UserResponse getUserById(Long userId);

    UserResponse createUser(AdminCreateUserRequest request, String ipAddress, String userAgent);

    UserResponse updateUser(Long userId, AdminUpdateUserRequest request, String ipAddress, String userAgent);

    void toggleUserStatus(Long userId, boolean active, String ipAddress, String userAgent);

    void changeUserRole(Long userId, String role, String ipAddress, String userAgent);

    void resetUserPassword(Long userId, String newPassword, String ipAddress, String userAgent);

    void deleteUser(Long userId, String ipAddress, String userAgent);

    // ── Project Management ────────────────────────────────────────────────────

    Page<Project> getAllProjects(String search, String status, Boolean active, Pageable pageable);

    void archiveProject(Long projectId, String ipAddress, String userAgent);

    void restoreProject(Long projectId, String ipAddress, String userAgent);

    void deleteProject(Long projectId, String ipAddress, String userAgent);

    void transferProjectOwnership(Long projectId, Long newOwnerId, String ipAddress, String userAgent);

    // ── Scans ─────────────────────────────────────────────────────────────────

    Page<ScanHistory> getAllScans(Pageable pageable);

    // ── Audit Logs ────────────────────────────────────────────────────────────

    Page<AuditLog> getAllAuditLogs(String action, String userEmail, String resource,
                                   LocalDateTime fromDate, LocalDateTime toDate, Pageable pageable);

    byte[] exportAuditLogsCsv(String action, String userEmail, LocalDateTime fromDate, LocalDateTime toDate);
}
