package com.imageplatform.common.event;

import com.imageplatform.common.constants.OperationType;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class JobCreatedEvent {

    private UUID jobId;
    private UUID userId;
    private String fileName;
    private String filePath;
    private OperationType operationType;
    private Map<String, String> parameters;  // e.g. {"width":"800","height":"600"}
    private Instant createdAt;
}
