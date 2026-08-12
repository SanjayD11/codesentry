package com.sanjay.aisecurity.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Request DTO for an admin editing an existing user's metadata.
 * Does NOT allow editing passwords directly (use reset password endpoint instead).
 *
 * @author Sanjay
 * @version 1.0.0
 */
@Data
public class AdminUpdateUserRequest {

    @NotBlank(message = "First name is required")
    @Size(max = 50)
    private String firstName;

    @NotBlank(message = "Last name is required")
    @Size(max = 50)
    private String lastName;

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    @Size(max = 100)
    private String email;
}
