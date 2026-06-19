package com.tsp.jimi_api.controllers;

import com.tsp.jimi_api.global.Shared;
import com.tsp.jimi_api.services.CalendarAccountService;
import com.tsp.jimi_api.services.calendar.microsoft.MicrosoftTokenStore;
import com.tsp.jimi_api.services.oauth.MicrosoftOAuthService;
import io.swagger.v3.oas.annotations.Operation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.Map;

/**
 * Microsoft (Outlook / Microsoft 365) connection endpoints. Mirrors the Google
 * ones in {@link ConnectController}; the shared {@code GET /connections} already
 * reports every linked provider.
 */
@RestController
public class MicrosoftConnectController {

    private static final Logger LOGGER = LoggerFactory.getLogger(MicrosoftConnectController.class);

    private final MicrosoftOAuthService microsoftOAuth;
    private final CalendarAccountService accounts;

    public MicrosoftConnectController(final MicrosoftOAuthService microsoftOAuth,
                                      final CalendarAccountService accounts) {
        this.microsoftOAuth = microsoftOAuth;
        this.accounts = accounts;
    }

    @Operation(summary = "Start linking the user's Microsoft/Outlook calendar (redirects to Microsoft consent).")
    @GetMapping("/connect/microsoft")
    public ResponseEntity<?> connect(
            @RequestParam("userId") final String userId,
            @RequestParam(value = "returnUrl", required = false) final String returnUrl) {
        if (userId == null || userId.isBlank()) {
            return Shared.raiseError("Connect failed.", "Incorrect or missing user id.", LOGGER);
        }
        if (!microsoftOAuth.isConfigured()) {
            return Shared.raiseError("Connect failed.",
                    "Microsoft OAuth is not configured on the server.", LOGGER);
        }
        return ResponseEntity.status(HttpStatus.FOUND)
                .location(URI.create(microsoftOAuth.authorizationUrl(userId, returnUrl)))
                .build();
    }

    @Operation(summary = "OAuth callback for Microsoft; links the account and returns to the app.")
    @GetMapping("/oauth/microsoft/callback")
    public ResponseEntity<?> callback(
            @RequestParam(value = "code", required = false) final String code,
            @RequestParam(value = "state", required = false) final String state,
            @RequestParam(value = "error", required = false) final String error) {

        if (error != null || code == null || state == null) {
            LOGGER.warn("[oauth] microsoft callback error={} hasCode={} hasState={}",
                    error, code != null, state != null);
            return redirect(microsoftOAuth.appReturn("error"));
        }
        try {
            return redirect(microsoftOAuth.handleCallback(code, state));
        } catch (Exception e) {
            LOGGER.warn("[oauth] microsoft callback failed: {}", e.getMessage());
            return redirect(microsoftOAuth.appReturn("error"));
        }
    }

    @Operation(summary = "Disconnect (unlink) the user's Microsoft calendar.")
    @DeleteMapping(value = "/connect/microsoft", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> disconnect(@RequestParam("userId") final String userId) {
        if (userId == null || userId.isBlank()) {
            return Shared.raiseError("Disconnect failed.", "Incorrect or missing user id.", LOGGER);
        }
        accounts.unlink(userId, MicrosoftTokenStore.MICROSOFT);
        return ResponseEntity.ok(Map.of("disconnected", true));
    }

    private ResponseEntity<?> redirect(final String url) {
        return ResponseEntity.status(HttpStatus.FOUND).location(URI.create(url)).build();
    }
}
