-- JIMI no longer stores calendar events: the user's connected calendar
-- (Google / CalDAV / Microsoft) is the single source of truth. The only
-- persisted state is in-progress conversation drafts and pending actions.
DROP TABLE IF EXISTS agenda;
DROP TABLE IF EXISTS conversation;

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
