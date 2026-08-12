package com.sanjay.aisecurity.constants;

/**
 * Application-wide API path constants.
 *
 * <p>Centralizes all URL path prefixes to ensure consistency across
 * controllers and Spring Security configuration. Changing a root path
 * here automatically propagates to all referencing classes.</p>
 *
 * @author Sanjay
 * @version 1.0.0
 */
public final class ApiConstants {

    // =========================================================================
    // ROOT PATHS
    // =========================================================================

    /** Root prefix for all versioned API endpoints. */
    public static final String API_V1 = "/api/v1";

    // =========================================================================
    // MODULE PATHS
    // =========================================================================

    public static final String AUTH_BASE      = API_V1 + "/auth";
    public static final String PROJECT_BASE   = API_V1 + "/projects";
    public static final String UPLOAD_BASE    = API_V1 + "/uploads";
    public static final String SCAN_BASE      = API_V1 + "/scans";
    public static final String AI_BASE        = API_V1 + "/ai";
    public static final String REPORT_BASE    = API_V1 + "/reports";
    public static final String CHAT_BASE      = API_V1 + "/chat";
    public static final String DASHBOARD_BASE = API_V1 + "/dashboard";
    public static final String ADMIN_BASE     = API_V1 + "/admin";
    public static final String NOTIFICATION_BASE = API_V1 + "/notifications";
    public static final String AUDIT_BASE     = API_V1 + "/audit";

    // =========================================================================
    // WILDCARD MATCHERS (used in SecurityConfig)
    // =========================================================================

    public static final String AUTH_WILDCARD         = AUTH_BASE + "/**";
    public static final String SWAGGER_UI_WILDCARD   = "/swagger-ui/**";
    public static final String API_DOCS_WILDCARD     = "/api-docs/**";
    public static final String ADMIN_WILDCARD        = ADMIN_BASE + "/**";

    // =========================================================================
    // AUTH SUB-PATHS
    // =========================================================================

    public static final String AUTH_REGISTER = AUTH_BASE + "/register";
    public static final String AUTH_LOGIN    = AUTH_BASE + "/login";

    // =========================================================================
    // PAGINATION DEFAULTS
    // =========================================================================

    public static final int    DEFAULT_PAGE      = 0;
    public static final int    DEFAULT_PAGE_SIZE = 10;
    public static final int    MAX_PAGE_SIZE     = 100;
    public static final String DEFAULT_SORT_BY   = "createdAt";
    public static final String DEFAULT_SORT_DIR  = "desc";

    // =========================================================================
    // HTTP HEADER NAMES
    // =========================================================================

    public static final String AUTHORIZATION_HEADER = "Authorization";
    public static final String BEARER_PREFIX        = "Bearer ";
    public static final String X_FORWARDED_FOR      = "X-Forwarded-For";

    // Private constructor prevents instantiation of this utility class.
    private ApiConstants() {
        throw new UnsupportedOperationException("ApiConstants is a constants class and cannot be instantiated.");
    }
}
