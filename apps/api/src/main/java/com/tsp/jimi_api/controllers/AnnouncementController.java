package com.tsp.jimi_api.controllers;

import com.tsp.jimi_api.entities.Announcement;
import com.tsp.jimi_api.records.AnnouncementDto;
import com.tsp.jimi_api.records.AnnouncementRequest;
import com.tsp.jimi_api.services.AnnouncementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

/**
 * Broadcast announcements shown in the app popup.
 *
 * <p>{@code GET /announcement} is public and read by every app at startup: it
 * returns the latest active announcement (or 204 when there is none). The app
 * remembers, per id, which ones the user dismissed with "Don't show again", so
 * posting a new one (new id) makes the popup reappear — no per-user state here.
 *
 * <p>Writes ({@code POST}/{@code DELETE /admin/announcement}) are guarded by the
 * {@code X-Admin-Token} header compared to the {@code ADMIN_TOKEN} env var. When
 * that var is unset the admin endpoints are disabled (403) — there is no other
 * auth layer in this service.
 */
@RestController
public class AnnouncementController {

    private static final Logger LOGGER = LoggerFactory.getLogger(AnnouncementController.class);

    private final AnnouncementService service;
    private final String adminToken;

    public AnnouncementController(final AnnouncementService service,
                                 @Value("${admin.token:}") final String adminToken) {
        this.service = service;
        this.adminToken = adminToken;
    }

    @Operation(summary = "Latest active announcement to show in the app popup (204 if none).")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "The current announcement", content = {
                    @Content(mediaType = "application/json", schema = @Schema(implementation = AnnouncementDto.class))}),
            @ApiResponse(responseCode = "204", description = "No active announcement")})
    @GetMapping(value = "/announcement", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<AnnouncementDto> latest() {
        return service.getLatestActive()
                .map(a -> ResponseEntity.ok(AnnouncementDto.from(a)))
                .orElseGet(() -> ResponseEntity.noContent().build());
    }

    @Operation(summary = "Admin: post a new announcement (requires X-Admin-Token).")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Announcement created"),
            @ApiResponse(responseCode = "400", description = "Blank message"),
            @ApiResponse(responseCode = "403", description = "Missing/invalid admin token")})
    @PostMapping(value = "/admin/announcement", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> create(@RequestHeader(value = "X-Admin-Token", required = false) final String token,
                                    @RequestBody final AnnouncementRequest request) {
        if (!authorized(token)) {
            return forbidden();
        }
        if (request == null || request.message() == null || request.message().isBlank()) {
            return ResponseEntity.badRequest().body("{\"error\":\"message must not be blank\"}");
        }
        Announcement created = service.create(request.message(), request.level());
        LOGGER.info("[announcement] created id={} level={}", created.getId(), created.getLevel());
        return ResponseEntity.status(201).body(AnnouncementDto.from(created));
    }

    @Operation(summary = "Admin: retire an announcement so it stops showing (requires X-Admin-Token).")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Retired (or already gone)"),
            @ApiResponse(responseCode = "403", description = "Missing/invalid admin token")})
    @DeleteMapping(value = "/admin/announcement/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> deactivate(@RequestHeader(value = "X-Admin-Token", required = false) final String token,
                                        @PathVariable final Long id) {
        if (!authorized(token)) {
            return forbidden();
        }
        service.deactivate(id);
        return ResponseEntity.noContent().build();
    }

    /** Admin writes require a configured token that the caller matches exactly. */
    private boolean authorized(final String token) {
        return adminToken != null && !adminToken.isBlank() && adminToken.equals(token);
    }

    private ResponseEntity<String> forbidden() {
        return ResponseEntity.status(403).body("{\"error\":\"invalid or missing admin token\"}");
    }
}
