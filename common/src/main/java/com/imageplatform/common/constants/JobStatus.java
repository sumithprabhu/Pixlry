package com.imageplatform.common.constants;

public enum JobStatus {
    PENDING,      // job row created, not yet sent to queue
    QUEUED,       // event published to RabbitMQ
    PROCESSING,   // worker picked it up
    COMPLETED,
    FAILED,
    CANCELLED
}
