package com.sanjay.aisecurity.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * Filter for centralized request logging.
 * Logs method, URI, status, processing time, and client IP without exposing sensitive data.
 * Assigns a unique Request ID for traceability.
 */
@Slf4j
@Component
public class RequestLoggingFilter extends OncePerRequestFilter {

    private static final String REQUEST_ID = "requestId";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        // Generate or retrieve Correlation ID
        String correlationId = request.getHeader("X-Request-ID");
        if (correlationId == null || correlationId.isEmpty()) {
            correlationId = UUID.randomUUID().toString();
        }

        // Add to MDC for structured logging across the thread
        MDC.put(REQUEST_ID, correlationId);
        response.setHeader("X-Request-ID", correlationId);

        long startTime = System.currentTimeMillis();
        String method = request.getMethod();
        String uri = request.getRequestURI();
        String clientIp = getClientIp(request);

        try {
            // Proceed with the request
            filterChain.doFilter(request, response);
        } finally {
            long duration = System.currentTimeMillis() - startTime;
            int status = response.getStatus();

            if (status >= 500) {
                log.error("HTTP {} {} - {} - {}ms - IP: {}", method, uri, status, duration, clientIp);
            } else if (status >= 400) {
                log.warn("HTTP {} {} - {} - {}ms - IP: {}", method, uri, status, duration, clientIp);
            } else {
                log.info("HTTP {} {} - {} - {}ms", method, uri, status, duration); // Kept concise for success
            }
            
            // Clean up MDC
            MDC.remove(REQUEST_ID);
        }
    }

    private String getClientIp(HttpServletRequest request) {
        String xfHeader = request.getHeader("X-Forwarded-For");
        if (xfHeader == null || xfHeader.isEmpty()) {
            return request.getRemoteAddr();
        }
        return xfHeader.split(",")[0].trim();
    }
}
