package com.sanjay.aisecurity.controller;

import com.sanjay.aisecurity.constants.ApiConstants;
import com.sanjay.aisecurity.dto.AdminCreateUserRequest;
import com.sanjay.aisecurity.dto.AdminDashboardStatsResponse;
import com.sanjay.aisecurity.dto.AdminUpdateUserRequest;
import com.sanjay.aisecurity.dto.SettingResponse;
import com.sanjay.aisecurity.dto.SystemHealthResponse;
import com.sanjay.aisecurity.dto.UpdateSettingRequest;
import com.sanjay.aisecurity.dto.UserResponse;
import com.sanjay.aisecurity.common.ApiResponse;
import com.sanjay.aisecurity.entity.AuditLog;
import com.sanjay.aisecurity.entity.Project;
import com.sanjay.aisecurity.entity.ScanHistory;
import com.sanjay.aisecurity.enums.Role;
import com.sanjay.aisecurity.service.AdminService;
import com.sanjay.aisecurity.service.ApplicationSettingsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Admin REST Controller.
 *
 * <p>All endpoints require ROLE_ADMIN. Every operation is audit-logged with the
 * admin's IP address and user-agent. Privacy rules are enforced at service level —
 * user source code, scan results, and AI summaries are never accessible here.</p>
 *
 * @author Sanjay
 * @version 1.0.0
 */
