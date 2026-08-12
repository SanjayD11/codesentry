package com.sanjay.aisecurity.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Request body for the Firebase-backed password reset confirmation endpoint.
 *
 * <p>Received after Firebase has already validated the oobCode and the user has
 * signed in with their new credentials. The frontend provides:
 * <ul>
 *   <li>{@code firebaseIdToken} — a fresh Firebase ID token proving the reset was authentic</li>
 *   <li>{@code newPassword} — the plain-text password to BCrypt-hash and store in MySQL</li>
 * </ul>
 * The backend verifies the ID token server-side via Firebase Admin SDK, identifies
 * the user by their email, hashes the password, and updates the MySQL record.</p>
 *
 * @author Sanjay
 * @version 1.0.0
 */
@Data
public class ConfirmPasswordResetRequest {

    @NotBlank(message = "Firebase ID token is required.")
    private String firebaseIdToken;

    @NotBlank(message = "New password is required.")
    @Size(min = 8, max = 128, message = "Password must be between 8 and 128 characters.")
    private String newPassword;
}
