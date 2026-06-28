package com.tsp.jimi_api.entities;

import com.tsp.jimi_api.enums.AnnouncementLevel;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * A broadcast message shown to every app user in a popup. Created/retired
 * through the admin endpoints; the app reads only the latest active one via
 * {@code GET /announcement}. This lets us communicate (outage, maintenance,
 * notice…) without shipping a new app build.
 *
 * <p>"Don't show again" is tracked client-side per id, so retiring/posting a
 * new announcement (new id) makes the popup reappear — no per-user server state.
 */
@Entity
@Table(name = "announcement")
public class Announcement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Lob
    @Column(nullable = false)
    private String message;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private AnnouncementLevel level;

    /** Soft-retire flag: an inactive announcement is never served. */
    @Column(nullable = false)
    private boolean active;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    public Announcement() {
    }

    public Announcement(final String message, final AnnouncementLevel level, final Instant createdAt) {
        this.message = message;
        this.level = level;
        this.active = true;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public void setId(final Long id) {
        this.id = id;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(final String message) {
        this.message = message;
    }

    public AnnouncementLevel getLevel() {
        return level;
    }

    public void setLevel(final AnnouncementLevel level) {
        this.level = level;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(final boolean active) {
        this.active = active;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(final Instant createdAt) {
        this.createdAt = createdAt;
    }
}
