package com.sanjay.aisecurity.controller;

import com.sanjay.aisecurity.common.ApiResponse;
import com.sanjay.aisecurity.constants.ApiConstants;
import com.sanjay.aisecurity.constants.MessageConstants;
import com.sanjay.aisecurity.dto.AuthenticationResponse;
import com.sanjay.aisecurity.dto.LoginRequest;
import com.sanjay.aisecurity.dto.RegisterRequest;
import com.sanjay.aisecurity.dto.UserResponse;
import com.sanjay.aisecurity.service.AuthService;
import com.sanjay.aisecurity.service.EmailService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.Map;

/**
 * Authentication Controller.
 *
 * <p>Exposes three endpoints for the authentication lifecycle:</p>
 * <ul>
 *   <li>{@code POST /api/v1/auth/register} — create a new account and receive a JWT</li>
 *   <li>{@code POST /api/v1/auth/login}    — authenticate with credentials and receive a JWT</li>
 *   <li>{@code GET  /api/v1/auth/me}       — return the profile of the token-bearing caller</li>
 * </ul>
 *
 * <p>All request bodies are validated via Jakarta Bean Validation ({@code @Valid}).
 * Validation failures are caught centrally by
 * {@link com.sanjay.aisecurity.exception.GlobalExceptionHandler}.</p>
 *
 * @author Sanjay
 * @version 1.0.0
 */
@Slf4j
@RestController
@RequestMapping(ApiConstants.AUTH_BASE)
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "User registration, login, and profile endpoints")
public class AuthController {

    private final AuthService authService;
    private final EmailService emailService;

    // =========================================================================
    // REGISTER
    // =========================================================================

