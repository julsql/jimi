package com.tsp.jimi_api.services.oauth;

import com.tsp.jimi_api.services.crypto.TokenCipher;
import org.junit.jupiter.api.Test;

import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OAuthStateCodecTest {

    private final TokenCipher cipher =
            new TokenCipher(Base64.getEncoder().encodeToString(new byte[32]));
    private final OAuthStateCodec codec = new OAuthStateCodec(cipher);

    @Test
    void roundTripsUserIdAndCarriesThePkceVerifierStatelessly() {
        OAuthStateCodec.IssuedState issued = codec.issue("user-42");

        OAuthStateCodec.StatePayload payload = codec.verify(issued.state());

        assertThat(payload.userId()).isEqualTo("user-42");
        assertThat(payload.codeVerifier()).isNotBlank();
        // PKCE challenge must be URL-safe base64 (no +, /, =).
        assertThat(issued.codeChallenge()).doesNotContain("+", "/", "=");
    }

    @Test
    void aTamperedStateIsRejected() {
        OAuthStateCodec.IssuedState issued = codec.issue("user-42");
        String tampered = issued.state().substring(1) + "A";

        assertThatThrownBy(() -> codec.verify(tampered)).isInstanceOf(RuntimeException.class);
    }
}
