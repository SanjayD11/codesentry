package com.sanjay.aisecurity.service;

import com.sanjay.aisecurity.dto.AdminCreateUserRequest;
import com.sanjay.aisecurity.dto.AdminDashboardStatsResponse;
import com.sanjay.aisecurity.dto.AdminUpdateUserRequest;
import com.sanjay.aisecurity.dto.SystemHealthResponse;
import com.sanjay.aisecurity.dto.UserResponse;
import com.sanjay.aisecurity.entity.AuditLog;
import com.sanjay.aisecurity.entity.Project;
import com.sanjay.aisecurity.entity.ScanHistory;
import com.sanjay.aisecurity.entity.User;
import com.sanjay.aisecurity.enums.Role;
import com.sanjay.aisecurity.enums.ScanStatus;
import com.sanjay.aisecurity.enums.Severity;
import com.sanjay.aisecurity.repository.*;
import com.sanjay.aisecurity.util.SecurityUtils;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Admin Service Implementation.
 *
 * <p>Provides production-ready, security-enforced implementations of all admin operations.
 * Every mutating operation writes an audit log entry including the admin's identity,
 * the target resource, the action taken, and the outcome.</p>
 *
 * <p>Privacy rules are strictly enforced at this layer:
 * <ul>
 *   <li>User source code, scan reports, vulnerabilities, and AI summaries are NEVER
 *       accessible to admins through any method in this service.</li>
 *   <li>Project management operations are limited to lifecycle metadata only.</li>
 * </ul>
 * </p>
 *
 * @author Sanjay
 * @version 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminServiceImpl implements AdminService {

    private final UserRepository userRepository;
    private final ProjectRepository projectRepository;
    private final ScanHistoryRepository scanHistoryRepository;
    private final VulnerabilityRepository vulnerabilityRepository;
    private final AuditLogRepository auditLogRepository;
    private final ReportRepository reportRepository;
    private final EntityManager entityManager;
    private final PasswordEncoder passwordEncoder;

    // =========================================================================
    // DASHBOARD
    // =========================================================================

    @Override
    @Transactional(readOnly = true)
    public AdminDashboardStatsResponse getDashboardStats() {
        LocalDateTime todayStart = LocalDate.now().atStartOfDay();

        // User stats
        long totalUsers = userRepository.count();
        long activeUsers = userRepository.countByActiveTrue();
        long disabledUsers = userRepository.countByActiveFalse();
        long usersRegisteredToday = userRepository.countByCreatedAtAfter(todayStart);

        // Project stats
        long totalProjects = projectRepository.count();
        long activeProjects = projectRepository.countByActiveTrue();
        long archivedProjects = projectRepository.countByActiveFalse();
        long projectsCreatedToday = projectRepository.countByCreatedAtAfter(todayStart);

        // Scan stats
        long totalScans = scanHistoryRepository.count();
        long completedScans = scanHistoryRepository.countByStatus(ScanStatus.COMPLETED);
        long failedScans = scanHistoryRepository.countByStatus(ScanStatus.FAILED);
        long scansToday = ((Number) entityManager
                .createQuery("SELECT COUNT(s) FROM ScanHistory s WHERE s.createdAt >= :today")
                .setParameter("today", todayStart)
                .getSingleResult()).longValue();

        // Vulnerability counts by severity
        long criticalVulnerabilities = vulnerabilityRepository.countBySeverity(Severity.CRITICAL);
        long highVulnerabilities = vulnerabilityRepository.countBySeverity(Severity.HIGH);
        long mediumVulnerabilities = vulnerabilityRepository.countBySeverity(Severity.MEDIUM);
        long lowVulnerabilities = vulnerabilityRepository.countBySeverity(Severity.LOW);

        // Aggregates
        Number avgScoreResult = (Number) entityManager
                .createQuery("SELECT AVG(s.securityScore) FROM ScanHistory s")
                .getResultStream().findFirst().orElse(100.0);
        double averageScanScore = avgScoreResult != null ? avgScoreResult.doubleValue() : 100.0;

        Number avgFindingsResult = (Number) entityManager
                .createQuery("SELECT AVG(s.totalVulnerabilities) FROM ScanHistory s")
                .getResultStream().findFirst().orElse(0.0);
        double averageFindings = avgFindingsResult != null ? avgFindingsResult.doubleValue() : 0.0;

        Number avgDurationResult = (Number) entityManager
                .createNativeQuery("SELECT AVG(TIMESTAMPDIFF(SECOND, scan_start, scan_end)) FROM scan_history WHERE status = 'COMPLETED'")
                .getResultStream().findFirst().orElse(0.0);
        double averageScanDuration = avgDurationResult != null ? avgDurationResult.doubleValue() : 0.0;

        long totalReports = reportRepository.count();
        long aiRequests = ((Number) entityManager
                .createQuery("SELECT COUNT(c) FROM ChatHistory c")
                .getSingleResult()).longValue();

        return AdminDashboardStatsResponse.builder()
                .totalUsers(totalUsers)
                .activeUsers(activeUsers)
                .disabledUsers(disabledUsers)
                .usersRegisteredToday(usersRegisteredToday)
                .totalProjects(totalProjects)
                .activeProjects(activeProjects)
                .archivedProjects(archivedProjects)
                .projectsCreatedToday(projectsCreatedToday)
                .totalScans(totalScans)
                .completedScans(completedScans)
                .failedScans(failedScans)
                .scansToday(scansToday)
                .criticalVulnerabilities(criticalVulnerabilities)
                .highVulnerabilities(highVulnerabilities)
                .mediumVulnerabilities(mediumVulnerabilities)
                .lowVulnerabilities(lowVulnerabilities)
                .averageScanScore(averageScanScore)
                .averageFindings(averageFindings)
                .averageScanDuration(averageScanDuration)
                .totalReports(totalReports)
                .aiReportsGenerated(totalReports)
                .aiRequests(aiRequests)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public SystemHealthResponse getSystemHealth() {
        String dbStatus = "Disconnected";
        try {
            entityManager.createNativeQuery("SELECT 1").getSingleResult();
            dbStatus = "Connected";
        } catch (Exception e) {
            log.error("Database health check failed", e);
        }
        return SystemHealthResponse.builder()
                .backendStatus("UP")
                .databaseStatus(dbStatus)
                .aiProviderStatus("Operational")
                .storageUsage("Available")
                .build();
    }

    // =========================================================================
    // USER MANAGEMENT
    // =========================================================================

    @Override
    @Transactional(readOnly = true)
    public Page<UserResponse> getAllUsers(String search, Role role, Boolean active, Pageable pageable) {
        String searchParam = (search != null && !search.isBlank()) ? search.trim() : null;
        Page<User> usersPage = userRepository.findUsersWithFilters(searchParam, role, active, pageable);

        // Build a map of user IDs to project counts efficiently
        java.util.Map<Long, Long> projectCounts = new java.util.HashMap<>();
        try {
            List<Object[]> counts = projectRepository.countProjectsGroupedByUser();
            for (Object[] row : counts) {
                Long userId = ((Number) row[0]).longValue();
                Long count = ((Number) row[1]).longValue();
                projectCounts.put(userId, count);
            }
        } catch (Exception e) {
            log.warn("Could not load project counts: {}", e.getMessage());
        }

        return usersPage.map(user -> {
            long projectCount = projectCounts.getOrDefault(user.getId(), 0L);
            return UserResponse.from(user, projectCount);
        });
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse getUserById(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        long projectCount = projectRepository.countByUserId(userId);
        return UserResponse.from(user, projectCount);
    }

    @Override
    @Transactional
    public UserResponse createUser(AdminCreateUserRequest request, String ipAddress, String userAgent) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Email already registered: " + request.getEmail());
        }

        Role role = Role.valueOf(request.getRole().toUpperCase());
        User newUser = User.builder()
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(role)
                .active(true)
                .emailVerified(true) // admin-created accounts are pre-verified
                .build();
        User saved = userRepository.save(newUser);

        String adminEmail = SecurityUtils.requireCurrentUserEmail();
        saveAuditLog("USER_CREATED", "User", adminEmail,
                "Admin created user: " + saved.getEmail() + " with role " + role.name(),
                ipAddress, userAgent, "SUCCESS", saved.getId(), saved.getEmail());

        return UserResponse.from(saved, 0L);
    }

    @Override
    @Transactional
    public UserResponse updateUser(Long userId, AdminUpdateUserRequest request,
                                   String ipAddress, String userAgent) {
        User target = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        // Check email uniqueness if changed
        if (!target.getEmail().equalsIgnoreCase(request.getEmail())
                && userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Email already taken: " + request.getEmail());
        }

        String oldEmail = target.getEmail();
        target.setFirstName(request.getFirstName());
        target.setLastName(request.getLastName());
        target.setEmail(request.getEmail());
        User saved = userRepository.save(target);

        String adminEmail = SecurityUtils.requireCurrentUserEmail();
        saveAuditLog("USER_UPDATED", "User", adminEmail,
                "Admin updated user profile: " + oldEmail,
                ipAddress, userAgent, "SUCCESS", saved.getId(), saved.getEmail());

        long projectCount = projectRepository.countByUserId(userId);
        return UserResponse.from(saved, projectCount);
    }

    @Override
    @Transactional
    public void toggleUserStatus(Long userId, boolean active, String ipAddress, String userAgent) {
        String adminEmail = SecurityUtils.requireCurrentUserEmail();
        User target = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        if (target.getEmail().equalsIgnoreCase(adminEmail)) {
            throw new IllegalArgumentException("You cannot deactivate your own account.");
        }

        target.setActive(active);
        userRepository.save(target);

        saveAuditLog(active ? "USER_ENABLED" : "USER_DISABLED", "User", adminEmail,
                "User " + target.getEmail() + " status set to " + (active ? "Active" : "Inactive"),
                ipAddress, userAgent, "SUCCESS", target.getId(), target.getEmail());
    }

    @Override
    @Transactional
    public void changeUserRole(Long userId, String roleStr, String ipAddress, String userAgent) {
        String adminEmail = SecurityUtils.requireCurrentUserEmail();
        User target = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        if (target.getEmail().equalsIgnoreCase(adminEmail)) {
            throw new IllegalArgumentException("You cannot change your own role.");
        }

        Role newRole = Role.valueOf(roleStr.toUpperCase());
        if (target.getRole() == Role.ADMIN && newRole != Role.ADMIN) {
            long adminCount = userRepository.countByRole(Role.ADMIN);
            if (adminCount <= 1) {
                throw new IllegalStateException("System must always contain at least one administrator.");
            }
        }

        Role oldRole = target.getRole();
        target.setRole(newRole);
        userRepository.save(target);

        saveAuditLog("ROLE_CHANGED", "User", adminEmail,
                "User " + target.getEmail() + " role changed from " + oldRole.name() + " to " + newRole.name(),
                ipAddress, userAgent, "SUCCESS", target.getId(), target.getEmail());
    }

    @Override
    @Transactional
    public void resetUserPassword(Long userId, String newPassword, String ipAddress, String userAgent) {
        String adminEmail = SecurityUtils.requireCurrentUserEmail();
        User target = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        target.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(target);

        saveAuditLog("PASSWORD_RESET", "User", adminEmail,
                "Admin reset password for user: " + target.getEmail(),
                ipAddress, userAgent, "SUCCESS", target.getId(), target.getEmail());
    }

    @Override
    @Transactional
    public void deleteUser(Long userId, String ipAddress, String userAgent) {
        String adminEmail = SecurityUtils.requireCurrentUserEmail();
        User target = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        if (target.getEmail().equalsIgnoreCase(adminEmail)) {
            throw new IllegalArgumentException("You cannot delete your own account.");
        }
        if (target.getRole() == Role.ADMIN) {
            long adminCount = userRepository.countByRole(Role.ADMIN);
            if (adminCount <= 1) {
                throw new IllegalStateException("System must always contain at least one administrator.");
            }
        }

        String targetEmail = target.getEmail();
        Long targetId = target.getId();
        userRepository.delete(target);

        saveAuditLog("USER_DELETED", "User", adminEmail,
                "Admin deleted user: " + targetEmail,
                ipAddress, userAgent, "SUCCESS", targetId, targetEmail);
    }

    // =========================================================================
    // PROJECT MANAGEMENT (Lifecycle only — NEVER exposes contents)
    // =========================================================================

    @Override
    @Transactional(readOnly = true)
    public Page<Project> getAllProjects(String search, String status, Boolean active, Pageable pageable) {
        String searchParam = (search != null && !search.isBlank()) ? search.trim() : null;
        String statusParam = (status != null && !status.isBlank()) ? status.trim() : null;
        return projectRepository.findProjectsWithFilters(searchParam, statusParam, active, pageable);
    }

    @Override
    @Transactional
    public void archiveProject(Long projectId, String ipAddress, String userAgent) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new IllegalArgumentException("Project not found"));
        project.setStatus("ARCHIVED");
        project.setActive(false);
        projectRepository.save(project);

        String adminEmail = SecurityUtils.requireCurrentUserEmail();
        saveAuditLog("PROJECT_ARCHIVED", "Project", adminEmail,
                "Project '" + project.getName() + "' (owner: " + project.getUser().getEmail() + ") archived by admin",
                ipAddress, userAgent, "SUCCESS", project.getId(), null);
    }

    @Override
    @Transactional
    public void restoreProject(Long projectId, String ipAddress, String userAgent) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new IllegalArgumentException("Project not found"));
        project.setStatus("ACTIVE");
        project.setActive(true);
        projectRepository.save(project);

        String adminEmail = SecurityUtils.requireCurrentUserEmail();
        saveAuditLog("PROJECT_RESTORED", "Project", adminEmail,
                "Project '" + project.getName() + "' (owner: " + project.getUser().getEmail() + ") restored by admin",
                ipAddress, userAgent, "SUCCESS", project.getId(), null);
    }

    @Override
    @Transactional
    public void deleteProject(Long projectId, String ipAddress, String userAgent) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new IllegalArgumentException("Project not found"));
        String projectName = project.getName();
        String ownerEmail = project.getUser().getEmail();
        projectRepository.delete(project);

        String adminEmail = SecurityUtils.requireCurrentUserEmail();
        saveAuditLog("PROJECT_DELETED", "Project", adminEmail,
                "Project '" + projectName + "' (owner: " + ownerEmail + ") permanently deleted by admin",
                ipAddress, userAgent, "SUCCESS", projectId, null);
    }

    @Override
    @Transactional
    public void transferProjectOwnership(Long projectId, Long newOwnerId, String ipAddress, String userAgent) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new IllegalArgumentException("Project not found"));
        User newOwner = userRepository.findById(newOwnerId)
                .orElseThrow(() -> new IllegalArgumentException("New owner not found"));

        String oldOwnerEmail = project.getUser().getEmail();
        project.setUser(newOwner);
        projectRepository.save(project);

        String adminEmail = SecurityUtils.requireCurrentUserEmail();
        saveAuditLog("PROJECT_TRANSFERRED", "Project", adminEmail,
                "Project '" + project.getName() + "' ownership transferred from " + oldOwnerEmail + " to " + newOwner.getEmail(),
                ipAddress, userAgent, "SUCCESS", projectId, null);
    }

    // =========================================================================
    // SCANS
    // =========================================================================

    @Override
    @Transactional(readOnly = true)
    public Page<ScanHistory> getAllScans(Pageable pageable) {
        return scanHistoryRepository.findAll(pageable);
    }

    // =========================================================================
    // AUDIT LOGS
    // =========================================================================

    @Override
    @Transactional(readOnly = true)
    public Page<AuditLog> getAllAuditLogs(String action, String userEmail, String resource,
                                           LocalDateTime fromDate, LocalDateTime toDate, Pageable pageable) {
        String actionParam = (action != null && !action.isBlank()) ? action.trim() : null;
        String emailParam = (userEmail != null && !userEmail.isBlank()) ? userEmail.trim() : null;
        String resourceParam = (resource != null && !resource.isBlank()) ? resource.trim() : null;
        
        // Hibernate 6 fails when passing null to temporal parameters, so we use dummy dates if null
        LocalDateTime effectiveFrom = fromDate != null ? fromDate : LocalDateTime.of(1970, 1, 1, 0, 0);
        LocalDateTime effectiveTo = toDate != null ? toDate : LocalDateTime.of(2100, 1, 1, 0, 0);
        
        return auditLogRepository.searchAuditLogs(actionParam, emailParam, resourceParam, effectiveFrom, effectiveTo, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] exportAuditLogsCsv(String action, String userEmail, LocalDateTime fromDate, LocalDateTime toDate) {
        String actionParam = (action != null && !action.isBlank()) ? action.trim() : null;
        String emailParam = (userEmail != null && !userEmail.isBlank()) ? userEmail.trim() : null;
        LocalDateTime effectiveFrom = fromDate != null ? fromDate : LocalDateTime.of(1970, 1, 1, 0, 0);
        LocalDateTime effectiveTo = toDate != null ? toDate : LocalDateTime.of(2100, 1, 1, 0, 0);
        
        List<AuditLog> logs = auditLogRepository.findForExport(actionParam, emailParam, effectiveFrom, effectiveTo);

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             PrintWriter pw = new PrintWriter(baos, true, StandardCharsets.UTF_8)) {

            // UTF-8 BOM for Excel compatibility
            baos.write(new byte[]{(byte) 0xEF, (byte) 0xBB, (byte) 0xBF});

            pw.println("Timestamp,User Email,Action,Resource,Status,IP Address,Details");
            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

            for (AuditLog log : logs) {
                String ts = log.getCreatedAt() != null ? log.getCreatedAt().format(fmt) : "";
                String email = log.getUser() != null ? escapeCsv(log.getUser().getEmail()) : "Anonymous";
                String act = escapeCsv(log.getAction());
                String res = escapeCsv(log.getResource());
                String st = escapeCsv(log.getStatus());
                String ip = escapeCsv(log.getIpAddress());
                String details = escapeCsv(log.getDetails());
                pw.printf("%s,%s,%s,%s,%s,%s,%s%n", ts, email, act, res, st, ip, details);
            }

            pw.flush();
            return baos.toByteArray();
        } catch (Exception e) {
            log.error("Failed to generate audit log CSV export", e);
            throw new RuntimeException("Export failed", e);
        }
    }

    // =========================================================================
    // HELPERS
    // =========================================================================

    private void saveAuditLog(String action, String resource, String actorEmail,
                               String details, String ipAddress, String userAgent,
                               String status, Long targetUserId, String targetUserEmail) {
        try {
            User actor = userRepository.findByEmail(actorEmail).orElse(null);
            AuditLog auditLog = AuditLog.builder()
                    .action(action)
                    .resource(resource)
                    .details(details)
                    .ipAddress(ipAddress)
                    .userAgent(truncate(userAgent, 500))
                    .status(status)
                    .targetUserId(targetUserId)
                    .targetUserEmail(targetUserEmail)
                    .user(actor)
                    .build();
            auditLogRepository.save(auditLog);
        } catch (Exception e) {
            log.error("Failed to write audit log for action '{}': {}", action, e.getMessage());
        }
    }

    private String escapeCsv(String value) {
        if (value == null) return "";
        String escaped = value.replace("\"", "\"\"");
        if (escaped.contains(",") || escaped.contains("\"") || escaped.contains("\n")) {
            return "\"" + escaped + "\"";
        }
        return escaped;
    }

    private String truncate(String value, int maxLength) {
        if (value == null) return null;
        return value.length() > maxLength ? value.substring(0, maxLength) : value;
    }
}
