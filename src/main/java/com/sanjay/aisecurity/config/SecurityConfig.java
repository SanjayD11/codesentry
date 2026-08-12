package com.sanjay.aisecurity.config;

import com.sanjay.aisecurity.constants.ApiConstants;
import com.sanjay.aisecurity.security.JwtAuthenticationEntryPoint;
import com.sanjay.aisecurity.security.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * Spring Security Configuration.
 *
 * <p>Defines the complete security filter chain for the AI Security Analysis Platform.
 * Implements a fully stateless JWT-based authentication model.</p>
 *
 * <p>Key decisions:</p>
 * <ul>
 *   <li>Sessions are never created ({@code STATELESS}).</li>
 *   <li>CSRF is disabled — appropriate for stateless REST APIs.</li>
 *   <li>CORS is configured to allow the configured origins.</li>
 *   <li>Public endpoints are explicitly whitelisted; all others require authentication.</li>
 *   <li>{@code /api/v1/admin/**} is restricted to the {@code ADMIN} role.</li>
 *   <li>The JWT filter runs before Spring Security's default username/password filter.</li>
 * </ul>
 *
 * @author Sanjay
 * @version 1.0.0
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;
    private final UserDetailsService userDetailsService;

    @org.springframework.beans.factory.annotation.Value("${cors.allowed-origins:http://localhost:3000}")
    private List<String> allowedOrigins;

    // =========================================================================
    // SECURITY FILTER CHAIN
    // =========================================================================

    /**
     * Configures the main {@link SecurityFilterChain}.
     *
     * @param http the {@link HttpSecurity} builder
     * @return the fully configured {@link SecurityFilterChain}
     * @throws Exception if configuration fails
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // ── CORS ─────────────────────────────────────────────────────────
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))

            // ── CSRF disabled for stateless REST ─────────────────────────────
            .csrf(AbstractHttpConfigurer::disable)

            // ── Exception handling: return JSON on 401 ────────────────────────
            .exceptionHandling(exceptions -> exceptions
                .authenticationEntryPoint(jwtAuthenticationEntryPoint)
            )

            // ── Session management: STATELESS ─────────────────────────────────
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )

            // ── Authorization rules ───────────────────────────────────────────
            .authorizeHttpRequests(auth -> auth
                // Public authentication endpoints
                .requestMatchers(ApiConstants.AUTH_WILDCARD, ApiConstants.API_V1 + "/public/**").permitAll()

                // Swagger / OpenAPI UI (development convenience)
                .requestMatchers(
                    ApiConstants.SWAGGER_UI_WILDCARD,
                    ApiConstants.API_DOCS_WILDCARD,
                    "/swagger-ui.html",
                    "/swagger-resources/**",
                    "/webjars/**"
                ).permitAll()

                // H2 console (if needed for dev)
                .requestMatchers("/h2-console/**", "/api/v1/test/**", "/api/v1/test2/**").permitAll()

                // Actuator health (if added later)
                .requestMatchers(HttpMethod.GET, "/actuator/health").permitAll()
                
                // System Mail endpoints
                .requestMatchers(HttpMethod.GET, "/api/system/mail/health").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/system/mail/test").permitAll()

                // Admin-only endpoints
                .requestMatchers(ApiConstants.ADMIN_WILDCARD).hasRole(com.sanjay.aisecurity.enums.Role.ADMIN.name())

                // Everything else requires authentication
                .anyRequest().authenticated()
            )

            // ── Authentication provider ───────────────────────────────────────
            .authenticationProvider(authenticationProvider())

            // ── JWT filter before Spring's default auth filter ────────────────
            .addFilterBefore(jwtAuthenticationFilter,
                             UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    // =========================================================================
    // CORS CONFIGURATION
    // =========================================================================

    /**
     * Configures CORS to allow requests from all origins during development.
     *
     * <p><strong>Production note:</strong> Replace {@code *} with the specific
     * frontend origin(s) before deploying (e.g. {@code https://yourdomain.com}).</p>
     *
     * @return the {@link CorsConfigurationSource}
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(allowedOrigins);
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("Authorization", "Content-Type", "X-Requested-With", "Accept", "Origin", "Access-Control-Request-Method", "Access-Control-Request-Headers"));
        configuration.setExposedHeaders(List.of("Access-Control-Allow-Origin", "Access-Control-Allow-Credentials"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    // =========================================================================
    // AUTHENTICATION PROVIDER & BEANS
    // =========================================================================

    /**
     * Configures a {@link DaoAuthenticationProvider} that uses the custom
     * {@link UserDetailsService} and BCrypt encoder for credential verification.
     *
     * @return the fully wired {@link AuthenticationProvider}
     */
    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    /**
     * Exposes the {@link AuthenticationManager} as a Spring bean so it can
     * be injected into {@link com.sanjay.aisecurity.service.AuthServiceImpl}.
     *
     * @param config the auto-configured {@link AuthenticationConfiguration}
     * @return the application {@link AuthenticationManager}
     * @throws Exception if the manager cannot be resolved
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config)
            throws Exception {
        return config.getAuthenticationManager();
    }

    /**
     * Declares the {@link BCryptPasswordEncoder} bean used for password hashing.
     * Strength factor of 12 provides a good balance of security and performance.
     *
     * @return a {@link BCryptPasswordEncoder} with strength 12
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }
}
