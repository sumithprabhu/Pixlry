package com.imageplatform.job.repository;

import com.imageplatform.common.constants.JobStatus;
import com.imageplatform.job.entity.Job;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface JobRepository extends JpaRepository<Job, UUID> {
    Page<Job> findByUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);
    long countByStatus(JobStatus status);
    Optional<Job> findByIdAndUserId(UUID id, UUID userId);
}
