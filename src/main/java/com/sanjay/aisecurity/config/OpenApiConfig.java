package com.sanjay.aisecurity.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI (Swagger) Documentation Configuration.
 *
 * <p>Configures the OpenAPI 3.0 specification exposed at {@code /api-docs}
 * and rendered via Swagger UI at {@code /swagger-ui.html}.</p>
 *
 * <p>A global Bearer JWT security scheme is registered so that every secured
 * endpoint automatically shows the authorization input in the Swagger UI
 * without requiring per-endpoint annotation.</p>
 *
 * @author Sanjay
 * @version 1.0.0
 */
@Configuration
public class OpenApiConfig {

    private static final String SECURITY_SCHEME_NAME = "BearerAuth";

    /**
     * Builds the OpenAPI metadata and global security scheme.
     *
     * @return configured {@link OpenAPI} instance
     */
    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(buildApiInfo())
                .addSecurityItem(new SecurityRequirement().addList(SECURITY_SCHEME_NAME))
                .components(new Components()
                        .addSecuritySchemes(SECURITY_SCHEME_NAME, buildSecurityScheme()));
    }

    /**
     * Builds the API metadata (title, version, description, contact, license).
     *
     * @return populated {@link Info} object
     */
    private Info buildApiInfo() {
        return new Info()
                .title("AI Security Analysis Platform API")
                .version("1.0.0")
                .description("""
                        Enterprise-grade AI-powered source code security analysis platform.
                        
                        Features:
                        - JWT-based authentication and role-based authorization
                        - Multi-language static code analysis
                        - AI-powered vulnerability explanations via Google Gemini
                        - Professional PDF security report generation
                        - AI cybersecurity chatbot
                        - Real-time scan progress tracking
                        """)
                .contact(new Contact()
                        .name("Sanjay")
                        .email("sanjay@aisecurity.com"))
                .license(new License()
                        .name("MIT License")
                        .url("https://opensource.org/licenses/MIT"));
    }

    /**
     * Defines the Bearer JWT security scheme applied globally to the API.
     *
     * @return configured {@link SecurityScheme} for JWT Bearer tokens
     */
    private SecurityScheme buildSecurityScheme() {
        return new SecurityScheme()
                .name(SECURITY_SCHEME_NAME)
                .type(SecurityScheme.Type.HTTP)
                .scheme("bearer")
                .bearerFormat("JWT")
                .description("Provide a valid JWT access token. Obtain one via POST /api/v1/auth/login");
    }
}
