UPDATE ai_settings
SET codex_timeout_seconds = 180
WHERE codex_timeout_seconds IS NULL OR codex_timeout_seconds < 180;
