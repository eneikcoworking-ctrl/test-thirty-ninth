-- Flyway migration to add session details to tg_accounts table.
-- Reserved Version: V20260727201057838

ALTER TABLE tg_accounts ADD COLUMN session_data TEXT;
ALTER TABLE tg_accounts ADD COLUMN session_type VARCHAR(50);
