package com.imageplatform.analytics.consumer;

import com.imageplatform.common.event.JobCompletedEvent;
import com.imageplatform.analytics.entity.JobStat;
import com.imageplatform.analytics.repository.JobStatRepository;
import com.imageplatform.analytics.service.AnalyticsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * SERVICE: analytics-service
 * PURPOSE: Consume job completion events and record stats in analytics_db.
 *          Also evicts the Redis cache so the next dashboard query reflects fresh data.
 *
 * CACHE EVICTION ON EVENT:
 *   The summary stats are cached in Redis with a TTL. When a job completes, the
 *   cached numbers are stale (they don't include this job yet). We evict the cache
 *   here so the next GET /analytics/summary hits the DB and returns fresh numbers.
 *
 *   Alternative: let the TTL expire naturally (simpler, slight staleness).
 *   We chose explicit eviction for a more responsive dashboard.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JobEventConsumer {

    private final JobStatRepository jobStatRepository;
    private final AnalyticsService analyticsService;

    @RabbitListener(queues = "job.completed.analytics")
    public void onJobCompleted(JobCompletedEvent event) {
        log.info("Recording analytics for jobId={} status={}", event.getJobId(), event.getStatus());

        JobStat stat = JobStat.builder()
                .jobId(event.getJobId())
                .userId(event.getUserId())
                .status(event.getStatus().name())
                .processingTimeMs(event.getProcessingTimeMs())
                .build();

        jobStatRepository.save(stat);

        // Evict cached summary so the next request sees this new job
        analyticsService.evictSummaryCache();
    }
}
