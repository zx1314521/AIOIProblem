ALTER TABLE problems
    ADD COLUMN external_platform VARCHAR(32),
    ADD COLUMN external_source_id VARCHAR(80),
    ADD COLUMN source_url VARCHAR(512);

CREATE UNIQUE INDEX uq_problems_external_source
    ON problems(external_platform, external_source_id)
    WHERE external_platform IS NOT NULL
      AND external_source_id IS NOT NULL;

ALTER TABLE batch_job_items
    ADD COLUMN external_platform VARCHAR(32),
    ADD COLUMN external_source_id VARCHAR(80),
    ADD COLUMN source_url VARCHAR(512),
    ADD COLUMN mark_passed_after_import BOOLEAN NOT NULL DEFAULT FALSE;

CREATE INDEX idx_batch_items_external_source
    ON batch_job_items(external_platform, external_source_id);
