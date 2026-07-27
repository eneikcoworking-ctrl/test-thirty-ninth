-- U20260727201345674__create_dialogue_views_and_indexes.sql
-- Manual Rollback Script (For Database Administrator reference)
-- Note: Flyway Community Edition does not support automatic undo migrations, but this file serves as the documented disaster recovery path.

DROP VIEW IF EXISTS unified_inbox_view;
DROP INDEX IF EXISTS idx_conv_status_updated_at;
DROP TABLE IF EXISTS conversations;
DROP TABLE IF EXISTS telegram_accounts;
