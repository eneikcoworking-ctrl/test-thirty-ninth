-- Add use_llm_personalization flag to campaigns table
ALTER TABLE campaigns ADD COLUMN use_llm_personalization BOOLEAN NOT NULL DEFAULT FALSE;
