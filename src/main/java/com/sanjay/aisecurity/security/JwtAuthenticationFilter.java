package com.sanjay.aisecurity.security;

import com.sanjay.aisecurity.constants.ApiConstants;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * JWT Authentication Filter.
 *
 * <p>Executes exactly once per request (extends {@link OncePerRequestFilter}).
 * Intercepts every incoming HTTP request to extract and validate a Bearer JWT
 * from the {@code Authorization} header. On success, it populates the
 * {@link SecurityContextHolder} so that downstream Spring Security filters
 * and controllers can read the authenticated principal.</p>
 *
 * <p>Processing pipeline:</p>
 * <ol>
 *   <li>Read {@code Authorization: Bearer &lt;token&gt;} header.</li>
 *   <li>Extract the JWT string.</li>
 *   <li>Extract the username (email) claim.</li>
 *   <li>Load the full {@link UserDetails} from the database.</li>
 *   <li>Validate the token against the loaded principal.</li>
 *   <li>Set the authentication in the {@link SecurityContextHolder}.</li>
 *   <li>Continue the filter chain.</li>
 * </ol>
 *
 * @author Sanjay
 * @version 1.0.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final CustomUserDetailsService userDetailsService;

    /**
     * Core filter logic: validates the JWT and authenticates the request.
     *
     * @param request     the incoming HTTP request
     * @param response    the HTTP response
     * @param filterChain the remaining filter chain
     * @throws ServletException if a servlet error occurs
     * @throws IOException      if an I/O error occurs
     */
    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain)
            throws ServletException, IOException {

        final String authHeader = request.getHeader(ApiConstants.AUTHORIZATION_HEADER);

        // Skip filter if there is no Authorization header or it is not a Bearer token
        if (!StringUtils.hasText(authHeader) || !authHeader.startsWith(ApiConstants.BEARER_PREFIX)) {
            filterChain.doFilter(request, response);
            return;
        }

        // Extract the raw JWT (strip "Bearer " prefix)
        final String jwt = authHeader.substring(ApiConstants.BEARER_PREFIX.length());

        try {
            final String userEmail = jwtService.extractUsername(jwt);

            // Only authenticate if not already authenticated in this request context
            if (StringUtils.hasText(userEmail)
                    && SecurityContextHolder.getContext().getAuthentication() == null) {

                UserDetails userDetails = userDetailsService.loadUserByUsername(userEmail);

                if (jwtService.isTokenValid(jwt, userDetails)) {
                    UsernamePasswordAuthenticationToken authToken =
                            new UsernamePasswordAuthenticationToken(
                                    userDetails,
                                    null,
                                    userDetails.getAuthorities()
                            );
                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                    log.debug("JWT authenticated user [{}] for URI [{}]", userEmail, request.getRequestURI());
                }
            }

        } catch (ExpiredJwtException e) {
            log.warn("Expired JWT token for request [{}]: {}", request.getRequestURI(), e.getMessage());
            // Do NOT set authentication — Spring Security will trigger the entry point
        } catch (JwtException e) {
            log.warn("Invalid JWT token for request [{}]: {}", request.getRequestURI(), e.getMessage());
        }

        filterChain.doFilter(request, response);
    }
}
