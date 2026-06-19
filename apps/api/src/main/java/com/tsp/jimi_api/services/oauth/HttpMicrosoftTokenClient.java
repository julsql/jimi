package com.tsp.jimi_api.services.oauth;

import com.tsp.jimi_api.configurations.MicrosoftOAuthProperties;
import org.json.JSONObject;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;

/**
 * HTTP implementation of {@link MicrosoftTokenClient} (RestTemplate).
 */
@Component
public class HttpMicrosoftTokenClient implements MicrosoftTokenClient {

    private final MicrosoftOAuthProperties props;
    private final RestTemplate restTemplate = new RestTemplate();

    public HttpMicrosoftTokenClient(final MicrosoftOAuthProperties props) {
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

    private MultiValueMap<String, String> baseForm() {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("client_id", props.getClientId());
        form.add("client_secret", props.getClientSecret());
        form.add("scope", props.getScope());
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
