package com.imageplatform.analytics.service;

import com.imageplatform.analytics.repository.JobStatRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * SERVICE: analytics-service
 * PURPOSE: Compute and cache dashboard statistics.
 *
 * CACHE-ASIDE PATTERN:
 *   1. Request arrives at GET /analytics/summary
 *   2. Spring checks Redis for key "analytics:summary"
 *   3. Cache HIT  → return cached value (~1ms, no DB query)
 *   4. Cache MISS → execute method, query DB, store result in Redis, return result
 *
 *   TTL is configured in application.yml (spring.cache.redis.time-to-live = 60s).
 *   This means the dashboard can be 60s stale at most — acceptable for analytics.
 *   We also evict the cache immediately when a job completes (see JobEventConsumer).
 *
 * WHY NOT JUST QUERY DB ON EVERY REQUEST?
 *   COUNT(*) and AVG() on millions of rows takes seconds. The dashboard would be
 *   painfully slow under load. Redis returns cached results in <1ms regardless of
 *   how many rows are in the table.
 *
 * INTERVIEW Q: What is the Cache-Aside pattern?
 *   The application checks the cache before querying the DB. On a miss, it queries
 *   the DB, populates the cache, and returns the result. On a hit, it returns from
 *   cache directly. The application manages cache population (vs write-through where
 *   writes update cache and DB simultaneously).
 */
@Service
@RequiredArgsConstructor
public class AnalyticsService {

    private final JobStatRepository jobStatRepository;

    @Cacheable(value = "analytics:summary")
    public Map<String, Object> getSummary() {
        long total     = jobStatRepository.count();
        long completed = jobStatRepository.countByStatus("COMPLETED");
        long failed    = jobStatRepository.countByStatus("FAILED");
        Double avgMs   = jobStatRepository.avgProcessingTimeMs();

        Map<String, Object> summary = new HashMap<>();
        summary.put("totalJobs", total);
        summary.put("completedJobs", completed);
        summary.put("failedJobs", failed);
        summary.put("avgProcessingTimeMs", avgMs != null ? Math.round(avgMs) : 0);
        return summary;
    }

    // Called by JobEventConsumer when a job completes — forces next request to re-query DB
    @CacheEvict(value = "analytics:summary", allEntries = true)
    public void evictSummaryCache() {
        // @CacheEvict does the work — method body intentionally empty
    }
}
