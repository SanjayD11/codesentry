package com.sanjay.aisecurity.mapper;

import com.sanjay.aisecurity.dto.request.CreateProjectRequest;
import com.sanjay.aisecurity.dto.request.UpdateProjectRequest;
import com.sanjay.aisecurity.dto.response.ProjectResponse;
import com.sanjay.aisecurity.dto.response.ProjectSummaryResponse;
import com.sanjay.aisecurity.entity.Project;
import com.sanjay.aisecurity.entity.User;
import org.springframework.stereotype.Component;

/**
 * Project Mapper.
 *
 * <p>Handles mappings between the Project JPA entity and request/response DTOs.</p>
 *
 * @author Sanjay
 * @version 1.0.0
 */
@Component
public class ProjectMapper {

    /**
     * Converts a CreateProjectRequest DTO to a Project JPA entity.
     *
     * @param request the create request DTO
     * @param owner   the authenticated user who will own this project
     * @return the unpersisted Project entity
     */
    public Project toEntity(CreateProjectRequest request, User owner) {
        return Project.builder()
                .name(request.getProjectName().trim())
                .description(request.getDescription() != null ? request.getDescription().trim() : null)
                .version(request.getVersion() != null ? request.getVersion().trim() : null)
                .projectType(request.getProjectType())
                .user(owner)
                .active(true)
                .status("ACTIVE")
                .build();
    }

    /**
     * Converts a Project entity to a ProjectResponse DTO.
     *
     * @param project the domain entity
     * @return the populated response DTO
     */
    public ProjectResponse toResponse(Project project) {
        return ProjectResponse.from(project);
    }

    /**
     * Converts a Project entity to a ProjectSummaryResponse DTO.
     *
     * @param project the domain entity
     * @return the summary DTO
     */
    public ProjectSummaryResponse toSummary(Project project) {
        return ProjectSummaryResponse.from(project);
    }

    /**
     * Applies updates from an UpdateProjectRequest to an existing Project entity.
     *
     * @param project the existing Project entity
     * @param request the update request DTO containing new values
     */
    public void applyUpdate(Project project, UpdateProjectRequest request) {
        if (request.getProjectName() != null && !request.getProjectName().isBlank()) {
            project.setName(request.getProjectName().trim());
        }
        if (request.getDescription() != null) {
            project.setDescription(request.getDescription().trim());
        }
        if (request.getVersion() != null) {
            project.setVersion(request.getVersion().trim());
        }
        if (request.getProjectType() != null) {
            project.setProjectType(request.getProjectType());
        }
        if (request.getActive() != null) {
            project.setActive(request.getActive());
        }
        if (request.getStatus() != null && !request.getStatus().isBlank()) {
            project.setStatus(request.getStatus().toUpperCase().trim());
        }
    }
}
