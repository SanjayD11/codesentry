package com.sanjay.aisecurity.constants;

/**
 * Application-wide user-facing message constants.
 *
 * <p>Centralizes all API response messages to ensure consistency
 * across the application. Avoids hardcoded strings scattered in
 * service and controller layers.</p>
 *
 * @author Sanjay
 * @version 1.0.0
 */
public final class MessageConstants {

    // =========================================================================
    // AUTHENTICATION MESSAGES
    // =========================================================================

    public static final String USER_REGISTERED_SUCCESS  = "User registered successfully.";
    public static final String LOGIN_SUCCESS             = "Login successful.";
    public static final String LOGOUT_SUCCESS            = "Logged out successfully.";
    public static final String USER_ALREADY_EXISTS      = "A user with this email address already exists.";
    public static final String INVALID_CREDENTIALS      = "Invalid email or password.";
    public static final String ACCOUNT_DISABLED         = "Your account has been deactivated. Please contact support.";
    public static final String PASSWORD_MISMATCH        = "Password and confirm password do not match.";
    public static final String PASSWORD_CHANGED_SUCCESS = "Password changed successfully.";

    // =========================================================================
    // JWT MESSAGES
    // =========================================================================

    public static final String TOKEN_EXPIRED   = "JWT token has expired. Please log in again.";
    public static final String TOKEN_INVALID   = "Invalid JWT token.";
    public static final String TOKEN_MISSING   = "Authorization token is missing.";
    public static final String UNAUTHORIZED    = "You are not authorized to access this resource.";
    public static final String ACCESS_DENIED   = "Access denied. You do not have permission to perform this action.";

    // =========================================================================
    // PROJECT MESSAGES
    // =========================================================================

    public static final String PROJECT_CREATED        = "Project created successfully.";
    public static final String PROJECT_UPDATED        = "Project updated successfully.";
    public static final String PROJECT_DELETED        = "Project deleted successfully.";
    public static final String PROJECT_NOT_FOUND      = "Project not found or you do not have access to it.";
    public static final String PROJECT_FETCH_SUCCESS  = "Projects retrieved successfully.";

    // =========================================================================
    // FILE UPLOAD MESSAGES
    // =========================================================================

    public static final String FILE_UPLOAD_SUCCESS    = "File uploaded successfully.";
    public static final String FILES_UPLOAD_SUCCESS   = "Files uploaded successfully.";
    public static final String ZIP_UPLOAD_SUCCESS     = "Project ZIP extracted and uploaded successfully.";
    public static final String FILE_NOT_FOUND         = "File not found.";
    public static final String FILE_DELETED           = "File deleted successfully.";
    public static final String FILE_INVALID           = "Invalid file. Only allowed file types are accepted.";
    public static final String FILE_TOO_LARGE         = "File size exceeds the maximum allowed limit.";
    public static final String FILE_EMPTY             = "Uploaded file is empty.";
    public static final String ZIP_INVALID            = "Invalid or corrupted ZIP archive.";
    public static final String ZIP_SLIP_DETECTED      = "Invalid ZIP entry path detected. Upload rejected for security reasons.";
    public static final String FILE_DUPLICATE         = "A file with this name already exists in this project.";

    // =========================================================================
    // SCAN MESSAGES
    // =========================================================================

    public static final String SCAN_STARTED           = "Security scan initiated successfully.";
    public static final String SCAN_NOT_FOUND         = "Scan not found or you do not have access to it.";
    public static final String SCAN_FETCH_SUCCESS     = "Scan results retrieved successfully.";
    public static final String SCAN_IN_PROGRESS       = "A scan is already in progress for this project.";

    // =========================================================================
    // AI MESSAGES
    // =========================================================================

    public static final String AI_ANALYSIS_SUCCESS    = "AI analysis completed successfully.";
    public static final String AI_ANALYSIS_FAILED     = "AI analysis failed. Please try again later.";
    public static final String AI_RATE_LIMIT          = "AI provider rate limit exceeded. Please retry in a few moments.";

    // =========================================================================
    // REPORT MESSAGES
    // =========================================================================

    public static final String REPORT_GENERATED       = "PDF report generated successfully.";
    public static final String REPORT_NOT_FOUND       = "Report not found or you do not have access to it.";
    public static final String REPORT_DELETED         = "Report deleted successfully.";

    // =========================================================================
    // CHAT MESSAGES
    // =========================================================================

    public static final String CHAT_RESPONSE_SUCCESS  = "Response generated successfully.";
    public static final String CONVERSATION_NOT_FOUND = "Conversation not found.";
    public static final String CONVERSATION_DELETED   = "Conversation deleted successfully.";

    // =========================================================================
    // GENERIC MESSAGES
    // =========================================================================

    public static final String SUCCESS                = "Operation completed successfully.";
    public static final String INTERNAL_SERVER_ERROR  = "An unexpected error occurred. Please try again later.";
    public static final String VALIDATION_FAILED      = "Validation failed. Please check your input.";
    public static final String RESOURCE_NOT_FOUND     = "The requested resource was not found.";

    // Private constructor — prevents instantiation.
    private MessageConstants() {
        throw new UnsupportedOperationException("MessageConstants is a constants class and cannot be instantiated.");
    }
}
