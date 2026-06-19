package com.tsp.jimi_api.configurations;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Secrets used to encrypt data at rest.
 *
 * <p>{@code security.token-encryption-key} must be a base64-encoded 32-byte
 * (256-bit) key, supplied via the {@code TOKEN_ENCRYPTION_KEY} env var. It
 * encrypts the OAuth access/refresh tokens stored in {@code calendar_account}
 * so a database leak never exposes usable calendar credentials.
 *
 * <p>Generate one with: {@code openssl rand -base64 32}
 */
@Configuration
@ConfigurationProperties(prefix = "security")
public class CryptoProperties {

    private String tokenEncryptionKey = "";

    public String getTokenEncryptionKey() {
        return tokenEncryptionKey;
    }

    public void setTokenEncryptionKey(final String tokenEncryptionKey) {
        this.tokenEncryptionKey = tokenEncryptionKey;
    }
}
