package com.tsp.jimi_api.controllers;

import com.tsp.jimi_api.configurations.RetentionProperties;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Exposes the data-retention policy so the Privacy Policy page can show the
 * actual configured durations instead of hard-coded numbers — change the
 * {@code RETENTION_*} env vars and the page updates itself.
 */
@RestController
public class PrivacyController {

    private final RetentionProperties retention;

    public PrivacyController(final RetentionProperties retention) {
        this.retention = retention;
    }

    @Operation(summary = "The active data-retention windows (days), for the Privacy Policy page.")
    @GetMapping(value = "/privacy/retention", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> retention() {
        return ResponseEntity.ok(Map.of(
                "enabled", retention.isEnabled(),
                "userDays", retention.getUserDays(),
                "contextDays", retention.getContextDays()));
    }
}
