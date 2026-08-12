package com.sanjay.aisecurity.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * JWT Service.
 *
 * <p>Centralizes all JSON Web Token operations: generation, parsing, validation,
 * and claim extraction. Reads the secret key and expiration window from
 * {@code application.yml} so they can be rotated without code changes.</p>
 *
 * <p>Tokens are signed with HMAC-SHA256 using a key derived from the configured
 * base64-encoded secret.</p>
 *
 * @author Sanjay
 * @version 1.0.0
 */
@Slf4j
@Service
public class JwtService {

    /** Base64-encoded HMAC-SHA256 signing secret (min 256 bits). */
    @Value("${app.jwt.secret}")
    private String jwtSecret;

    /** Token validity window in milliseconds (default 24 h = 86 400 000 ms). */
    @Value("${app.jwt.expiration}")
    private long jwtExpiration;

    // =========================================================================
    // TOKEN GENERATION
    // =========================================================================

    /**
     * Generates a signed JWT for the given {@link UserDetails}.
     *
     * @param userDetails the authenticated principal
     * @return a compact, URL-safe JWT string
     */
    public String generateToken(UserDetails userDetails) {
        return generateToken(new HashMap<>(), userDetails);
    }

    /**
     * Generates a JWT with additional custom claims embedded in the payload.
     *
     * @param extraClaims additional key-value pairs to embed in the JWT
     * @param userDetails the authenticated principal
     * @return a compact, URL-safe JWT string
     */
    public String generateToken(Map<String, Object> extraClaims, UserDetails userDetails) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + jwtExpiration);

        return Jwts.builder()
                .setClaims(extraClaims)
                .setSubject(userDetails.getUsername())
                .setIssuedAt(now)
                .setExpiration(expiry)
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    // =========================================================================
    // VALIDATION
    // =========================================================================

    /**
     * Validates a JWT against the provided {@link UserDetails}.
     *
     * <p>A token is valid if:<br>
     * 1. The subject matches the username in {@code userDetails}.<br>
     * 2. The token has not expired.</p>
     *
     * @param token       the JWT to validate
     * @param userDetails the principal to validate against
     * @return {@code true} if the token is valid; {@code false} otherwise
     */
    public boolean isTokenValid(String token, UserDetails userDetails) {
        try {
            final String username = extractUsername(token);
            return username.equals(userDetails.getUsername()) && !isTokenExpired(token);
        } catch (JwtException | IllegalArgumentException e) {
            log.warn("JWT validation failed: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Checks whether the given token has passed its expiration date.
     *
     * @param token the JWT to inspect
     * @return {@code true} if expired
     */
    public boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    // =========================================================================
    // CLAIM EXTRACTION
    // =========================================================================

    /**
     * Extracts the subject (username / email) from the token.
     *
     * @param token the JWT
     * @return the subject claim value
     */
    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    /**
     * Extracts the expiration date from the token.
     *
     * @param token the JWT
     * @return the expiration {@link Date}
     */
    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    /**
     * Extracts a single claim from the token using the provided resolver function.
     *
     * @param token          the JWT
     * @param claimsResolver a function to apply to the parsed {@link Claims}
     * @param <T>            the type of the resolved claim
     * @return the resolved claim value
     */
    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    // =========================================================================
    // INTERNAL
    // =========================================================================

    /**
     * Parses and returns all claims from the JWT.
     * Throws a specific {@link JwtException} subtype on any parsing failure.
     *
     * @param token the JWT to parse
     * @return the {@link Claims} payload
     * @throws ExpiredJwtException    if the token has expired
     * @throws MalformedJwtException  if the token format is invalid
     * @throws SignatureException     if the signature does not match
     * @throws UnsupportedJwtException if the token type is not supported
     */
    private Claims extractAllClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    /**
     * Derives the HMAC-SHA256 {@link Key} from the configured secret string.
     *
     * @return the signing key
     */
    private Key getSigningKey() {
        byte[] keyBytes = jwtSecret.getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}
