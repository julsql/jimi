package com.tsp.jimi_api.controllers;

import com.tsp.jimi_api.services.CalendarProviderAvailability;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Public, unauthenticated runtime config the app reads at startup to decide
 * which features to show.
 *
 * <p>{@code GET /config} advertises which calendar providers are currently
 * offered (enabled by deployment AND configured). Flip a provider off via its
 * {@code *_CALENDAR_ENABLED} env var and a redeploy: the app then hides that
 * connect option and the matching {@code /connect/*} endpoint refuses links —
 * no app release needed.
 */
@RestController
public class ConfigController {

    private final CalendarProviderAvailability availability;

    public ConfigController(final CalendarProviderAvailability availability) {
        this.availability = availability;
    }

    @Operation(summary = "Runtime config: which calendar providers the app should offer.")
    @ApiResponses(@ApiResponse(responseCode = "200",
            description = "Map of provider → enabled, e.g. {\"providers\":{\"google\":true,"
                    + "\"microsoft\":false,\"caldav\":true}}"))
    @GetMapping(value = "/config", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> config() {
        return ResponseEntity.ok(Map.of("providers", availability.snapshot()));
    }
}
