package com.tsp.jimi_api.controllers;

import com.tsp.jimi_api.entities.Agenda;
import com.tsp.jimi_api.records.AgendaEventDto;
import com.tsp.jimi_api.services.calendar.local.LocalDbCalendarProvider;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.sql.Date;
import java.sql.Time;
import java.util.Comparator;
import java.util.List;

/**
 * Read-only access to the legacy local agenda (JIMI's own DB). No LLM, no
 * connected calendar — this is the pre-pivot endpoint the deployed apps use to
 * render their schedule. Calendar-mode users read their events through the chat
 * (GET intent) instead; their events are not in this table.
 */
@RestController
@RequestMapping("/agenda")
public class AgendaController {

    private final LocalDbCalendarProvider localProvider;

    public AgendaController(final LocalDbCalendarProvider localProvider) {
        this.localProvider = localProvider;
    }

    @Operation(summary = "List the user's local agenda events, sorted by date then start time.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Events for the given user", content = {
                    @Content(mediaType = "application/json", schema = @Schema(implementation = AgendaEventDto.class))})})
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<AgendaEventDto>> list(@RequestParam("userId") final String userId) {
        List<AgendaEventDto> sorted = localProvider.listRaw(userId).stream()
                .sorted(Comparator
                        .comparing(Agenda::getDate, Comparator.nullsLast(Date::compareTo))
                        .thenComparing(Agenda::getBegin, Comparator.nullsLast(Time::compareTo)))
                .map(AgendaEventDto::from)
                .toList();
        return ResponseEntity.ok(sorted);
    }
}
