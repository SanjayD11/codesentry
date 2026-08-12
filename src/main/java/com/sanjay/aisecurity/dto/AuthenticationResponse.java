package com.sanjay.aisecurity.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Authentication Response DTO.
 *
 * <p>Returned after a successful registration or login.
 * Contains the JWT access token and the authenticated user's public profile.</p>
 *
 * <p>{@code null} fields are excluded from the JSON output via
 * {@link JsonInclude.Include#NON_NULL} to keep the payload lean.</p>
 *
 * @author Sanjay
 * @version 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AuthenticationResponse {

    /** The signed JWT access token to be stored client-side. */
    private String accessToken;

    /** Always {@code "Bearer"} — informs the client of the token scheme. */
    @Builder.Default
    private String tokenType = "Bearer";

    /** Expiry duration in milliseconds (mirrors {@code app.jwt.expiration}). */
    private long expiresIn;

    /** Public profile of the authenticated user. */
    private UserResponse user;

    /** Timestamp of token issuance. */
    @Builder.Default
    private LocalDateTime issuedAt = LocalDateTime.now();
}
