package com.sanjay.aisecurity.service;

import com.sanjay.aisecurity.dto.AuthenticationResponse;
import com.sanjay.aisecurity.dto.LoginRequest;
import com.sanjay.aisecurity.dto.RegisterRequest;
import com.sanjay.aisecurity.dto.UserResponse;

/**
 * Authentication Service Interface.
 *
 * <p>Defines the contract for user registration, login, and profile retrieval.
 * Decouples the controller layer from the concrete implementation,
 * enabling future alternative implementations (e.g. OAuth2, SSO).</p>
 *
 * @author Sanjay
 * @version 1.0.0
 */
public interface AuthService {

    /**
     * Registers a new user account.
     *
     * <p>Validates that no account already exists for the provided email,
     * encrypts the password, assigns the default {@code USER} role,
     * persists the account, and returns a signed JWT.</p>
     *
     * @param request the registration payload
     * @return an {@link AuthenticationResponse} containing the JWT and user profile
     * @throws com.sanjay.aisecurity.exception.UserAlreadyExistsException
     *         if the email is already registered
     */
    AuthenticationResponse register(RegisterRequest request);

    /**
     * Authenticates an existing user with their credentials.
     *
     * <p>Delegates credential verification to the
     * {@link org.springframework.security.authentication.AuthenticationManager},
     * then generates and returns a fresh JWT.</p>
     *
     * @param request the login payload
     * @return an {@link AuthenticationResponse} containing the JWT and user profile
     * @throws org.springframework.security.authentication.BadCredentialsException
     *         if the email/password combination is incorrect
     * @throws org.springframework.security.authentication.DisabledException
     *         if the account has been deactivated
     */
    AuthenticationResponse login(LoginRequest request);

    /**
     * Returns the public profile of the currently authenticated user.
     *
     * <p>Reads the email from the {@link org.springframework.security.core.context.SecurityContextHolder}
     * via {@link com.sanjay.aisecurity.util.SecurityUtils}.</p>
     *
     * @return a {@link UserResponse} for the authenticated principal
     * @throws com.sanjay.aisecurity.exception.ResourceNotFoundException
     *         if the authenticated user no longer exists in the database
     */
    UserResponse getCurrentUser();

    /**
     * Initiates the password reset flow natively via Spring Boot.
     * Always returns success regardless of whether the email exists (anti-enumeration).
     *
     * @param email the user's email address
     */
    void forgotPassword(String email);

    /**
     * Resets the password natively via Spring Boot using a token.
     *
     * @param token       the unique reset token
     * @param newPassword the new password to set
     */
    void resetPassword(String token, String newPassword);

    /**
     * Verifies the user's email address using a valid verification token.
     *
     * @param token the unique verification token
     */
    void verifyEmail(String token);

    /**
     * Resends the verification email to the provided email address.
     *
     * @param email the user's email address
     */
    void resendVerificationEmail(String email);

    /**
     * Changes the password for the currently authenticated user.
     *
     * @param currentPassword the user's current password
     * @param newPassword     the desired new password
     */
    void changePassword(String currentPassword, String newPassword);

    /**
     * Updates the first name and last name of the currently authenticated user.
     *
     * @param firstName updated first name
     * @param lastName  updated last name
     * @return updated user profile
     */
    UserResponse updateProfile(String firstName, String lastName);

    /**
     * Authenticates a user via a Firebase ID token (Google/GitHub OAuth).
     *
     * <p>Verifies the Firebase token, finds or auto-creates the local user,
     * and returns a Spring Boot JWT. Firebase is used only as an identity
     * provider — all authorization remains in the Spring Boot backend.</p>
     *
     * @param firebaseIdToken the raw Firebase ID token from the frontend
     * @return an {@link AuthenticationResponse} containing the custom JWT and user profile
     */
    AuthenticationResponse firebaseLogin(String firebaseIdToken);

}
