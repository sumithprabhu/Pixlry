package com.imageplatform.analytics.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

/**
 * SERVICE: analytics-service
 * PURPOSE: Analytics-local record of every completed/failed job event.
 *
 * WHY DOES ANALYTICS HAVE ITS OWN TABLE INSTEAD OF QUERYING jobs_db?
 *   Database-per-service is a core microservices principle. If analytics-service
 *   queried jobs_db directly, it would be coupled to job-service's schema.
 *   Any schema change in job-service (renaming a column, adding a table)
 *   would silently break analytics queries.
 *
 *   Instead, analytics builds its own read-optimised data from events.
 *   The trade-off: slight eventual consistency (analytics sees events a few ms late).
 *   For a dashboard, this is perfectly acceptable.
 *
 * This is also called the "materialized view" pattern — analytics maintains
 * a pre-computed view of job data, optimized for its own query patterns.
 */
@Entity
@Table(name = "job_stats")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class JobStat {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID jobId;

    @Column(nullable = false)
    private UUID userId;

    @Column(nullable = false)
    private String status;   // COMPLETED or FAILED

    private String operationType;

    private Long processingTimeMs;

    @CreationTimestamp
    private Instant recordedAt;
}