@RestController
@RequestMapping(ApiConstants.ADMIN_BASE)
@RequiredArgsConstructor
@Tag(name = "Admin Operations", description = "Endpoints for platform administration (ADMIN role only)")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final AdminService adminService;
    private final ApplicationSettingsService settingsService;

    // =========================================================================
    // DASHBOARD
    // =========================================================================

    @GetMapping("/stats")
    @Operation(summary = "Get real-time platform statistics from database")
    public ResponseEntity<ApiResponse<AdminDashboardStatsResponse>> getDashboardStats() {
        return ResponseEntity.ok(ApiResponse.success(
                "Dashboard stats fetched successfully",
                adminService.getDashboardStats()
        ));
    }

    @GetMapping("/health")
    @Operation(summary = "Get system health status")
    public ResponseEntity<ApiResponse<SystemHealthResponse>> getSystemHealth() {
        return ResponseEntity.ok(ApiResponse.success(
                "System health fetched successfully",
                adminService.getSystemHealth()
        ));
    }

    // =========================================================================
    // USER MANAGEMENT
    // =========================================================================

    @GetMapping("/users")
    @Operation(summary = "Get paginated, filterable list of all users")
    public ResponseEntity<ApiResponse<Page<UserResponse>>> getAllUsers(
            @RequestParam(defaultValue = "") String search,
            @RequestParam(required = false) String role,
            @RequestParam(required = false) Boolean active,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String direction) {

        Role roleEnum = null;
        if (role != null && !role.isBlank()) {
            try { roleEnum = Role.valueOf(role.toUpperCase()); } catch (IllegalArgumentException ignored) {}
        }
        Sort sort = Sort.by(Sort.Direction.fromString(direction), sortBy);
        Pageable pageable = PageRequest.of(page, size, sort);
        return ResponseEntity.ok(ApiResponse.success(
                "Users fetched successfully",
                adminService.getAllUsers(search, roleEnum, active, pageable)
        ));
    }

    @GetMapping("/users/{userId}")
    @Operation(summary = "Get a specific user by ID")
    public ResponseEntity<ApiResponse<UserResponse>> getUserById(@PathVariable Long userId) {
        return ResponseEntity.ok(ApiResponse.success(
                "User fetched successfully",
                adminService.getUserById(userId)
        ));
    }

    @PostMapping("/users")
    @Operation(summary = "Create a new user account")
    public ResponseEntity<ApiResponse<UserResponse>> createUser(
            @Valid @RequestBody AdminCreateUserRequest request,
            HttpServletRequest httpRequest) {
        UserResponse created = adminService.createUser(request,
                getClientIp(httpRequest), getUserAgent(httpRequest));
        return ResponseEntity.status(201).body(ApiResponse.created("User created successfully", created));
    }

    @PutMapping("/users/{userId}")
    @Operation(summary = "Update user metadata (name, email)")
    public ResponseEntity<ApiResponse<UserResponse>> updateUser(
            @PathVariable Long userId,
            @Valid @RequestBody AdminUpdateUserRequest request,
            HttpServletRequest httpRequest) {
        return ResponseEntity.ok(ApiResponse.success(
                "User updated successfully",
                adminService.updateUser(userId, request, getClientIp(httpRequest), getUserAgent(httpRequest))
        ));
    }

    @PatchMapping("/users/{userId}/status")
    @Operation(summary = "Enable or disable a user account")
    public ResponseEntity<ApiResponse<Void>> toggleUserStatus(
            @PathVariable Long userId,
            @RequestBody Map<String, Boolean> body,
            HttpServletRequest httpRequest) {
        Boolean active = body.getOrDefault("active", true);
        adminService.toggleUserStatus(userId, active, getClientIp(httpRequest), getUserAgent(httpRequest));
        return ResponseEntity.ok(ApiResponse.success("User status updated successfully", null));
    }

    @PatchMapping("/users/{userId}/role")
    @Operation(summary = "Change a user's role")
    public ResponseEntity<ApiResponse<Void>> changeUserRole(
            @PathVariable Long userId,
            @RequestBody Map<String, String> body,
            HttpServletRequest httpRequest) {
        String role = body.getOrDefault("role", "USER");
        adminService.changeUserRole(userId, role, getClientIp(httpRequest), getUserAgent(httpRequest));
        return ResponseEntity.ok(ApiResponse.success("User role updated successfully", null));
    }

    @PatchMapping("/users/{userId}/reset-password")
    @Operation(summary = "Reset a user's password")
    public ResponseEntity<ApiResponse<Void>> resetUserPassword(
            @PathVariable Long userId,
            @RequestBody Map<String, String> body,
            HttpServletRequest httpRequest) {
        String newPassword = body.get("newPassword");
        if (newPassword == null || newPassword.length() < 8) {
            return ResponseEntity.badRequest().body(ApiResponse.badRequest("Password must be at least 8 characters"));
        }
        adminService.resetUserPassword(userId, newPassword, getClientIp(httpRequest), getUserAgent(httpRequest));
        return ResponseEntity.ok(ApiResponse.success("Password reset successfully", null));
    }

    @DeleteMapping("/users/{userId}")
    @Operation(summary = "Permanently delete a user account")
    public ResponseEntity<ApiResponse<Void>> deleteUser(
            @PathVariable Long userId,
            HttpServletRequest httpRequest) {
        adminService.deleteUser(userId, getClientIp(httpRequest), getUserAgent(httpRequest));
        return ResponseEntity.ok(ApiResponse.success("User deleted successfully", null));
    }

    // =========================================================================
    // PROJECT MANAGEMENT (Lifecycle only — contents are never exposed)
    // =========================================================================

    @GetMapping("/projects")
    @Operation(summary = "Get paginated list of all projects (metadata only)")
    public ResponseEntity<ApiResponse<Page<Project>>> getAllProjects(
            @RequestParam(defaultValue = "") String search,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Boolean active,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String direction) {

        Sort sort = Sort.by(Sort.Direction.fromString(direction), sortBy);
        Pageable pageable = PageRequest.of(page, size, sort);
        return ResponseEntity.ok(ApiResponse.success(
                "Projects fetched successfully",
                adminService.getAllProjects(search, status, active, pageable)
        ));
    }

    @PatchMapping("/projects/{projectId}/archive")
    @Operation(summary = "Archive a project")
    public ResponseEntity<ApiResponse<Void>> archiveProject(
            @PathVariable Long projectId,
            HttpServletRequest httpRequest) {
        adminService.archiveProject(projectId, getClientIp(httpRequest), getUserAgent(httpRequest));
        return ResponseEntity.ok(ApiResponse.success("Project archived successfully", null));
    }

    @PatchMapping("/projects/{projectId}/restore")
    @Operation(summary = "Restore an archived project")
    public ResponseEntity<ApiResponse<Void>> restoreProject(
            @PathVariable Long projectId,
            HttpServletRequest httpRequest) {
        adminService.restoreProject(projectId, getClientIp(httpRequest), getUserAgent(httpRequest));
        return ResponseEntity.ok(ApiResponse.success("Project restored successfully", null));
    }

    @DeleteMapping("/projects/{projectId}")
    @Operation(summary = "Permanently delete a project")
    public ResponseEntity<ApiResponse<Void>> deleteProject(
            @PathVariable Long projectId,
            HttpServletRequest httpRequest) {
        adminService.deleteProject(projectId, getClientIp(httpRequest), getUserAgent(httpRequest));
        return ResponseEntity.ok(ApiResponse.success("Project deleted successfully", null));
    }

    @PatchMapping("/projects/{projectId}/transfer")
    @Operation(summary = "Transfer project ownership to another user")
    public ResponseEntity<ApiResponse<Void>> transferProjectOwnership(
            @PathVariable Long projectId,
            @RequestBody Map<String, Long> body,
            HttpServletRequest httpRequest) {
        Long newOwnerId = body.get("newOwnerId");
        if (newOwnerId == null) throw new IllegalArgumentException("newOwnerId is required");
        adminService.transferProjectOwnership(projectId, newOwnerId, getClientIp(httpRequest), getUserAgent(httpRequest));
        return ResponseEntity.ok(ApiResponse.success("Project transferred successfully", null));
    }

    // =========================================================================
    // SCANS
    // =========================================================================

    @GetMapping("/scans")
    @Operation(summary = "Get paginated list of all scans")
    public ResponseEntity<ApiResponse<Page<ScanHistory>>> getAllScans(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "scanStart") String sortBy,
            @RequestParam(defaultValue = "desc") String direction) {
        Sort sort = Sort.by(Sort.Direction.fromString(direction), sortBy);
        Pageable pageable = PageRequest.of(page, size, sort);
        return ResponseEntity.ok(ApiResponse.success(
                "Scans fetched successfully",
                adminService.getAllScans(pageable)
        ));
    }

    // =========================================================================
    // AUDIT LOGS
    // =========================================================================

    @GetMapping("/audit-logs")
    @Operation(summary = "Get searchable, filterable paginated audit logs")
    public ResponseEntity<ApiResponse<Page<AuditLog>>> getAllAuditLogs(
            @RequestParam(required = false) String action,
            @RequestParam(required = false) String userEmail,
            @RequestParam(required = false) String resource,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime toDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String direction) {

        Sort sort = Sort.by(Sort.Direction.fromString(direction), sortBy);
        Pageable pageable = PageRequest.of(page, size, sort);
        return ResponseEntity.ok(ApiResponse.success(
                "Audit logs fetched successfully",
                adminService.getAllAuditLogs(action, userEmail, resource, fromDate, toDate, pageable)
        ));
    }

    @GetMapping("/audit-logs/export")
    @Operation(summary = "Export audit logs as CSV file")
    public ResponseEntity<byte[]> exportAuditLogs(
            @RequestParam(required = false) String action,
            @RequestParam(required = false) String userEmail,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime toDate) {

        byte[] csv = adminService.exportAuditLogsCsv(action, userEmail, fromDate, toDate);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"audit-logs.csv\"")
                .header(HttpHeaders.CONTENT_TYPE, "text/csv; charset=UTF-8")
                .body(csv);
    }

    // =========================================================================
    // SETTINGS
    // =========================================================================

    @GetMapping("/settings")
    @Operation(summary = "Get all platform settings grouped by category")
    public ResponseEntity<ApiResponse<Map<String, List<SettingResponse>>>> getAllSettings() {
        return ResponseEntity.ok(ApiResponse.success(
                "Settings fetched successfully",
                settingsService.getAllSettingsGrouped()
        ));
    }

    @PutMapping("/settings/{settingKey}")
    @Operation(summary = "Update a platform setting value")
    public ResponseEntity<ApiResponse<SettingResponse>> updateSetting(
            @PathVariable String settingKey,
            @Valid @RequestBody UpdateSettingRequest request,
            HttpServletRequest httpRequest) {
        SettingResponse updated = settingsService.updateSetting(settingKey, request.getValue());
        return ResponseEntity.ok(ApiResponse.success("Setting updated successfully", updated));
    }

    // =========================================================================
    // DATA EXPORT
    // =========================================================================

    @PostMapping("/export")
    @Operation(summary = "Export selected platform data")
    public ResponseEntity<byte[]> exportPlatformData(
            @RequestBody com.sanjay.aisecurity.dto.ExportRequest request) throws Exception {

        com.sanjay.aisecurity.service.AdminExportService exportService =
            org.springframework.web.context.support.WebApplicationContextUtils
                .getRequiredWebApplicationContext(
                    ((org.springframework.web.context.request.ServletRequestAttributes)
                    org.springframework.web.context.request.RequestContextHolder.getRequestAttributes())
                    .getRequest().getServletContext()
                ).getBean(com.sanjay.aisecurity.service.AdminExportService.class);

        byte[] fileData = exportService.generateExport(request);
        String extension = "excel".equalsIgnoreCase(request.getFormat()) ? "xlsx" :
                          ("csv".equalsIgnoreCase(request.getFormat()) ? "csv" : "pdf");
        String contentType = "excel".equalsIgnoreCase(request.getFormat()) ?
                             "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet" :
                             ("csv".equalsIgnoreCase(request.getFormat()) ? "text/csv" : "application/pdf");

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"aegis-nexus-export." + extension + "\"")
                .header(HttpHeaders.CONTENT_TYPE, contentType)
                .body(fileData);
    }

    // =========================================================================
    // UTILITY
    // =========================================================================

    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isBlank()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private String getUserAgent(HttpServletRequest request) {
        String ua = request.getHeader("User-Agent");
        if (ua == null) return null;
        // Truncate to safe length — no personal data in user agent
        return ua.length() > 500 ? ua.substring(0, 500) : ua;
    }
}
