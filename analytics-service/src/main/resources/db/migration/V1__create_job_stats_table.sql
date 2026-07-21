CREATE TABLE job_stats (
    id                  UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    job_id              UUID        NOT NULL,
    user_id             UUID        NOT NULL,
    status              VARCHAR(50) NOT NULL,
    operation_type      VARCHAR(50),
    processing_time_ms  BIGINT,
    recorded_at         TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Index for fast COUNT queries by status (used in dashboard summary)
CREATE INDEX idx_job_stats_status      ON job_stats(status);
CREATE INDEX idx_job_stats_user_id     ON job_stats(user_id);
CREATE INDEX idx_job_stats_recorded_at ON job_stats(recorded_at DESC);
