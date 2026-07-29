ALTER TABLE dialogue_state ADD COLUMN IF NOT EXISTS tg_account_id BIGINT;

ALTER TABLE dialogue_state DROP CONSTRAINT IF EXISTS fk_dialogue_state_tg_account;
ALTER TABLE dialogue_state ADD CONSTRAINT fk_dialogue_state_tg_account FOREIGN KEY (tg_account_id) REFERENCES tg_accounts(id) ON DELETE CASCADE;

ALTER TABLE dialogue_state ADD COLUMN IF NOT EXISTS status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE';

CREATE INDEX IF NOT EXISTS idx_dialogue_state_status_time ON dialogue_state(status, updated_at DESC);

DROP VIEW IF EXISTS unified_inbox_view;
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
