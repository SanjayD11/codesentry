package com.sanjay.aisecurity.service;

import com.sanjay.aisecurity.constants.MessageConstants;
import com.sanjay.aisecurity.dto.AuthenticationResponse;
import com.sanjay.aisecurity.dto.LoginRequest;
import com.sanjay.aisecurity.dto.RegisterRequest;
import com.sanjay.aisecurity.dto.UserResponse;
import com.sanjay.aisecurity.entity.User;
import com.sanjay.aisecurity.enums.AuthProvider;
import com.sanjay.aisecurity.enums.Role;
import com.sanjay.aisecurity.exception.ResourceNotFoundException;
import com.sanjay.aisecurity.exception.UserAlreadyExistsException;
import com.sanjay.aisecurity.repository.UserRepository;
import com.sanjay.aisecurity.security.CustomUserDetails;
import com.sanjay.aisecurity.security.FirebaseTokenVerifier;
import com.sanjay.aisecurity.security.JwtService;
import com.sanjay.aisecurity.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.sanjay.aisecurity.entity.PasswordResetToken;
import com.sanjay.aisecurity.entity.EmailVerificationToken;
import com.sanjay.aisecurity.entity.AuditLog;
import com.sanjay.aisecurity.repository.PasswordResetTokenRepository;
import com.sanjay.aisecurity.repository.EmailVerificationTokenRepository;
import com.sanjay.aisecurity.repository.AuditLogRepository;
import java.util.UUID;
import java.time.LocalDateTime;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;

