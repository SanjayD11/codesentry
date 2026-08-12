package com.sanjay.aisecurity.controller;

import com.sanjay.aisecurity.common.ApiResponse;
import com.sanjay.aisecurity.constants.ApiConstants;
import com.sanjay.aisecurity.constants.MessageConstants;
import com.sanjay.aisecurity.dto.request.CreateProjectRequest;
import com.sanjay.aisecurity.dto.request.ProjectSearchRequest;
import com.sanjay.aisecurity.dto.request.UpdateProjectRequest;
import com.sanjay.aisecurity.dto.response.ProjectListResponse;
import com.sanjay.aisecurity.dto.response.ProjectResponse;
import com.sanjay.aisecurity.dto.response.ProjectStatisticsResponse;
import com.sanjay.aisecurity.enums.ProjectType;
import com.sanjay.aisecurity.service.ProjectService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

/**
 * Controller class for project management REST endpoints.
 *
 * @author Sanjay
 * @version 1.0.0
 */
@Slf4j
@RestController
@RequestMapping(ApiConstants.PROJECT_BASE)
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Projects", description = "Endpoints for creating, updating, deleting, listing and searching user projects.")
public class ProjectController {

    private final ProjectService projectService;

    /**
     * Creates a new project for the authenticated user.
     *
     * @param request the create project request payload
     * @return the created project detail response
     */
    @PostMapping
    @Operation(summary = "Create project", description = "Creates a new project owned by the authenticated user.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Project created successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request payload"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Project name collision")
    })
    public ResponseEntity<ApiResponse<ProjectResponse>> createProject(
            @Valid @RequestBody CreateProjectRequest request) {
        ProjectResponse response = projectService.createProject(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(MessageConstants.PROJECT_CREATED, response));
    }

    /**
     * Lists the projects belonging to the authenticated user (or all projects if administrator is viewing and all=true).
     *
     * @param page      the page number
     * @param size      the size of page
     * @param sortBy    the field to sort by
     * @param sortDir   the direction of sorting
     * @param adminView flag to allow admins to see all projects
     * @return the list response containing projects
     */
    @GetMapping
    @Operation(summary = "List projects", description = "Returns a paginated list of projects owned by the user. Admins can view all projects if adminView=true.")
    public ResponseEntity<ApiResponse<ProjectListResponse>> getProjects(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir,
            @RequestParam(defaultValue = "false") boolean adminView) {

        Pageable pageable = buildPageable(page, size, sortBy, sortDir);
        ProjectListResponse response;
        
        if (adminView && isCurrentUserAdmin()) {
            response = projectService.getAllProjects(pageable);
        } else {
            response = projectService.getMyProjects(pageable);
        }
        
        return ResponseEntity.ok(ApiResponse.success(MessageConstants.PROJECT_FETCH_SUCCESS, response));
    }

    /**
     * Retrieves details of a specific project by ID.
     *
     * @param projectId the project ID
     * @return the project detail response
     */
    @GetMapping("/{projectId}")
    @Operation(summary = "Get project", description = "Retrieves full details of a specific project. Enforces ownership.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Project retrieved successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Project not found or access denied")
    })
    public ResponseEntity<ApiResponse<ProjectResponse>> getProject(
            @PathVariable Long projectId) {
        ProjectResponse response = projectService.getProject(projectId);
        return ResponseEntity.ok(ApiResponse.success(MessageConstants.PROJECT_FETCH_SUCCESS, response));
    }

    /**
     * Updates an existing project.
     *
     * @param projectId the project ID
     * @param request   the update project payload
     * @return the updated project detail response
     */
    @PutMapping("/{projectId}")
    @Operation(summary = "Update project", description = "Partially updates an existing project. Only non-null fields will be modified.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Project updated successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request payload"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Project not found")
    })
    public ResponseEntity<ApiResponse<ProjectResponse>> updateProject(
            @PathVariable Long projectId,
            @Valid @RequestBody UpdateProjectRequest request) {
        ProjectResponse response = projectService.updateProject(projectId, request);
        return ResponseEntity.ok(ApiResponse.success(MessageConstants.PROJECT_UPDATED, response));
    }

    /**
     * Deletes a project (soft delete).
     *
     * @param projectId the project ID
     * @return an empty api response
     */
    @DeleteMapping("/{projectId}")
    @Operation(summary = "Delete project", description = "Performs a soft delete on a project (sets active=false).")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Project deleted successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Project not found")
    })
    public ResponseEntity<ApiResponse<Void>> deleteProject(
            @PathVariable Long projectId) {
        projectService.deleteProject(projectId);
        return ResponseEntity.ok(ApiResponse.success(MessageConstants.PROJECT_DELETED));
    }

    /**
     * Searches projects dynamically based on query criteria.
     *
     * @param projectName   partial project name search
     * @param projectType   exact project type filter
     * @param active        active status filter
     * @param createdAfter  creation date range start
     * @param createdBefore creation date range end
     * @param page          page number
     * @param size          page size
     * @param sortBy        sort field
     * @param sortDir       sort direction
     * @return the paginated search results
     */
    @GetMapping("/search")
    @Operation(summary = "Search projects", description = "Allows dynamic query parameters to search and filter user projects.")
    public ResponseEntity<ApiResponse<ProjectListResponse>> searchProjects(
            @RequestParam(required = false) String projectName,
            @RequestParam(required = false) ProjectType projectType,
            @RequestParam(required = false) Boolean active,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime createdAfter,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime createdBefore,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {

        Pageable pageable = buildPageable(page, size, sortBy, sortDir);
        ProjectSearchRequest searchRequest = ProjectSearchRequest.builder()
                .projectName(projectName)
                .projectType(projectType)
                .active(active)
                .createdAfter(createdAfter)
                .createdBefore(createdBefore)
                .build();

        ProjectListResponse response = projectService.searchProjects(searchRequest, pageable);
        return ResponseEntity.ok(ApiResponse.success(MessageConstants.PROJECT_FETCH_SUCCESS, response));
    }

    /**
     * Retrieves statistics about projects owned by the user.
     *
     * @return statistics response
     */
    @GetMapping("/statistics")
    @Operation(summary = "Get statistics", description = "Retrieves project counts, scan counts, and risk metrics for the user's projects.")
    public ResponseEntity<ApiResponse<ProjectStatisticsResponse>> getStatistics() {
        ProjectStatisticsResponse response = projectService.getProjectStatistics();
        return ResponseEntity.ok(ApiResponse.success(MessageConstants.SUCCESS, response));
    }

    // =========================================================================
    // PRIVATE HELPERS
    // =========================================================================

    private Pageable buildPageable(int page, int size, String sortBy, String sortDir) {
        int clampedSize = Math.min(size, ApiConstants.MAX_PAGE_SIZE);
        Sort.Direction direction = "asc".equalsIgnoreCase(sortDir) ? Sort.Direction.ASC : Sort.Direction.DESC;
        return PageRequest.of(Math.max(page, 0), clampedSize, Sort.by(direction, sortBy));
    }

    private boolean isCurrentUserAdmin() {
        return org.springframework.security.core.context.SecurityContextHolder
                .getContext()
                .getAuthentication()
                .getAuthorities()
                .stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
    }
}
