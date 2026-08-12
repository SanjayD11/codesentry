package com.sanjay.aisecurity.dto.request;

import com.sanjay.aisecurity.enums.ProjectType;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Update Project Request DTO.
 *
 * @author Sanjay
 * @version 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateProjectRequest {

    @Size(min = 2, max = 100, message = "Project name must be between 2 and 100 characters")
    private String projectName;

    @Size(max = 500, message = "Description must be less than 500 characters")
    private String description;

    @Pattern(
        regexp = "^[0-9a-zA-Z.\\-_]{1,20}$",
        message = "Version must be alphanumeric with dots, hyphens or underscores (max 20 chars)"
    )
    private String version;

    private ProjectType projectType;

    private Boolean active;

    @Size(max = 20, message = "Status must be less than 20 characters")
    private String status;
}
