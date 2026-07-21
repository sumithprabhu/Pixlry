package com.imageplatform.analytics.repository;

import com.imageplatform.analytics.entity.JobStat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.UUID;

public interface JobStatRepository extends JpaRepository<JobStat, UUID> {

    long countByStatus(String status);

    // AVG on a nullable column returns null if no rows exist — Double handles this gracefully
    @Query("SELECT AVG(j.processingTimeMs) FROM JobStat j WHERE j.status = 'COMPLETED'")
    Double avgProcessingTimeMs();
}
