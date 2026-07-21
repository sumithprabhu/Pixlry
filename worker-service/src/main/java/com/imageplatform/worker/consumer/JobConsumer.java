package com.imageplatform.worker.consumer;

import com.imageplatform.common.constants.JobStatus;
import com.imageplatform.common.event.JobCompletedEvent;
import com.imageplatform.common.event.JobCreatedEvent;
import com.imageplatform.worker.processor.ImageProcessorFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

/**
 * SERVICE: worker-service
 * PURPOSE: Consume image processing jobs from RabbitMQ and process them asynchronously.
 *
 * CONCURRENCY MODEL:
 *   Two layers of concurrency work together here:
 *
 *   Layer 1 — RabbitMQ listener concurrency (concurrency = "2-5"):
 *     Spring creates between 2 and 5 listener threads. Each thread pulls messages
 *     from the queue and calls consume(). The broker auto-scales between min and max
 *     based on queue depth. These threads are RabbitMQ consumer threads.
 *
 *   Layer 2 — CompletableFuture on imageProcessingExecutor:
 *     Each RabbitMQ consumer thread immediately hands off the actual image work to
 *     our fixed thread pool (defined in ThreadPoolConfig). The consumer thread is
 *     freed instantly to pull the next message from the queue.
 *
 *   Result: if you have 5 RabbitMQ consumer threads and a pool of 10 processing threads,
 *   you can process 10 images in parallel while the 5 consumer threads pull 5 more.
 *
 * WHY CompletableFuture AND NOT JUST BLOCK?
 *   If the consumer thread blocked while processing the image, RabbitMQ wouldn't ACK
 *   the message until processing finished. With prefetch=5, you'd stall after 5 messages.
 *   CompletableFuture returns immediately — the consumer ACKs the message right away
 *   and pulls the next one. Processing happens in parallel on the executor pool.
 *
 * IMPORTANT — ACK TIMING:
 *   Since we use acknowledge-mode: auto, the message is ACKed when consume() returns,
 *   which is BEFORE the CompletableFuture completes. This means if the worker crashes
 *   mid-processing, the message is already ACKed and lost.
 *   For truly reliable processing, use acknowledge-mode: manual and ACK inside
 *   the CompletableFuture callback. This is a trade-off: reliability vs throughput.
 *
 * WORKER DOES NOT TOUCH THE DATABASE:
 *   After processing, we publish a JobCompletedEvent to RabbitMQ.
 *   The job-service consumes this event and updates the job status in its own DB.
 *   The notification-service consumes the same event and pushes a WebSocket update.
 *   This is the "event-driven status update" pattern.
 *
 * INTERVIEW Q: How do you process 500 images concurrently in Java?
 *   Submit each job as a CompletableFuture to a fixed thread pool sized for the CPU.
 *   Use CompletableFuture.allOf() to wait for all 500 to complete if you need a batch
 *   result. Each future runs on a separate thread, CPU cores are maximally utilized,
 *   and you avoid creating 500 raw threads (which would thrash the OS scheduler).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JobConsumer {

    private final ExecutorService imageProcessingExecutor;
    private final ImageProcessorFactory processorFactory;
    private final RabbitTemplate rabbitTemplate;

    @RabbitListener(queues = "job.created", concurrency = "2-5")
    public void consume(JobCreatedEvent event) {
        log.info("Received job {} | operation: {}", event.getJobId(), event.getOperationType());

        long startTime = System.currentTimeMillis();

        CompletableFuture
                .supplyAsync(() -> processorFactory.process(event), imageProcessingExecutor)
                .thenAccept(outputPath -> {
                    long elapsed = System.currentTimeMillis() - startTime;
                    publishCompletion(event, outputPath, null, elapsed);
                })
                .exceptionally(ex -> {
                    long elapsed = System.currentTimeMillis() - startTime;
                    log.error("Job {} failed: {}", event.getJobId(), ex.getMessage());
                    publishCompletion(event, null, ex.getCause() != null ? ex.getCause().getMessage() : ex.getMessage(), elapsed);
                    return null;
                });
    }

    private void publishCompletion(JobCreatedEvent event, String outputPath, String error, long processingTimeMs) {
        JobCompletedEvent completed = JobCompletedEvent.builder()
                .jobId(event.getJobId())
                .userId(event.getUserId())
                .status(error == null ? JobStatus.COMPLETED : JobStatus.FAILED)
                .outputFilePath(outputPath)
                .errorMessage(error)
                .processingTimeMs(processingTimeMs)
                .completedAt(Instant.now())
                .build();

        // One publish → fans out to job.completed.status, job.completed.notifications, job.completed.analytics
        rabbitTemplate.convertAndSend("image.jobs", "job.completed", completed);
        log.info("Published completion for job {} | status: {} | took: {}ms",
                event.getJobId(), completed.getStatus(), processingTimeMs);
    }
}
