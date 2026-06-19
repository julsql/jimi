-- JIMI no longer stores calendar events: the user's connected calendar
-- (Google / CalDAV / Microsoft) is the single source of truth. The only
-- persisted state is in-progress conversation drafts/pending actions and the
-- encrypted OAuth tokens for each linked calendar account.
DROP TABLE IF EXISTS agenda;
DROP TABLE IF EXISTS conversation;
DROP TABLE IF EXISTS calendar_account;

CREATE TABLE conversation (
    id VARCHAR(36) PRIMARY KEY,
    user_id VARCHAR(255) NOT NULL,
    category VARCHAR(10),
    status VARCHAR(30) NOT NULL,
    draft_json TEXT,
    history_json TEXT,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    INDEX idx_conversation_user (user_id)
);

CREATE TABLE calendar_account (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id VARCHAR(255) NOT NULL,
    provider VARCHAR(20) NOT NULL,
    account_email VARCHAR(255),
    access_token_enc TEXT NOT NULL,
    refresh_token_enc TEXT,
    access_token_expiry TIMESTAMP NULL,
    scopes VARCHAR(512),
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    UNIQUE KEY uq_account_user_provider (user_id, provider)
);
