ALTER TABLE ai_settings
    ADD COLUMN problem_analysis_provider VARCHAR(24),
    ADD COLUMN recommendation_provider VARCHAR(24);

UPDATE ai_settings
SET problem_analysis_provider = provider,
    recommendation_provider = provider
WHERE problem_analysis_provider IS NULL
   OR recommendation_provider IS NULL;
