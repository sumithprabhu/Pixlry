CREATE TABLE jobs (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id             UUID         NOT NULL,
    original_file_name  VARCHAR(255) NOT NULL,
    file_path           TEXT         NOT NULL,
    output_file_path    TEXT,
    operation_type      VARCHAR(50)  NOT NULL,
    status              VARCHAR(50)  NOT NULL DEFAULT 'PENDING',
    error_message       TEXT,
    processing_time_ms  BIGINT,
    version             BIGINT       NOT NULL DEFAULT 0,
    created_at          TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE TABLE job_parameters (
    job_id      UUID         NOT NULL REFERENCES jobs(id) ON DELETE CASCADE,
    param_key   VARCHAR(100) NOT NULL,
    param_value VARCHAR(500),
    PRIMARY KEY (job_id, param_key)
);

CREATE INDEX idx_jobs_user_id     ON jobs(user_id);
CREATE INDEX idx_jobs_status      ON jobs(status);
CREATE INDEX idx_jobs_created_at  ON jobs(created_at DESC);
