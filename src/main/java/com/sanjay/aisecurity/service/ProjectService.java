package com.sanjay.aisecurity.service;

import com.sanjay.aisecurity.dto.request.CreateProjectRequest;
import com.sanjay.aisecurity.dto.request.ProjectSearchRequest;
import com.sanjay.aisecurity.dto.request.UpdateProjectRequest;
import com.sanjay.aisecurity.dto.response.ProjectListResponse;
import com.sanjay.aisecurity.dto.response.ProjectResponse;
import com.sanjay.aisecurity.dto.response.ProjectStatisticsResponse;
import org.springframework.data.domain.Pageable;

/**
 * Project Service Interface.
 *
 * <p>Defines the contract for all operations related to managing user projects.</p>
 *
 * @author Sanjay
 * @version 1.0.0
 */
public interface ProjectService {

    /**
     * Creates a new project.
     *
     * @param request the create request details
     * @return the created project details
     */
    ProjectResponse createProject(CreateProjectRequest request);

    /**
     * Retrieves full details of a specific project by ID.
     *
     * @param projectId the project ID
     * @return the project details
     */
    ProjectResponse getProject(Long projectId);

    /**
     * Retrieves all active projects owned by the currently authenticated user with pagination and sorting.
     *
     * @param pageable pagination details
     * @return the paginated list of projects
     */
    ProjectListResponse getMyProjects(Pageable pageable);

    /**
     * Retrieves all projects in the system (Admin only).
     *
     * @param pageable pagination details
     * @return the paginated list of all projects
     */
    ProjectListResponse getAllProjects(Pageable pageable);

    /**
     * Updates an existing project.
     *
     * @param projectId the project ID to update
     * @param request   the update values
     * @return the updated project details
     */
    ProjectResponse updateProject(Long projectId, UpdateProjectRequest request);

    /**
     * Deletes a project by ID (soft delete).
     *
     * @param projectId the project ID
     */
    void deleteProject(Long projectId);

    /**
     * Searches for projects matching the criteria in ProjectSearchRequest.
     *
     * @param request  the search criteria DTO
     * @param pageable pagination details
     * @return the matching list of projects
     */
    ProjectListResponse searchProjects(ProjectSearchRequest request, Pageable pageable);

    /**
     * Retrieves aggregated statistics of projects owned by the currently authenticated user.
     *
     * @return the project statistics
     */
    ProjectStatisticsResponse getProjectStatistics();
}
