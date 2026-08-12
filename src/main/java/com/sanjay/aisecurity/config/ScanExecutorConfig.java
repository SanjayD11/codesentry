package com.sanjay.aisecurity.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * Configuration for the parallel scan execution thread pool.
 *
 * <p>Used by {@code ScanServiceImpl} to execute file scans concurrently,
 * and by {@code AiEnrichmentService} via {@code @Async("scanTaskExecutor")}.</p>
 */
@Configuration
public class ScanExecutorConfig {

    @Bean(name = "scanTaskExecutor")
    public Executor scanTaskExecutor() {
        int cores = Runtime.getRuntime().availableProcessors();

        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(cores);
        executor.setMaxPoolSize(cores + 2); // Avoid creating thousands of threads, block if queue is full
        executor.setQueueCapacity(5000); // 5000 files can wait before rejection
        executor.setThreadNamePrefix("scan-worker-");

        // If the queue fills up (e.g., massive 10k file project),
        // the caller (main scan thread) will execute the task itself rather than aborting.
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());

        // Graceful shutdown: wait up to 120s for in-flight scans to complete
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(120);

        executor.initialize();
        return executor;
    }
}
