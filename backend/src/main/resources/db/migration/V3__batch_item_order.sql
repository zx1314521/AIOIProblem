ALTER TABLE batch_job_items
    ADD COLUMN sort_order INT NOT NULL DEFAULT 0;

UPDATE batch_job_items
SET sort_order = id;

CREATE INDEX idx_batch_items_job_order ON batch_job_items(job_id, sort_order, id);

