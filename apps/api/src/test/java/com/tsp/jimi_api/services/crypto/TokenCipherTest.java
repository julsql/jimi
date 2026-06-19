package com.tsp.jimi_api.services.crypto;

import org.junit.jupiter.api.Test;

import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TokenCipherTest {

    private static final String KEY = Base64.getEncoder().encodeToString(new byte[32]);

    @Test
    void roundTripsAToken() {
        TokenCipher cipher = new TokenCipher(KEY);
        String secret = "ya29.a0AfH-very-secret-refresh-token";

        String encrypted = cipher.encrypt(secret);

        assertThat(encrypted).isNotEqualTo(secret);
        assertThat(cipher.decrypt(encrypted)).isEqualTo(secret);
    }

    @Test
    void usesAFreshIvSoCiphertextDiffersEachTime() {
        TokenCipher cipher = new TokenCipher(KEY);

        assertThat(cipher.encrypt("same")).isNotEqualTo(cipher.encrypt("same"));
    }

    @Test
    void withoutAKey_isNotConfiguredAndRefusesToEncrypt() {
        TokenCipher cipher = new TokenCipher("");

        assertThat(cipher.isConfigured()).isFalse();
        assertThatThrownBy(() -> cipher.encrypt("x")).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void rejectsAWrongLengthKey() {
        assertThatThrownBy(() -> new TokenCipher(Base64.getEncoder().encodeToString(new byte[7])))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
