package com.sanjay.aisecurity.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * Asynchronous Task Executor Configuration.
 *
 * <p>Defines the default Spring async thread pool ({@code taskExecutor}) used
 * by {@code @Async} methods that do not specify a qualifier.</p>
 *
 * <p>The dedicated scan/AI enrichment executor ({@code scanTaskExecutor}) is
 * defined in {@link ScanExecutorConfig} and is referenced by
 * {@code @Async("scanTaskExecutor")} in {@code AiEnrichmentService}.</p>
 *
 * @author Sanjay
 * @version 1.1.0
 */
@Configuration
public class AsyncConfig implements AsyncConfigurer {

    /**
     * Default async executor — used by {@code @Async} without a qualifier.
     *
     * <p>Configuration:
     * <ul>
     *   <li>Core pool: 4 threads (always alive)</li>
     *   <li>Max pool: 20 threads (burst capacity)</li>
     *   <li>Queue capacity: 100 tasks</li>
     *   <li>Thread name prefix: {@code AiSecurity-Async-}</li>
     *   <li>Graceful shutdown: 60 s await</li>
     * </ul>
     *
     * @return configured {@link Executor} instance
     */
    @Bean(name = "taskExecutor")
    @Override
    public Executor getAsyncExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(4);
        executor.setMaxPoolSize(20);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("AiSecurity-Async-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(60);
        executor.initialize();
        return executor;
    }
}
