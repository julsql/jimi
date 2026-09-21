package com.tsp.jimi_api.entities;

import com.tsp.jimi_api.enums.Categories;
import com.tsp.jimi_api.enums.ConversationStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * Server-side draft of an in-progress event extraction.
 *
 * When the LLM cannot fully fill an event (missing date, time, etc.), the
 * partial JSON is persisted here keyed by a UUID. The frontend echoes that
 * UUID with its next message so we can resume without re-sending history.
 */
@Entity
@Table(name = "conversation")
public class Conversation {

    @Id
    @Column(name = "id", length = 36)
    private String id;

    @Column(name = "user_id", nullable = false)
    private String userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "category", length = 10)
    private Categories category;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 30, nullable = false)
    private ConversationStatus status;

    @Lob
    @Column(name = "draft_json", columnDefinition = "TEXT")
    private String draftJson;

    @Lob
    @Column(name = "history_json", columnDefinition = "TEXT")
    private String historyJson;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public Conversation() {
    }

    public Conversation(final String id, final String userId) {
        this.id = id;
        this.userId = userId;
        this.status = ConversationStatus.AWAITING_INFO;
    }

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        this.updatedAt = Instant.now();
    }

    public String getId() {
        return id;
    }

    public void setId(final String id) {
        this.id = id;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(final String userId) {
        this.userId = userId;
    }

    public Categories getCategory() {
        return category;
    }

    public void setCategory(final Categories category) {
        this.category = category;
    }

    public ConversationStatus getStatus() {
        return status;
    }

    public void setStatus(final ConversationStatus status) {
        this.status = status;
    }

    public String getDraftJson() {
        return draftJson;
    }

    public void setDraftJson(final String draftJson) {
        this.draftJson = draftJson;
    }

    public String getHistoryJson() {
        return historyJson;
    }

    public void setHistoryJson(final String historyJson) {
        this.historyJson = historyJson;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
