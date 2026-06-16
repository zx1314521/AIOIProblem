ALTER TABLE batch_job_items
    ADD COLUMN task_type VARCHAR(32);

UPDATE batch_job_items
SET task_type = 'PROBLEM_ANALYSIS'
WHERE task_type IS NULL;

ALTER TABLE batch_job_items
    ALTER COLUMN task_type SET NOT NULL;
