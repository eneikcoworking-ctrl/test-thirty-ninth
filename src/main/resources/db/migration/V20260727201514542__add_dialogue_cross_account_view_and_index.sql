-- Drop the view if it exists so we can recreate it with new fields
DROP VIEW IF EXISTS unified_inbox_view;

-- 1. Add tg_account_id linking dialogue_state to tg_accounts
ALTER TABLE dialogue_state ADD COLUMN tg_account_id BIGINT;
ALTER TABLE dialogue_state ADD CONSTRAINT fk_dialogue_state_tg_account FOREIGN KEY (tg_account_id) REFERENCES tg_accounts(id) ON DELETE CASCADE;

-- 2. Add status column to represent current dialogue / conversation status
ALTER TABLE dialogue_state ADD COLUMN status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE';

-- 3. Create optimized composite index on conversation status and timestamp (updated_at)
CREATE INDEX idx_dialogue_state_status_time ON dialogue_state(status, updated_at DESC);

-- 4. Create unified view for cross-account dialogues (unified inbox)
CREATE VIEW unified_inbox_view AS
SELECT
    ds.id AS dialogue_id,
    ds.tg_account_id,
    ta.phone_number AS account_phone_number,
    ds.status AS dialogue_status,
    ds.ai_turns_count,
    ds.human_intervention_required,
    ds.updated_at AS last_activity_at
FROM dialogue_state ds
JOIN tg_accounts ta ON ds.tg_account_id = ta.id;
