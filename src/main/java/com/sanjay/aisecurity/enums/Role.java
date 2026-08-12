package com.sanjay.aisecurity.enums;

/**
 * User role enumeration for role-based access control.
 *
 * <p>Roles are stored as string values in the database via
 * {@code @Enumerated(EnumType.STRING)} on the {@code User} entity.
 * Spring Security reads these roles and maps them to authorities prefixed
 * with {@code ROLE_} (e.g. {@code ROLE_USER}, {@code ROLE_ADMIN}).</p>
 *
 * <ul>
 *   <li>{@code USER} — Standard platform user with access to their own
 *       projects, scans, reports, and chat.</li>
 *   <li>{@code ADMIN} — Administrator with access to all user data,
 *       global statistics, system health, and user management.</li>
 * </ul>
 *
 * @author Sanjay
 * @version 1.0.0
 */
public enum Role {

    /** Standard user role — default for all registered users. */
    USER,

    /** Administrator role — elevated privileges for platform management. */
    ADMIN
}
