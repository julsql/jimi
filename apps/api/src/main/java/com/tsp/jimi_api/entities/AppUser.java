package com.tsp.jimi_api.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * Minimal record of an anonymous app user, used only for data lifecycle:
 * {@code last_seen_at} is refreshed on every request so a scheduled job can
 * purge users (and all their data) after a period of inactivity.
 *
 * <p>Holds no PII — just the opaque app-generated userId and two timestamps.
 */
@Entity
@Table(name = "app_user")
public class AppUser {

    @Id
    @Column(name = "user_id", length = 255)
    private String userId;

    @Column(name = "first_seen_at", nullable = false)
    private Instant firstSeenAt;

    @Column(name = "last_seen_at", nullable = false)
    private Instant lastSeenAt;

    public AppUser() {
    }

    public AppUser(final String userId, final Instant now) {
        this.userId = userId;
        this.firstSeenAt = now;
        this.lastSeenAt = now;
    }

    public String getUserId() {
        return userId;
    }

    public Instant getFirstSeenAt() {
        return firstSeenAt;
    }

    public Instant getLastSeenAt() {
        return lastSeenAt;
    }

    public void setLastSeenAt(final Instant lastSeenAt) {
        this.lastSeenAt = lastSeenAt;
    }
}
