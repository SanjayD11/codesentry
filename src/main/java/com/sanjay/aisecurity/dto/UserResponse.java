package com.sanjay.aisecurity.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.sanjay.aisecurity.entity.User;
import com.sanjay.aisecurity.enums.Role;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * User Response DTO.
 *
 * <p>Represents the public-facing view of a user for admin use. Deliberately omits the
 * password hash, uploaded files, scan reports, vulnerabilities, and AI summaries
 * to protect user data privacy per GDPR and platform security policy.</p>
 *
 * <p>Use the static {@link #from(User, long)} factory method to build an instance
 * from the domain {@link User} entity with project count.</p>
 *
 * @author Sanjay
 * @version 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UserResponse {

    private Long id;
    private String firstName;
    private String lastName;
    private String email;
    private Role role;
    private boolean active;
    private boolean emailVerified;
    private String profileImage;
    private LocalDateTime lastLogin;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /** Number of projects owned by this user — shown as a count only, never the project contents. */
    private long projectCount;

    /**
     * Maps a {@link User} domain entity to a {@link UserResponse} DTO.
     * Includes project count for admin display.
     *
     * @param user         the domain entity to map from
     * @param projectCount number of projects belonging to this user
     * @return a populated {@link UserResponse}
     */
    public static UserResponse from(User user, long projectCount) {
        return UserResponse.builder()
                .id(user.getId())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .email(user.getEmail())
                .role(user.getRole())
                .active(user.isActive())
                .emailVerified(user.isEmailVerified())
                .profileImage(user.getProfileImage())
                .lastLogin(user.getLastLogin())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .projectCount(projectCount)
                .build();
    }

    /**
     * Maps a {@link User} without project count (for non-admin contexts).
     *
     * @param user the domain entity to map from
     * @return a populated {@link UserResponse}
     */
    public static UserResponse from(User user) {
        return from(user, 0L);
    }
}
