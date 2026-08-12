package com.sanjay.aisecurity.ai;

import com.sanjay.aisecurity.exception.RateLimitException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Simple in-memory thread-safe rate limiter to protect the free Groq API quota on a per-user basis.
 *
 * <p>Uses a sliding-window algorithm: each user is allowed at most {@code maxRequests}
 * within a rolling {@code windowSeconds} window.
 *
 * <p>Stale entries are cleaned up every 5 minutes via {@link #cleanupStaleEntries()}
 * to prevent unbounded map growth across many users over time (Fix E1).
 *
 * @author Sanjay
 * @version 1.1.0
 */
@Component
public class SimpleRateLimiter {

    private final Map<String, List<Long>> userRequests = new ConcurrentHashMap<>();

    @Value("${app.ai.rate-limit.max-requests:10}")
    private int maxRequests;

    @Value("${app.ai.rate-limit.window-seconds:60}")
    private int windowSeconds;

    /**
     * Checks if the user has exceeded their request limit. Throws {@link RateLimitException} if exceeded.
     *
     * @param email user email identifier
     */
    public void checkLimit(String email) {
        long now = System.currentTimeMillis();
        long windowStart = now - (windowSeconds * 1000L);

        userRequests.compute(email, (key, timestamps) -> {
            if (timestamps == null) {
                timestamps = new ArrayList<>();
            }

            // Remove timestamps older than the sliding window
            timestamps.removeIf(t -> t < windowStart);

            if (timestamps.size() >= maxRequests) {
                throw new RateLimitException("AI request rate limit exceeded. Please wait a moment before trying again.");
            }

            timestamps.add(now);
            return timestamps;
        });
    }

    /**
     * Periodically removes stale user entries from the rate limiter map.
     *
     * <p>Runs every 5 minutes. An entry is removed only if its timestamp list
     * is empty after the current sliding window has been applied, meaning the
     * user has not made any AI requests recently. Active users are unaffected.
     *
     * <p>Fix E1: prevents the {@code userRequests} map from growing indefinitely
     * in long-running deployments with many unique users.
     */
    @Scheduled(fixedDelay = 300_000) // every 5 minutes
    public void cleanupStaleEntries() {
        long windowStart = System.currentTimeMillis() - (windowSeconds * 1000L);
        userRequests.entrySet().removeIf(entry -> {
            List<Long> timestamps = entry.getValue();
            // Remove old timestamps, then evict the entry if no recent requests remain
            synchronized (timestamps) {
                timestamps.removeIf(t -> t < windowStart);
                return timestamps.isEmpty();
            }
        });
    }
}