/**
 * Authentication Service Implementation.
 *
 * <p>Provides the complete authentication lifecycle: user registration (with
 * duplicate guard and password encryption), login (credential verification
 * via Spring's {@link AuthenticationManager}), and current-user retrieval.</p>
 *
 * <p>All mutating operations are wrapped in a transaction to ensure atomicity.</p>
 *
 * @author Sanjay
 * @version 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final FirebaseTokenVerifier firebaseTokenVerifier;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final EmailVerificationTokenRepository emailVerificationTokenRepository;
    private final AuditLogRepository auditLogRepository;
    private final EmailService emailService;

    /** Mirrors {@code app.jwt.expiration} to embed expiry info in the response. */
    @Value("${app.jwt.expiration}")
    private long jwtExpiration;

    @Value("${app.auth.password-reset.expiry-hours:24}")
    private int passwordResetExpiryHours;

    @Value("${app.frontend.base-url:http://localhost:3000}")
    private String frontendBaseUrl;

    // =========================================================================
    // REGISTRATION
    // =========================================================================

    /**
     * {@inheritDoc}
     *
     * <p>Steps:</p>
     * <ol>
     *   <li>Guard against duplicate email.</li>
     *   <li>BCrypt-encode the plaintext password.</li>
     *   <li>Build and persist the new {@link User} with the {@code USER} role.</li>
     *   <li>Generate a JWT for the new user.</li>
     *   <li>Return the token + public profile.</li>
     * </ol>
     */
    @Override
    @Transactional
    public AuthenticationResponse register(RegisterRequest request) {
        log.info("Processing registration for email: {}", request.getEmail());

        // Guard: prevent duplicate registrations
        if (userRepository.existsByEmail(request.getEmail())) {
            log.warn("Registration attempt with existing email: {}", request.getEmail());
            throw new UserAlreadyExistsException(MessageConstants.USER_ALREADY_EXISTS);
        }

        // Build and persist the user entity
        User user = User.builder()
                .firstName(request.getFirstName().trim())
                .lastName(request.getLastName().trim())
                .email(request.getEmail().toLowerCase().trim())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(Role.USER)
                .active(true)
                .emailVerified(false)
                .build();

        User savedUser = userRepository.save(user);
        log.info("New user registered with id: {}", savedUser.getId());
        
        auditLogRepository.save(AuditLog.builder()
                .action("USER_REGISTERED")
                .resource("User")
                .details("New user registered. Verification required.")
                .user(savedUser)
                .build());

        // Send email verification link
        String rawToken = generateSecureToken();
        EmailVerificationToken verificationToken = EmailVerificationToken.builder()
                .token(hashToken(rawToken))
                .user(savedUser)
                .expiryDate(LocalDateTime.now().plusHours(24))
                .build();
        emailVerificationTokenRepository.save(verificationToken);
        
        String verifyLink = frontendBaseUrl + "/verify-email?token=" + rawToken;
        emailService.sendVerificationEmail(savedUser.getEmail(), verifyLink);

        // Do not log them in immediately if verification is required
        return AuthenticationResponse.builder()
                .accessToken(null)
                .tokenType("Bearer")
                .expiresIn(0L)
                .user(UserResponse.from(savedUser))
                .build();
    }

    // =========================================================================
    // LOGIN
    // =========================================================================

    /**
     * {@inheritDoc}
     *
     * <p>Steps:</p>
     * <ol>
     *   <li>Delegate to {@link AuthenticationManager} for credential verification.</li>
     *   <li>On success, load the full user entity from the DB.</li>
     *   <li>Generate a fresh JWT.</li>
     *   <li>Return the token + public profile.</li>
     * </ol>
     */
    @Override
    @Transactional
    public AuthenticationResponse login(LoginRequest request) {
        log.info("Login attempt for email: {}", request.getEmail());
        
        User preUser = userRepository.findByEmail(request.getEmail().toLowerCase().trim()).orElse(null);
        // if (preUser != null && !preUser.isEmailVerified() && preUser.getAuthProvider() == AuthProvider.LOCAL) {
        //     throw new org.springframework.security.authentication.DisabledException("Please verify your email before logging in.");
        // }

        // Spring Security validates credentials; throws BadCredentialsException or
        // DisabledException automatically — both are handled in GlobalExceptionHandler
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmail().toLowerCase().trim(),
                        request.getPassword()
                )
        );

        // Retrieve the full user from the authenticated principal
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        User user = userDetails.getUser();

        // Refresh lastLogin timestamp
        user.setLastLogin(java.time.LocalDateTime.now());
        userRepository.save(user);

        String token = jwtService.generateToken(userDetails);
        log.info("Login successful for user id: {}", user.getId());

        return buildAuthResponse(token, user);
    }

    // =========================================================================
    // CURRENT USER
    // =========================================================================

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(readOnly = true)
    public UserResponse getCurrentUser() {
        String email = SecurityUtils.requireCurrentUserEmail();

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Authenticated user not found in the database."));

        return UserResponse.from(user);
    }

    @Override
    @Transactional
    public void forgotPassword(String email) {
        log.info("Password reset requested for email: {}", email);
        userRepository.findByEmail(email.toLowerCase().trim()).ifPresent(user -> {

            // Delete any existing tokens for this user
            passwordResetTokenRepository.deleteByUserId(user.getId());

            log.info("Generating password reset token...");
            String rawToken = generateSecureToken();
            String hashedToken = hashToken(rawToken);

            log.info("Saving token...");
            PasswordResetToken resetToken = PasswordResetToken.builder()
                    .token(hashedToken)
                    .user(user)
                    .expiryDate(LocalDateTime.now().plusMinutes(15)) // 15 minutes expiry per requirement
                    .build();
            passwordResetTokenRepository.save(resetToken);

            auditLogRepository.save(AuditLog.builder()
                    .action("PASSWORD_RESET_REQUESTED")
                    .resource("User")
                    .details("Password reset requested for email: " + user.getEmail())
                    .user(user)
                    .build());

            String resetLink = frontendBaseUrl + "/reset-password?token=" + rawToken;
            try {
                emailService.sendPasswordResetEmail(user.getEmail(), resetLink);
            } catch (Exception emailEx) {
                log.error("Password reset email delivery failed for user id: {}. Error: {}",
                    user.getId(), emailEx.getMessage());
                throw new RuntimeException("Failed to send password reset email. Please try again later.", emailEx);
            }
        });
    }

    @Override
    @Transactional
    public void resetPassword(String token, String newPassword) {
        String hashedToken = hashToken(token);
        PasswordResetToken resetToken = passwordResetTokenRepository.findByToken(hashedToken)
                .orElseThrow(() -> new IllegalArgumentException("Invalid or expired password reset token."));

        if (resetToken.isExpired()) {
            passwordResetTokenRepository.delete(resetToken);
            auditLogRepository.save(AuditLog.builder()
                    .action("PASSWORD_RESET_EXPIRED")
                    .resource("User")
                    .details("Expired token used for password reset.")
                    .user(resetToken.getUser())
                    .build());
            throw new IllegalArgumentException("Password reset token has expired.");
        }

        User user = resetToken.getUser();

        // Prevent password reuse
        if (passwordEncoder.matches(newPassword, user.getPassword())) {
            auditLogRepository.save(AuditLog.builder()
                    .action("PASSWORD_RESET_FAILED")
                    .resource("User")
                    .details("Attempted to reuse current password.")
                    .user(user)
                    .build());
            throw new IllegalArgumentException("Your new password cannot be the same as your current password.");
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        // TODO: Enhancement - Immediately invalidate all existing JWT tokens for this user 
        // to prevent previously stolen JWTs from being used after a password reset.
        // This requires implementing a token blocklist/revocation strategy.

        // Delete the token so it can't be reused
        passwordResetTokenRepository.delete(resetToken);
        
        auditLogRepository.save(AuditLog.builder()
                .action("PASSWORD_RESET_COMPLETED")
                .resource("User")
                .details("Password successfully reset.")
                .user(user)
                .build());
                
        log.info("Password successfully reset for user id: {}", user.getId());
    }

    private String generateSecureToken() {
        byte[] randomBytes = new byte[32];
        new SecureRandom().nextBytes(randomBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
    }

    private String hashToken(String token) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(token.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Failed to hash token", e);
        }
    }

    @Override
    @Transactional
    public void verifyEmail(String token) {
        String hashedToken = hashToken(token);
        EmailVerificationToken verificationToken = emailVerificationTokenRepository.findByToken(hashedToken)
                .orElseThrow(() -> new IllegalArgumentException("Invalid or expired verification token."));

        if (verificationToken.isExpired()) {
            emailVerificationTokenRepository.delete(verificationToken);
            throw new IllegalArgumentException("Verification link has expired. Please request a new one.");
        }

        User user = verificationToken.getUser();
        user.setEmailVerified(true);
        userRepository.save(user);

        emailVerificationTokenRepository.delete(verificationToken);
        
        auditLogRepository.save(AuditLog.builder()
                .action("EMAIL_VERIFIED")
                .resource("User")
                .details("Email address verified successfully.")
                .user(user)
                .build());
    }

    @Override
    @Transactional
    public void resendVerificationEmail(String email) {
        User user = userRepository.findByEmail(email.toLowerCase().trim())
                .orElseThrow(() -> new IllegalArgumentException("No account found with that email."));

        if (user.isEmailVerified()) {
            throw new IllegalArgumentException("This email is already verified.");
        }
        if (user.getAuthProvider() != AuthProvider.LOCAL) {
            throw new IllegalArgumentException("OAuth accounts do not require email verification.");
        }

        emailVerificationTokenRepository.deleteByUserId(user.getId());

        String rawToken = generateSecureToken();
        EmailVerificationToken verificationToken = EmailVerificationToken.builder()
                .token(hashToken(rawToken))
                .user(user)
                .expiryDate(LocalDateTime.now().plusHours(24))
                .build();
        emailVerificationTokenRepository.save(verificationToken);
        
        String verifyLink = frontendBaseUrl + "/verify-email?token=" + rawToken;
        emailService.sendVerificationEmail(user.getEmail(), verifyLink);
    }

    // =========================================================================
    // CHANGE PASSWORD
    // =========================================================================

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public void changePassword(String currentPassword, String newPassword) {
        String email = SecurityUtils.requireCurrentUserEmail();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found."));

        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            throw new BadCredentialsException("Current password is incorrect.");
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        log.info("Password changed successfully for user id: {}", user.getId());
    }

    // =========================================================================
    // UPDATE PROFILE
    // =========================================================================

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public UserResponse updateProfile(String firstName, String lastName) {
        String email = SecurityUtils.requireCurrentUserEmail();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found."));

        if (firstName != null && !firstName.isBlank()) {
            user.setFirstName(firstName.trim());
        }
        if (lastName != null && !lastName.isBlank()) {
            user.setLastName(lastName.trim());
        }
        userRepository.save(user);
        log.info("Profile updated for user id: {}", user.getId());
        return UserResponse.from(user);
    }

    // =========================================================================
    // PRIVATE HELPERS
    // =========================================================================

    /**
     * Assembles the {@link AuthenticationResponse} from a token and user entity.
     *
     * @param token the signed JWT string
     * @param user  the authenticated/registered user
     * @return a fully populated {@link AuthenticationResponse}
     */
    private AuthenticationResponse buildAuthResponse(String token, User user) {
        return AuthenticationResponse.builder()
                .accessToken(token)
                .tokenType("Bearer")
                .expiresIn(jwtExpiration)
                .user(UserResponse.from(user))
                .build();
    }

    // =========================================================================
    // FIREBASE / OAUTH LOGIN
    // =========================================================================

    /**
     * {@inheritDoc}
     *
     * <p>Steps:</p>
     * <ol>
     *   <li>Verify the Firebase ID token using the Admin SDK.</li>
     *   <li>Extract email, name, picture, and provider from the verified token.</li>
     *   <li>Look up the user by email in MySQL.</li>
     *   <li>If the user exists, update their last login and return a JWT.</li>
     *   <li>If the user does NOT exist, auto-create them and return a JWT.</li>
     * </ol>
     */
    @Override
    @Transactional
    public AuthenticationResponse firebaseLogin(String firebaseIdToken) {
        // Step 1: Verify the Firebase ID token
        FirebaseTokenVerifier.FirebaseUserInfo firebaseUser =
                firebaseTokenVerifier.verifyToken(firebaseIdToken);

        String email = firebaseUser.email();
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException(
                    "Firebase token does not contain an email. " +
                    "Ensure the OAuth provider is configured to share the email address.");
        }
        email = email.toLowerCase().trim();

        // Step 2: Determine the AuthProvider
        AuthProvider authProvider = resolveProvider(firebaseUser.provider());

        // Step 3: Find or create the local user
        String finalEmail = email;
        User user = userRepository.findByEmail(email).orElseGet(() -> {
            log.info("[Firebase] Auto-creating new user for email: {}, provider: {}",
                    finalEmail, authProvider);

            // Extract name parts from display name
            String displayName = firebaseUser.name() != null ? firebaseUser.name() : "User";
            String[] nameParts = displayName.trim().split("\\s+", 2);
            String firstName = nameParts[0];
            String lastName = nameParts.length > 1 ? nameParts[1] : "User";

            User newUser = User.builder()
                    .firstName(firstName)
                    .lastName(lastName)
                    .email(finalEmail)
                    .password(passwordEncoder.encode(java.util.UUID.randomUUID().toString())) // Dummy password for DB constraint
                    .role(Role.USER)
                    .authProvider(authProvider)
                    .providerId(firebaseUser.uid())
                    .profileImage(firebaseUser.picture())
                    .active(true)
                    .emailVerified(true)  // OAuth emails are pre-verified
                    .build();

            return userRepository.save(newUser);
        });

        // Step 4: Update last login and provider info if needed
        user.setLastLogin(java.time.LocalDateTime.now());
        if (user.getProviderId() == null) {
            user.setProviderId(firebaseUser.uid());
            user.setAuthProvider(authProvider);
        }
        if (user.getProfileImage() == null && firebaseUser.picture() != null) {
            user.setProfileImage(firebaseUser.picture());
        }
        userRepository.save(user);

        // Step 5: Generate our own Spring Boot JWT
        CustomUserDetails userDetails = new CustomUserDetails(user);
        String token = jwtService.generateToken(userDetails);

        log.info("[Firebase] OAuth login successful for user id: {}, provider: {}",
                user.getId(), authProvider);

        return buildAuthResponse(token, user);
    }

    /**
     * Maps the Firebase sign_in_provider string to our {@link AuthProvider} enum.
     */
    private AuthProvider resolveProvider(String firebaseProvider) {
        if (firebaseProvider == null) return AuthProvider.GOOGLE;
        return switch (firebaseProvider) {
            case "google.com"  -> AuthProvider.GOOGLE;
            case "github.com"  -> AuthProvider.GITHUB;
            case "password"    -> AuthProvider.LOCAL;
            default            -> AuthProvider.GOOGLE;
        };
    }
}
