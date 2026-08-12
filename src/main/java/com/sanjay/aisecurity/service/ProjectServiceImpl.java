package com.sanjay.aisecurity.service;

import com.sanjay.aisecurity.constants.MessageConstants;
import com.sanjay.aisecurity.dto.request.CreateProjectRequest;
import com.sanjay.aisecurity.dto.request.ProjectSearchRequest;
import com.sanjay.aisecurity.dto.request.UpdateProjectRequest;
import com.sanjay.aisecurity.dto.response.ProjectListResponse;
import com.sanjay.aisecurity.dto.response.ProjectResponse;
import com.sanjay.aisecurity.dto.response.ProjectStatisticsResponse;
import com.sanjay.aisecurity.dto.response.ProjectSummaryResponse;
import com.sanjay.aisecurity.entity.Project;
import com.sanjay.aisecurity.entity.User;
import com.sanjay.aisecurity.enums.ProjectType;
import com.sanjay.aisecurity.exception.ResourceNotFoundException;
import com.sanjay.aisecurity.exception.UserAlreadyExistsException;
import com.sanjay.aisecurity.mapper.ProjectMapper;
import com.sanjay.aisecurity.repository.ProjectRepository;
import com.sanjay.aisecurity.repository.ScanHistoryRepository;
import com.sanjay.aisecurity.repository.UserRepository;
import com.sanjay.aisecurity.repository.VulnerabilityRepository;
import com.sanjay.aisecurity.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Service implementation for managing project operations.
 *
 * @author Sanjay
 * @version 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProjectServiceImpl implements ProjectService {

    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;
    private final ScanHistoryRepository scanHistoryRepository;
    private final VulnerabilityRepository vulnerabilityRepository;
    private final ProjectMapper projectMapper;

    @Override
    @Transactional
    public ProjectResponse createProject(CreateProjectRequest request) {
        String email = SecurityUtils.requireCurrentUserEmail();
        User owner = loadUserByEmail(email);

        // Enforce project name uniqueness per user
        if (projectRepository.existsByNameAndUserEmailAndActiveTrue(request.getProjectName().trim(), email)) {
            throw new UserAlreadyExistsException(
                    "A project named '" + request.getProjectName() + "' already exists in your account.");
        }

        Project project = projectMapper.toEntity(request, owner);
        Project savedProject = projectRepository.save(project);
        log.info("Project created: ID = {}, Name = {} for user {}", savedProject.getId(), savedProject.getName(), email);

        return projectMapper.toResponse(savedProject);
    }

    @Override
    @Transactional(readOnly = true)
    public ProjectResponse getProject(Long projectId) {
        String email = SecurityUtils.requireCurrentUserEmail();
        Project project = resolveOwnedProject(projectId, email);

        return projectMapper.toResponse(project);
    }

    @Override
    @Transactional(readOnly = true)
    public ProjectListResponse getMyProjects(Pageable pageable) {
        String email = SecurityUtils.requireCurrentUserEmail();
        Page<Project> page = projectRepository.findByUserEmailAndActiveTrueAndNameNot(email, "[Direct Scans]", pageable);
        return toListResponse(page);
    }

    @Override
    @Transactional(readOnly = true)
    public ProjectListResponse getAllProjects(Pageable pageable) {
        Page<Project> page = projectRepository.findAll(pageable);
        return toListResponse(page);
    }

    @Override
    @Transactional
    public ProjectResponse updateProject(Long projectId, UpdateProjectRequest request) {
        String email = SecurityUtils.requireCurrentUserEmail();
        Project project = resolveOwnedProject(projectId, email);

        // If rename is requested, enforce uniqueness
        if (request.getProjectName() != null && !request.getProjectName().trim().equalsIgnoreCase(project.getName())) {
            if (projectRepository.existsByNameAndUserEmailAndActiveTrueExcludingId(
                    request.getProjectName().trim(), email, projectId)) {
                throw new UserAlreadyExistsException(
                        "A project named '" + request.getProjectName() + "' already exists in your account.");
            }
        }

        projectMapper.applyUpdate(project, request);
        Project updated = projectRepository.save(project);
        log.info("Project updated: ID = {} by user {}", projectId, email);

        return projectMapper.toResponse(updated);
    }

    @Override
    @Transactional
    public void deleteProject(Long projectId) {
        String email = SecurityUtils.requireCurrentUserEmail();
        Project project = resolveOwnedProject(projectId, email);

        // Perform Soft Delete
        project.setActive(false);
        projectRepository.save(project);
        log.info("Project soft-deleted: ID = {} by user {}", projectId, email);
    }

    @Override
    @Transactional(readOnly = true)
    public ProjectListResponse searchProjects(ProjectSearchRequest request, Pageable pageable) {
        String email = SecurityUtils.requireCurrentUserEmail();
        Page<Project> page = projectRepository.searchProjects(
                email,
                request.getProjectName() != null ? request.getProjectName().trim() : null,
                request.getProjectType(),
                request.getActive(),
                request.getCreatedAfter(),
                request.getCreatedBefore(),
                pageable
        );
        return toListResponse(page);
    }

    @Override
    @Transactional(readOnly = true)
    public ProjectStatisticsResponse getProjectStatistics() {
        String email = SecurityUtils.requireCurrentUserEmail();

        long activeCount = projectRepository.countByUserEmailAndActiveTrueAndNameNot(email, "[Direct Scans]");
        long inactiveCount = projectRepository.countByUserEmailAndActiveFalseAndNameNot(email, "[Direct Scans]");
        long totalProjects = activeCount + inactiveCount;

        // Scans and vulnerabilities counts
        List<com.sanjay.aisecurity.entity.ScanHistory> scans =
                scanHistoryRepository.findByProjectUserEmailOrderByCreatedAtDesc(email);
        long totalScans = scans.size();

        long totalVulnerabilities = vulnerabilityRepository
                .findByScanHistoryProjectUserEmail(email).size();

        // Total files uploaded
        Long totalFiles = projectRepository.sumTotalFilesByUserEmailAndActiveTrueAndNameNot(email);

        // Risk score metrics
        Double avgSecurityScore = projectRepository.averageSecurityScoreByUserEmailAndActiveTrueAndNameNot(email);
        double avgRiskScore = avgSecurityScore != null ? Math.max(0.0, 100.0 - avgSecurityScore) : 0.0;

        // Group by type for active projects
        Map<String, Long> byType = new LinkedHashMap<>();
        for (ProjectType type : ProjectType.values()) {
            long count = projectRepository.countByUserEmailAndActiveTrueAndProjectType(email, type);
            if (count > 0) {
                byType.put(type.name(), count);
            }
        }

        // Most recent project
        List<Project> myProjects = projectRepository.findByUserEmailAndActiveTrueAndNameNot(email, "[Direct Scans]");
        Project mostRecent = myProjects.stream()
                .max((a, b) -> a.getCreatedAt().compareTo(b.getCreatedAt()))
                .orElse(null);

        return ProjectStatisticsResponse.builder()
                .totalProjects(totalProjects)
                .activeProjects(activeCount)
                .inactiveProjects(inactiveCount)
                .totalScans(totalScans)
                .totalUploadedFiles(totalFiles != null ? totalFiles : 0L)
                .totalVulnerabilities(totalVulnerabilities)
                .averageProjectRiskScore(Math.round(avgRiskScore * 100.0) / 100.0)
                .lastProjectCreatedName(mostRecent != null ? mostRecent.getName() : null)
                .lastProjectCreatedAt(mostRecent != null ? mostRecent.getCreatedAt() : null)
                .projectsByType(byType)
                .build();
    }

    // =========================================================================
    // PRIVATE HELPERS
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
        
        if (!project.isActive() || "[Direct Scans]".equals(project.getName())) {
            throw new ResourceNotFoundException(MessageConstants.PROJECT_NOT_FOUND);
        }
        
        return project;
    }

    private ProjectListResponse toListResponse(Page<Project> page) {
        List<ProjectSummaryResponse> list = page.getContent()
                .stream()
                .map(projectMapper::toSummary)
                .collect(Collectors.toList());

        return ProjectListResponse.builder()
                .projects(list)
                .pageNumber(page.getNumber())
                .pageSize(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .isFirst(page.isFirst())
                .isLast(page.isLast())
                .build();
    }
}
