ALTER TABLE batch_job_items
    ADD COLUMN ai_provider VARCHAR(80),
    ADD COLUMN ai_model VARCHAR(160),
    ADD COLUMN ai_confidence DOUBLE PRECISION,
    ADD COLUMN ai_reasoning_summary TEXT,
    ADD COLUMN ai_hints TEXT,
    ADD COLUMN ai_duration_ms BIGINT;
