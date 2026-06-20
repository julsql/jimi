package com.tsp.jimi_api.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * A rolling per-user conversation memory: the last few (user/assistant) turns,
 * fed back to the LLM on every message so it keeps the thread — e.g. "what do I
 * have in an hour?" then "delete it" resolves to the event just discussed.
 *
 * <p>One row per user (id = userId). Holds no calendar data, only the recent
 * chat messages (trimmed to a small window).
 */
@Entity
@Table(name = "chat_context")
public class ChatContext {

    @Id
    @Column(name = "user_id", length = 255)
    private String userId;

    @Lob
    @Column(name = "history_json", columnDefinition = "TEXT")
    private String historyJson;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public ChatContext() {
    }

    public ChatContext(final String userId) {
        this.userId = userId;
    }

    @PrePersist
    @PreUpdate
    void touch() {
        this.updatedAt = Instant.now();
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(final String userId) {
        this.userId = userId;
    }

    public String getHistoryJson() {
        return historyJson;
    }

    public void setHistoryJson(final String historyJson) {
        this.historyJson = historyJson;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
