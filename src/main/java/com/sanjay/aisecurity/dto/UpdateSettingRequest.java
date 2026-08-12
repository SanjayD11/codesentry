package com.sanjay.aisecurity.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Request DTO for creating or updating a platform setting.
 *
 * @author Sanjay
 * @version 1.0.0
 */
@Data
public class UpdateSettingRequest {

    @NotBlank(message = "Value is required")
    @Size(max = 5000, message = "Value must be less than 5000 characters")
    private String value;
}
