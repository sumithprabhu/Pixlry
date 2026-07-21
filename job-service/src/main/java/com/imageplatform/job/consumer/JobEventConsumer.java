package com.imageplatform.job.consumer;

import com.imageplatform.common.event.JobCompletedEvent;
import com.imageplatform.job.entity.Job;
import com.imageplatform.job.repository.JobRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * SERVICE: job-service
 * PURPOSE: Listen for job completion events from workers and update job status in DB.
 *
 * WHY DOES JOB-SERVICE CONSUME ITS OWN COMPLETION EVENTS?
 *   The worker-service processes images but does NOT write to the database.
 *   This is intentional — the worker's only concern is image processing.
 *   If we gave the worker DB write access to jobs_db, we'd be coupling two
 *   services through a shared database (a common microservices anti-pattern).
 *
 *   Instead: worker publishes an event → job-service consumes it → updates its own DB.
 *   This keeps the job-service as the single source of truth for job state.
 *
 * OPTIMISTIC LOCKING:
 *   The Job entity has a @Version field for optimistic locking. If two events for
 *   the same job arrive concurrently (unlikely but possible in at-least-once delivery),
 *   only one update will succeed; the other will throw OptimisticLockException.
 *   RabbitMQ will redeliver the failed message and it will succeed on retry
 *   (since status is already COMPLETED/FAILED — the update is idempotent by status check).
 *
 * INTERVIEW Q: What is "at-least-once delivery" and why does it matter here?
 *   RabbitMQ guarantees at-least-once delivery — a message may be delivered more than
 *   once (e.g., if the consumer crashes after processing but before acknowledging).
 *   Our status update must be idempotent: applying COMPLETED twice should be harmless.
 *   The null-check on job (if not found, skip) handles cases where job was deleted.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JobEventConsumer {

    private final JobRepository jobRepository;

    @RabbitListener(queues = "job.completed.status")
    @Transactional
    public void onJobCompleted(JobCompletedEvent event) {
        log.info("Updating job status: jobId={} status={}", event.getJobId(), event.getStatus());

        jobRepository.findById(event.getJobId()).ifPresentOrElse(job -> {
            job.setStatus(event.getStatus());
            job.setOutputFilePath(event.getOutputFilePath());
            job.setErrorMessage(event.getErrorMessage());
            job.setProcessingTimeMs(event.getProcessingTimeMs());
            jobRepository.save(job);
            log.info("Job {} updated to {}", event.getJobId(), event.getStatus());
        }, () -> log.warn("Job not found for completion event: {}", event.getJobId()));
    }
}
