package com.sanjay.aisecurity.dto.request;

import com.sanjay.aisecurity.enums.ProjectType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Project Search Request DTO.
 *
 * @author Sanjay
 * @version 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProjectSearchRequest {

    private String projectName;
    private ProjectType projectType;
    private Boolean active;
    private LocalDateTime createdAfter;
    private LocalDateTime createdBefore;
}
