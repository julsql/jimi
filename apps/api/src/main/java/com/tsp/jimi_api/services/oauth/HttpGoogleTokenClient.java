package com.tsp.jimi_api.services.oauth;

import com.tsp.jimi_api.configurations.GoogleOAuthProperties;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;

/**
 * HTTP implementation of {@link GoogleTokenClient} (RestTemplate, matching the
 * project's existing LLM client style).
 */
@Component
public class HttpGoogleTokenClient implements GoogleTokenClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(HttpGoogleTokenClient.class);

    private final GoogleOAuthProperties props;
    private final RestTemplate restTemplate = new RestTemplate();

    public HttpGoogleTokenClient(final GoogleOAuthProperties props) {
        this.props = props;
    }

    @Override
    public TokenResponse exchangeCode(final String code, final String codeVerifier) {
        MultiValueMap<String, String> form = baseForm();
        form.add("grant_type", "authorization_code");
        form.add("code", code);
        form.add("redirect_uri", props.getRedirectUri());
        form.add("code_verifier", codeVerifier);
        return parse(post(form));
    }

    @Override
    public TokenResponse refresh(final String refreshToken) {
        MultiValueMap<String, String> form = baseForm();
        form.add("grant_type", "refresh_token");
        form.add("refresh_token", refreshToken);
        return parse(post(form));
    }

    @Override
    public void revoke(final String token) {
        try {
            MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
            form.add("token", token);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
            restTemplate.postForEntity(props.getRevokeUri(), new HttpEntity<>(form, headers), String.class);
        } catch (Exception e) {
            // Revocation is best-effort: the row is deleted regardless.
            LOGGER.warn("[oauth] token revocation failed: {}", e.getMessage());
        }
    }

    private MultiValueMap<String, String> baseForm() {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("client_id", props.getClientId());
        form.add("client_secret", props.getClientSecret());
        return form;
    }

    private String post(final MultiValueMap<String, String> form) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        return restTemplate.postForEntity(props.getTokenUri(), new HttpEntity<>(form, headers), String.class)
                .getBody();
    }

    private TokenResponse parse(final String body) {
        JSONObject json = new JSONObject(body);
        long expiresIn = json.optLong("expires_in", 3600);
        return new TokenResponse(
                json.getString("access_token"),
                json.has("refresh_token") ? json.getString("refresh_token") : null,
                Instant.now().plusSeconds(expiresIn),
                json.optString("scope", null));
    }
}
