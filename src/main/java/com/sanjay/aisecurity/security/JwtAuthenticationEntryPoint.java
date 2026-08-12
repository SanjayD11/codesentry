package com.sanjay.aisecurity.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sanjay.aisecurity.constants.MessageConstants;
import com.sanjay.aisecurity.exception.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.LocalDateTime;

/**
 * JWT Authentication Entry Point.
 *
 * <p>Invoked by Spring Security whenever an unauthenticated request attempts
 * to access a protected resource. Instead of redirecting to a login page
 * (which makes no sense for a stateless REST API), this implementation
 * writes a standardized JSON {@link ErrorResponse} with HTTP 401 directly
 * to the response output stream.</p>
 *
 * @author Sanjay
 * @version 1.0.0
 */
@Slf4j
@Component
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper = new ObjectMapper()
            .findAndRegisterModules(); // registers JavaTimeModule for LocalDateTime

    /**
     * Called when authentication fails.
     *
     * <p>Writes a 401 Unauthorized JSON error response and logs the attempt
     * without leaking any sensitive internal details to the client.</p>
     *
     * @param request       the HTTP request that triggered the failure
     * @param response      the HTTP response to write the error payload into
     * @param authException the authentication exception describing the failure
     * @throws IOException if writing to the response stream fails
     */
    @Override
    public void commence(HttpServletRequest request,
                         HttpServletResponse response,
                         AuthenticationException authException) throws IOException {

        log.warn("Unauthorized access attempt to [{}]: {}",
                request.getRequestURI(), authException.getMessage());

        ErrorResponse errorResponse = ErrorResponse.builder()
                .success(false)
                .status(HttpStatus.UNAUTHORIZED.value())
                .message(MessageConstants.UNAUTHORIZED)
                .timestamp(LocalDateTime.now())
                .build();

        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");

        objectMapper.writeValue(response.getOutputStream(), errorResponse);
    }
}
