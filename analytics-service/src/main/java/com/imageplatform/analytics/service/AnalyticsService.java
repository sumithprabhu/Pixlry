package com.imageplatform.analytics.service;

import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class AnalyticsService {

    // Cache-aside: result cached for TTL configured in application.yml.
    // On cache miss, hits the DB; on hit, returns from Redis in ~1ms.
    @Cacheable(value = "analytics:summary")
    public Map<String, Object> getSummary() {
        // Placeholder — real DB queries wired in Phase 6
        return Map.of(
                "totalJobs", 0,
                "completedJobs", 0,
                "failedJobs", 0,
                "avgProcessingTimeMs", 0
        );
    }
}
