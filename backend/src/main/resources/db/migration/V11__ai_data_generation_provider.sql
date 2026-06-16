ALTER TABLE ai_settings
    ADD COLUMN data_generation_provider VARCHAR(24);

UPDATE ai_settings
SET data_generation_provider = 'codex'
WHERE data_generation_provider IS NULL;
