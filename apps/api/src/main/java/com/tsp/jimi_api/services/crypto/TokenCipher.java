package com.tsp.jimi_api.services.crypto;

import com.tsp.jimi_api.configurations.CryptoProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Authenticated symmetric encryption (AES-256-GCM) for secrets stored at rest —
 * specifically the OAuth access/refresh tokens in {@code calendar_account}.
 *
 * <p>Output format is base64({@code iv[12] || ciphertext+tag}). A fresh random
 * IV is generated per call, so encrypting the same token twice yields different
 * ciphertexts.
 *
 * <p>If the key is missing OR invalid (bad base64 / wrong length) the bean still
 * constructs (so the app boots and chat keeps working); encryption is simply
 * disabled and any encrypt/decrypt call fails loudly, so calendar connection
 * stays unavailable until a valid {@code TOKEN_ENCRYPTION_KEY} is set. A bad key
 * must never take the whole API down.
 */
@Component
public class TokenCipher {

    private static final Logger LOGGER = LoggerFactory.getLogger(TokenCipher.class);

    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int IV_LENGTH = 12;
    private static final int TAG_BITS = 128;

    private final SecretKey key;
    private final SecureRandom random = new SecureRandom();

    @Autowired
    public TokenCipher(final CryptoProperties properties) {
        this(properties.getTokenEncryptionKey());
    }

    /** Direct constructor used by tests. */
    public TokenCipher(final String base64Key) {
        this.key = parseKey(base64Key);
    }

    public boolean isConfigured() {
        return key != null;
    }

    public String encrypt(final String plaintext) {
        if (plaintext == null) {
            return null;
        }
        try {
            byte[] iv = new byte[IV_LENGTH];
            random.nextBytes(iv);
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, requireKey(), new GCMParameterSpec(TAG_BITS, iv));
            byte[] ct = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));
            byte[] out = new byte[iv.length + ct.length];
            System.arraycopy(iv, 0, out, 0, iv.length);
            System.arraycopy(ct, 0, out, iv.length, ct.length);
            // URL-safe base64 (no '+' '/' '='): the OAuth state is carried in a
            // URL, where a '+' would be decoded back as a space and corrupt the
            // ciphertext, making decryption fail on the callback.
            return Base64.getUrlEncoder().withoutPadding().encodeToString(out);
        } catch (Exception e) {
            throw new IllegalStateException("Token encryption failed", e);
        }
    }

    public String decrypt(final String encoded) {
        if (encoded == null) {
            return null;
        }
        try {
            byte[] all = Base64.getUrlDecoder().decode(encoded);
            byte[] iv = new byte[IV_LENGTH];
            System.arraycopy(all, 0, iv, 0, IV_LENGTH);
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, requireKey(), new GCMParameterSpec(TAG_BITS, iv));
            byte[] pt = cipher.doFinal(all, IV_LENGTH, all.length - IV_LENGTH);
            return new String(pt, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new IllegalStateException("Token decryption failed", e);
        }
    }

    private SecretKey requireKey() {
        if (key == null) {
            throw new IllegalStateException(
                    "TOKEN_ENCRYPTION_KEY is not set — cannot store calendar credentials. "
                            + "Generate one with: openssl rand -base64 32");
        }
        return key;
    }

    private static SecretKey parseKey(final String base64Key) {
        if (base64Key == null || base64Key.isBlank()) {
            return null;
        }
        try {
            byte[] raw = Base64.getDecoder().decode(base64Key.trim());
            if (raw.length != 16 && raw.length != 24 && raw.length != 32) {
                LOGGER.error("TOKEN_ENCRYPTION_KEY must decode to 16/24/32 bytes (got {}); "
                        + "calendar encryption disabled. Generate one with: openssl rand -base64 32",
                        raw.length);
                return null;
            }
            return new SecretKeySpec(raw, "AES");
        } catch (IllegalArgumentException e) {
            LOGGER.error("TOKEN_ENCRYPTION_KEY is not valid base64; calendar encryption disabled. "
                    + "Generate one with: openssl rand -base64 32");
            return null;
        }
    }
}
