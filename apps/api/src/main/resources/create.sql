DROP TABLE IF EXISTS agenda;
DROP TABLE IF EXISTS conversation;

CREATE TABLE agenda (
    id INT AUTO_INCREMENT PRIMARY KEY,
    date DATE,
    type VARCHAR(10),
    begin_time TIME,
    end_time TIME,
    title VARCHAR(255),
    user_id VARCHAR(255)
);

CREATE TABLE conversation (
    id VARCHAR(36) PRIMARY KEY,
    user_id VARCHAR(255) NOT NULL,
    category VARCHAR(10),
    status VARCHAR(20) NOT NULL,
    draft_json TEXT,
    history_json TEXT,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    INDEX idx_conversation_user (user_id)
);