    /**
     * Registers a new user and returns a JWT access token.
     *
     * @param request validated registration payload
     * @return 201 Created with the JWT and user profile
     */
    @PostMapping("/register")
    @Operation(
        summary = "Register a new user account",
        description = "Creates a new user with the USER role, encrypts the password, "
                    + "and returns a signed JWT access token."
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "201", description = "User registered successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "400", description = "Validation errors in request body"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "409", description = "Email address already registered")
    })
    public ResponseEntity<ApiResponse<AuthenticationResponse>> register(
            @Valid @RequestBody RegisterRequest request) {

        log.debug("Register request for email: {}", request.getEmail());
        AuthenticationResponse authResponse = authService.register(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.created(MessageConstants.USER_REGISTERED_SUCCESS, authResponse));
    }

    // =========================================================================
    // LOGIN
    // =========================================================================

    /**
     * Authenticates an existing user and returns a fresh JWT access token.
     *
     * @param request validated login payload
     * @return 200 OK with the JWT and user profile
     */
    @PostMapping("/login")
    @Operation(
        summary = "Login with email and password",
        description = "Authenticates credentials against the database and returns "
                    + "a signed JWT Bearer token valid for 24 hours."
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200", description = "Login successful"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "401", description = "Invalid credentials"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "403", description = "Account is disabled")
    })
    public ResponseEntity<ApiResponse<AuthenticationResponse>> login(
            @Valid @RequestBody LoginRequest request) {

        log.debug("Login request for email: {}", request.getEmail());
        AuthenticationResponse authResponse = authService.login(request);
        return ResponseEntity.ok(ApiResponse.success(MessageConstants.LOGIN_SUCCESS, authResponse));
    }

    // =========================================================================
    // CURRENT USER
    // =========================================================================

    /**
     * Returns the public profile of the currently authenticated user.
     *
     * <p>The caller must supply a valid {@code Authorization: Bearer <token>} header.</p>
     *
     * @return 200 OK with the user's profile
     */
    @GetMapping("/me")
    @Operation(
        summary = "Get authenticated user profile",
        description = "Returns the public profile of the user identified by the Bearer JWT.",
        security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200", description = "Profile retrieved successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "401", description = "Missing or invalid JWT token")
    })
    public ResponseEntity<ApiResponse<UserResponse>> getCurrentUser() {
        UserResponse userResponse = authService.getCurrentUser();
        return ResponseEntity.ok(ApiResponse.success(MessageConstants.SUCCESS, userResponse));
    }

    // =========================================================================
    // FORGOT PASSWORD
    // =========================================================================

    @PostMapping("/forgot-password")
    @Operation(
        summary = "Initiate password reset",
        description = "Generates a native reset link and emails it using Gmail SMTP."
    )
    public ResponseEntity<ApiResponse<String>> forgotPassword(@RequestBody Map<String, String> body) {
        String email = body.getOrDefault("email", "").trim().toLowerCase();
        if (email.isBlank()) {
            return ResponseEntity.badRequest().body(ApiResponse.badRequest("Email address is required."));
        }
        try {
            authService.forgotPassword(email);
            return ResponseEntity.ok(ApiResponse.success("If that email exists, a reset link has been sent.", null));
        } catch (RuntimeException e) {
            log.error("Forgot-password failed for email [{}]: {}", email, e.getMessage());
            return ResponseEntity.internalServerError()
                .body(ApiResponse.internalError("Failed to send reset email. Please try again later."));
        }
    }

    @PostMapping("/reset-password")
    @Operation(
        summary = "Reset password using native token",
        description = "Validates the native reset token and sets the new password."
    )
    public ResponseEntity<ApiResponse<String>> resetPassword(@RequestBody Map<String, String> body) {
        String token = body.getOrDefault("token", "");
        String newPassword = body.getOrDefault("newPassword", "");
        
        try {
            authService.resetPassword(token, newPassword);
            return ResponseEntity.ok(ApiResponse.success("Password successfully updated in the database.", null));
        } catch (IllegalArgumentException e) {
            log.error("Failed to reset password: {}", e.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.badRequest(e.getMessage()));
        }
    }

    @PostMapping("/verify-email")
    @Operation(summary = "Verify user email using token")
    public ResponseEntity<ApiResponse<String>> verifyEmail(@RequestBody Map<String, String> body) {
        String token = body.getOrDefault("token", "");
        try {
            authService.verifyEmail(token);
            return ResponseEntity.ok(ApiResponse.success("Email verified successfully. You can now log in.", null));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponse.badRequest(e.getMessage()));
        }
    }

    @PostMapping("/resend-verification")
    @Operation(summary = "Resend email verification link")
    public ResponseEntity<ApiResponse<String>> resendVerification(@RequestBody Map<String, String> body) {
        String email = body.getOrDefault("email", "");
        try {
            authService.resendVerificationEmail(email);
            return ResponseEntity.ok(ApiResponse.success("Verification link sent.", null));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponse.badRequest(e.getMessage()));
        }
    }

    // =========================================================================
    // CHANGE PASSWORD
    // =========================================================================

    @PutMapping("/change-password")
    @Operation(
        summary = "Change current user password",
        description = "Validates the current password then replaces it with the new one.",
        security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<ApiResponse<String>> changePassword(@RequestBody Map<String, String> body) {
        String currentPassword = body.getOrDefault("currentPassword", "");
        String newPassword = body.getOrDefault("newPassword", "");
        authService.changePassword(currentPassword, newPassword);
        return ResponseEntity.ok(ApiResponse.success("Password changed successfully.", null));
    }

    @PutMapping("/profile")
    @Operation(
        summary = "Update profile",
        description = "Updates the first name and last name of the authenticated user.",
        security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<ApiResponse<UserResponse>> updateProfile(@RequestBody Map<String, String> body) {
        String firstName = body.getOrDefault("firstName", null);
        String lastName  = body.getOrDefault("lastName",  null);
        UserResponse updated = authService.updateProfile(firstName, lastName);
        return ResponseEntity.ok(ApiResponse.success("Profile updated successfully.", updated));
    }

    // =========================================================================
    // FIREBASE / OAUTH LOGIN
    // =========================================================================

    /**
     * Authenticates a user via a Firebase ID token (Google/GitHub OAuth).
     *
     * <p>The React frontend completes the Firebase popup login and sends
     * the resulting ID token here. The backend verifies it, finds or creates
     * the local user, and returns a custom Spring Boot JWT.</p>
     *
     * @param body must contain a {@code token} key with the Firebase ID token
     * @return 200 OK with the JWT and user profile
     */
    @PostMapping("/firebase-login")
    @Operation(
        summary = "Login via Firebase (Google/GitHub OAuth)",
        description = "Verifies a Firebase ID token, finds or auto-creates the local user, "
                    + "and returns a signed Spring Boot JWT Bearer token."
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200", description = "OAuth login successful"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "400", description = "Invalid or missing Firebase token"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "500", description = "Firebase Admin SDK not initialized")
    })
    public ResponseEntity<ApiResponse<AuthenticationResponse>> firebaseLogin(
            @RequestBody Map<String, String> body) {

        String firebaseToken = body.getOrDefault("token", "");
        if (firebaseToken.isBlank()) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.badRequest("Firebase token is required."));
        }

        log.debug("Firebase login request received");
        AuthenticationResponse authResponse = authService.firebaseLogin(firebaseToken);
        return ResponseEntity.ok(ApiResponse.success("Login successful.", authResponse));
    }
}
