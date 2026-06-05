package com.imageplatform.job.service;

import com.imageplatform.common.constants.JobStatus;
import com.imageplatform.common.event.JobCreatedEvent;
import com.imageplatform.job.config.RabbitMQConfig;
import com.imageplatform.job.entity.Job;
import com.imageplatform.job.repository.JobRepository;
import com.imageplatform.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class JobService {

    private final JobRepository jobRepository;
    private final RabbitTemplate rabbitTemplate;

    @Transactional
    public Job createJob(Job job) {
        job.setStatus(JobStatus.PENDING);
        Job saved = jobRepository.save(job);

        JobCreatedEvent event = JobCreatedEvent.builder()
                .jobId(saved.getId())
                .userId(saved.getUserId())
                .fileName(saved.getOriginalFileName())
                .filePath(saved.getFilePath())
                .operationType(saved.getOperationType())
                .parameters(saved.getParameters())
                .createdAt(Instant.now())
                .build();

        rabbitTemplate.convertAndSend(RabbitMQConfig.JOB_EXCHANGE, RabbitMQConfig.JOB_CREATED_KEY, event);

        saved.setStatus(JobStatus.QUEUED);
        jobRepository.save(saved);

        log.info("Job created and queued: {}", saved.getId());
        return saved;
    }

    public Job getJobByIdAndUser(UUID jobId, UUID userId) {
        return jobRepository.findByIdAndUserId(jobId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Job", jobId));
    }

    public Page<Job> getUserJobs(UUID userId, Pageable pageable) {
        return jobRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
    }
}
