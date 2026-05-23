CREATE TABLE ai_settings (
    id BIGINT PRIMARY KEY,
    provider VARCHAR(24) NOT NULL,
    deep_seek_api_key TEXT,
    deep_seek_base_url VARCHAR(512),
    deep_seek_model VARCHAR(120),
    deep_seek_timeout_seconds INT,
    codex_command VARCHAR(255),
    codex_timeout_seconds INT
);

INSERT INTO ai_settings (
    id,
    provider,
    deep_seek_api_key,
    deep_seek_base_url,
    deep_seek_model,
    deep_seek_timeout_seconds,
    codex_command,
    codex_timeout_seconds
) VALUES (
    1,
    'codex',
    '',
    'https://api.deepseek.com/chat/completions',
    'deepseek-chat',
    45,
    'codex',
    60
);

CREATE TABLE batch_jobs (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(180) NOT NULL,
    status VARCHAR(24) NOT NULL,
    owner_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    total_count INT NOT NULL,
    success_count INT NOT NULL DEFAULT 0,
    failed_count INT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ
);

CREATE TABLE batch_job_items (
    id BIGSERIAL PRIMARY KEY,
    job_id BIGINT NOT NULL REFERENCES batch_jobs(id) ON DELETE CASCADE,
    title VARCHAR(220) NOT NULL,
    content TEXT NOT NULL,
    status VARCHAR(24) NOT NULL,
    problem_id BIGINT REFERENCES problems(id) ON DELETE SET NULL,
    error_message TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    started_at TIMESTAMPTZ,
    finished_at TIMESTAMPTZ
);

CREATE INDEX idx_batch_jobs_owner_created ON batch_jobs(owner_id, created_at DESC);
CREATE INDEX idx_batch_jobs_status ON batch_jobs(status);
CREATE INDEX idx_batch_items_job_status ON batch_job_items(job_id, status);

