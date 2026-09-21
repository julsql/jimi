package com.tsp.jimi_api.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.Instant;

/**
 * A calendar account a user has linked to JIMI (one row per user+provider).
 *
 * <p>Stores the OAuth tokens needed to act on the user's behalf — always
 * <strong>encrypted</strong> (see {@code TokenCipher}); the raw tokens never
 * touch the database. No calendar <em>content</em> is stored here, only the
 * credentials and minimal metadata.
 */
@Entity
@Table(name = "calendar_account",
        uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "provider"}))
public class CalendarAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private String userId;

    /** Provider id: "google", "caldav", "microsoft". */
    @Column(name = "provider", nullable = false, length = 20)
    private String provider;

    /** Connected account email, shown to the user (e.g. "me@gmail.com"). */
    @Column(name = "account_email")
    private String accountEmail;

    @Column(name = "access_token_enc", columnDefinition = "TEXT", nullable = false)
    private String accessTokenEnc;

    /** Encrypted refresh token; null if the provider didn't return one. */
    @Column(name = "refresh_token_enc", columnDefinition = "TEXT")
    private String refreshTokenEnc;

    @Column(name = "access_token_expiry")
    private Instant accessTokenExpiry;

    @Column(name = "scopes", length = 512)
    private String scopes;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public CalendarAccount() {
    }

    public CalendarAccount(final String userId, final String provider) {
        this.userId = userId;
        this.provider = provider;
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

    public Long getId() {
        return id;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(final String userId) {
        this.userId = userId;
    }

    public String getProvider() {
        return provider;
    }

    public void setProvider(final String provider) {
        this.provider = provider;
    }

    public String getAccountEmail() {
        return accountEmail;
    }

    public void setAccountEmail(final String accountEmail) {
        this.accountEmail = accountEmail;
    }

    public String getAccessTokenEnc() {
        return accessTokenEnc;
    }

    public void setAccessTokenEnc(final String accessTokenEnc) {
        this.accessTokenEnc = accessTokenEnc;
    }

    public String getRefreshTokenEnc() {
        return refreshTokenEnc;
    }

    public void setRefreshTokenEnc(final String refreshTokenEnc) {
        this.refreshTokenEnc = refreshTokenEnc;
    }

    public Instant getAccessTokenExpiry() {
        return accessTokenExpiry;
    }

    public void setAccessTokenExpiry(final Instant accessTokenExpiry) {
        this.accessTokenExpiry = accessTokenExpiry;
    }

    public String getScopes() {
        return scopes;
    }

    public void setScopes(final String scopes) {
        this.scopes = scopes;
    }
}
