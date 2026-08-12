package com.sanjay.aisecurity.util;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Optional;

/**
 * Security Context Utility.
 *
 * <p>Provides helper methods to extract the currently authenticated user's
 * information from the Spring Security context. All service methods that
 * need the caller's identity should use this utility rather than relying
 * on request parameters (which could be tampered).</p>
 *
 * @author Sanjay
 * @version 1.0.0
 */
public final class SecurityUtils {

    // Private constructor — utility class.
    private SecurityUtils() {
        throw new UnsupportedOperationException("SecurityUtils cannot be instantiated.");
    }

    /**
     * Returns the email (username) of the currently authenticated user.
     *
     * @return an {@link Optional} containing the authenticated user's email,
     *         or empty if no authentication context is present
     */
    public static Optional<String> getCurrentUserEmail() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return Optional.empty();
        }
        Object principal = authentication.getPrincipal();
        if (principal instanceof UserDetails userDetails) {
            return Optional.of(userDetails.getUsername());
        }
        if (principal instanceof String email) {
            return Optional.of(email);
        }
        return Optional.empty();
    }

    /**
     * Returns the email of the currently authenticated user.
     * Throws an {@link IllegalStateException} if no authentication context exists.
     *
     * <p>Use this method in service methods that are guaranteed to run
     * only within a secured request context.</p>
     *
     * @return the authenticated user's email
     * @throws IllegalStateException if the security context holds no authenticated user
     */
    public static String requireCurrentUserEmail() {
        return getCurrentUserEmail()
                .orElseThrow(() -> new IllegalStateException(
                        "No authenticated user found in the security context."));
    }

    /**
     * Extracts the client IP address from the HTTP request.
     *
     * <p>Checks the {@code X-Forwarded-For} header first (for proxy/load-balancer
     * deployments), then falls back to {@code request.getRemoteAddr()}.</p>
     *
     * @param request the incoming HTTP servlet request
     * @return the client IP address string
     */
    public static String extractClientIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            // X-Forwarded-For may contain a comma-separated list; the first is the client
            return forwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    /**
     * Checks whether the current security context holds an authenticated principal.
     *
     * @return {@code true} if a valid authenticated user is present
     */
    public static boolean isAuthenticated() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null && auth.isAuthenticated()
                && !(auth.getPrincipal() instanceof String anonymousUser
                        && anonymousUser.equals("anonymousUser"));
    }
}
