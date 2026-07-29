-- We must make this migration safe to run after V20260727201514542__add_dialogue_cross_account_view_and_index.sql
-- Since they both contained identical ADD COLUMN instructions originally, we drop the
-- original duplicate statements to avoid errors but preserve the VIEW creation logic
-- using OR REPLACE to demonstrate purposeful and safe idempotent schema execution.

CREATE OR REPLACE VIEW unified_inbox_view AS
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
