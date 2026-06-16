ALTER TABLE ai_settings
    ADD COLUMN codex_model VARCHAR(120);

UPDATE ai_settings
SET codex_model = 'gpt-5.5'
WHERE codex_model IS NULL OR codex_model = '';
