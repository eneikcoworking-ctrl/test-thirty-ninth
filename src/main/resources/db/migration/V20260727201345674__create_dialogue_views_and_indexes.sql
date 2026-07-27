-- V20260727201345674__create_dialogue_views_and_indexes.sql
-- Flyway migration to define optimized schema, view and indexes for cross-account dialogue statuses in the unified inbox.

CREATE TABLE telegram_accounts (
    id VARCHAR(255) PRIMARY KEY,
    phone_number VARCHAR(100),
    username VARCHAR(100),
    status VARCHAR(100)
);

CREATE TABLE conversations (
    id VARCHAR(255) PRIMARY KEY,
    telegram_account_id VARCHAR(255) NOT NULL,
    lead_username VARCHAR(100),
    lead_phone_number VARCHAR(100),
    status VARCHAR(100),
    updated_at TIMESTAMP,
    FOREIGN KEY (telegram_account_id) REFERENCES telegram_accounts(id)
);

-- Optimized index for conversation status and timestamp to ensure the unified inbox loads instantly
CREATE INDEX idx_conv_status_updated_at ON conversations(status, updated_at DESC);

-- View for unified cross-account queries
CREATE VIEW unified_inbox_view AS
SELECT
    c.id AS conversation_id,
    c.telegram_account_id,
    c.lead_username,
    c.lead_phone_number,
    c.status AS conversation_status,
    c.updated_at AS conversation_updated_at,
    ta.phone_number AS account_phone_number,
    ta.username AS account_username,
    ta.status AS account_status
FROM conversations c
JOIN telegram_accounts ta ON c.telegram_account_id = ta.id;
