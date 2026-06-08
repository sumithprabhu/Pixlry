package com.imageplatform.worker.consumer;

import com.imageplatform.common.event.JobCreatedEvent;
import com.imageplatform.common.event.JobCompletedEvent;
import com.imageplatform.common.constants.JobStatus;
import com.imageplatform.worker.processor.ImageProcessorFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

@Slf4j
@Component
@RequiredArgsConstructor
public class JobConsumer {

    private final ExecutorService imageProcessingExecutor;
    private final ImageProcessorFactory processorFactory;
    private final RabbitTemplate rabbitTemplate;

    @RabbitListener(queues = "job.created", concurrency = "2-5")
    public void consume(JobCreatedEvent event) {
        log.info("Received job: {} | operation: {}", event.getJobId(), event.getOperationType());

        CompletableFuture.supplyAsync(() -> processorFactory.process(event), imageProcessingExecutor)
                .thenAccept(outputPath -> publishCompletion(event, outputPath, null))
                .exceptionally(ex -> {
                    publishCompletion(event, null, ex.getMessage());
                    return null;
                });
    }

    private void publishCompletion(JobCreatedEvent event, String outputPath, String error) {
        JobCompletedEvent completed = JobCompletedEvent.builder()
                .jobId(event.getJobId())
                .userId(event.getUserId())
                .status(error == null ? JobStatus.COMPLETED : JobStatus.FAILED)
                .outputFilePath(outputPath)
                .errorMessage(error)
                .completedAt(Instant.now())
                .build();

        rabbitTemplate.convertAndSend("image.jobs", "job.completed", completed);
        log.info("Job {} finished with status: {}", event.getJobId(), completed.getStatus());
    }
}
