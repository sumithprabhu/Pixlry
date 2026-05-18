package com.imageplatform.common.event;

import com.imageplatform.common.constants.JobStatus;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class JobCompletedEvent {

    private UUID jobId;
    private UUID userId;
    private JobStatus status;        // COMPLETED or FAILED
    private String outputFilePath;   // null if failed
    private String errorMessage;     // null if completed
    private long processingTimeMs;
    private Instant completedAt;
}
