package com.sanjay.aisecurity;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * AI Security Analysis Platform - Main Application Entry Point.
 *
 * <p>This class bootstraps the Spring Boot application context. The following
 * annotations are activated at startup:</p>
 * <ul>
 *   <li>{@code @SpringBootApplication} — enables auto-configuration, component
 *       scanning, and configuration property binding for the
 *       {@code com.sanjay.aisecurity} base package.</li>
 *   <li>{@code @EnableAsync} — activates Spring's asynchronous method execution
 *       capability. Background tasks such as file processing, security scanning,
 *       AI analysis, and email delivery run on a dedicated thread pool defined
 *       in {@code AsyncConfig}.</li>
 *   <li>{@code @EnableScheduling} — activates Spring's cron/fixed-rate task
 *       scheduler. Maintenance jobs (cleanup, statistics generation) defined
 *       in the {@code scheduler} package are triggered automatically.</li>
 * </ul>
 *
 * <p>Active profile is driven by the {@code spring.profiles.active} property
 * (default: {@code dev}).</p>
 *
 * @author Sanjay
 * @version 1.0.0
 */
@SpringBootApplication
@EnableAsync
@EnableScheduling
public class AiSecurityApplication {

    /**
     * Application entry point.
     *
     * @param args command-line arguments passed to the Spring context
     */
    public static void main(String[] args) {
        SpringApplication.run(AiSecurityApplication.class, args);
    }
}
