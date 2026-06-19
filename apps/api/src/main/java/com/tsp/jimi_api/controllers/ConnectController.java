package com.tsp.jimi_api.controllers;

import com.tsp.jimi_api.global.Shared;
import com.tsp.jimi_api.records.CalDavConnectRequest;
import com.tsp.jimi_api.services.CalendarAccountService;
import com.tsp.jimi_api.services.calendar.caldav.CalDavAccountService;
import com.tsp.jimi_api.services.calendar.caldav.CalDavClient;
import com.tsp.jimi_api.services.calendar.caldav.CalDavCredentials;
import com.tsp.jimi_api.services.oauth.GoogleOAuthService;
import io.swagger.v3.oas.annotations.Operation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.Map;

/**
 * Calendar account connection endpoints (OAuth + management).
 *
 * <ul>
 *   <li>{@code GET  /connect/google?userId=} — 302 to Google's consent screen.</li>
 *   <li>{@code GET  /oauth/google/callback}  — Google redirects here; we link the
 *       account and 302 back into the app via its deep link.</li>
 *   <li>{@code GET  /connections?userId=}    — which providers the user has linked.</li>
 *   <li>{@code DELETE /connect/google?userId=} — revoke + unlink.</li>
 * </ul>
 */
@RestController
public class ConnectController {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConnectController.class);

    private final GoogleOAuthService googleOAuth;
    private final CalendarAccountService accounts;
    private final CalDavAccountService calDavAccounts;
    private final CalDavClient calDavClient;

    public ConnectController(final GoogleOAuthService googleOAuth,
                             final CalendarAccountService accounts,
                             final CalDavAccountService calDavAccounts,
                             final CalDavClient calDavClient) {
        this.googleOAuth = googleOAuth;
        this.accounts = accounts;
        this.calDavAccounts = calDavAccounts;
        this.calDavClient = calDavClient;
    }

    @Operation(summary = "Start linking the user's Google Calendar (redirects to Google consent).")
    @GetMapping("/connect/google")
    public ResponseEntity<?> connectGoogle(@RequestParam("userId") final String userId) {
        if (userId == null || userId.isBlank()) {
            return Shared.raiseError("Connect failed.", "Incorrect or missing user id.", LOGGER);
        }
        if (!googleOAuth.isConfigured()) {
            return Shared.raiseError("Connect failed.",
                    "Google OAuth is not configured on the server.", LOGGER);
        }
        return ResponseEntity.status(HttpStatus.FOUND)
                .location(URI.create(googleOAuth.authorizationUrl(userId)))
                .build();
    }

    @Operation(summary = "OAuth callback for Google; links the account and returns to the app.")
    @GetMapping("/oauth/google/callback")
    public ResponseEntity<?> googleCallback(
            @RequestParam(value = "code", required = false) final String code,
            @RequestParam(value = "state", required = false) final String state,
            @RequestParam(value = "error", required = false) final String error) {

        if (error != null || code == null || state == null) {
            LOGGER.warn("[oauth] google callback error={} hasCode={} hasState={}",
                    error, code != null, state != null);
            return redirect(googleOAuth.appReturn("error"));
        }
        try {
            return redirect(googleOAuth.handleCallback(code, state));
        } catch (Exception e) {
            LOGGER.warn("[oauth] google callback failed: {}", e.getMessage());
            return redirect(googleOAuth.appReturn("error"));
        }
    }

    @Operation(summary = "List the calendar providers the user has connected.")
    @GetMapping(value = "/connections", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> connections(@RequestParam("userId") final String userId) {
        if (userId == null || userId.isBlank()) {
            return Shared.raiseError("Lookup failed.", "Incorrect or missing user id.", LOGGER);
        }
        return ResponseEntity.ok(Map.of("providers", accounts.connectedProviders(userId)));
    }

    @Operation(summary = "Disconnect (revoke + unlink) the user's Google Calendar.")
    @DeleteMapping(value = "/connect/google", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> disconnectGoogle(@RequestParam("userId") final String userId) {
        if (userId == null || userId.isBlank()) {
            return Shared.raiseError("Disconnect failed.", "Incorrect or missing user id.", LOGGER);
        }
        accounts.unlink(userId, CalendarAccountService.GOOGLE);
        return ResponseEntity.ok(Map.of("disconnected", true));
    }

    @Operation(summary = "Link a CalDAV calendar (Apple iCloud / Fastmail / Nextcloud) via Basic auth.")
    @PostMapping(value = "/connect/caldav",
            consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> connectCalDav(@RequestBody final CalDavConnectRequest request) {
        if (request == null || isBlank(request.userId())) {
            return Shared.raiseError("Connect failed.", "Incorrect or missing user id.", LOGGER);
        }
        if (isBlank(request.serverUrl()) || isBlank(request.username()) || isBlank(request.password())) {
            return Shared.raiseError("Connect failed.",
                    "serverUrl, username and password are all required.", LOGGER);
        }
        CalDavCredentials creds = new CalDavCredentials(
                request.serverUrl().trim(), request.username(), request.password());
        try {
            if (!calDavClient.validate(creds)) {
                return Shared.raiseError("Connect failed.",
                        "CalDAV server rejected the credentials or collection URL.", LOGGER);
            }
        } catch (Exception e) {
            return Shared.raiseError("Connect failed.",
                    "Could not reach the CalDAV collection: " + e.getMessage(), LOGGER);
        }
        calDavAccounts.link(request.userId(), creds);
        return ResponseEntity.ok(Map.of("connected", true));
    }

    @Operation(summary = "Disconnect (unlink) the user's CalDAV calendar.")
    @DeleteMapping(value = "/connect/caldav", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> disconnectCalDav(@RequestParam("userId") final String userId) {
        if (isBlank(userId)) {
            return Shared.raiseError("Disconnect failed.", "Incorrect or missing user id.", LOGGER);
        }
        calDavAccounts.unlink(userId);
        return ResponseEntity.ok(Map.of("disconnected", true));
    }

    private static boolean isBlank(final String s) {
        return s == null || s.isBlank();
    }

    private ResponseEntity<?> redirect(final String url) {
        return ResponseEntity.status(HttpStatus.FOUND).location(URI.create(url)).build();
    }
}
