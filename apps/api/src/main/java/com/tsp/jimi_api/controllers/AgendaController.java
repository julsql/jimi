package com.tsp.jimi_api.controllers;

import com.tsp.jimi_api.entities.Agenda;
import com.tsp.jimi_api.records.AgendaEventDto;
import com.tsp.jimi_api.services.AgendaService;
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
import java.util.stream.StreamSupport;

/**
 * Read-only access to a user's agenda. No LLM involvement — this is the path
 * the frontend uses for the schedule visualisation page.
 */
@RestController
@RequestMapping("/agenda")
public class AgendaController {

    private final AgendaService agendaService;

    public AgendaController(final AgendaService agendaService) {
        this.agendaService = agendaService;
    }

    @Operation(summary = "List all events for a user, sorted by date then start time.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Events for the given user", content = {
                    @Content(mediaType = "application/json", schema = @Schema(implementation = AgendaEventDto.class))})})
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<AgendaEventDto>> list(@RequestParam("userId") final String userId) {
        Iterable<Agenda> events = agendaService.findByUserId(userId);
        List<AgendaEventDto> sorted = StreamSupport.stream(events.spliterator(), false)
                .sorted(Comparator
                        .comparing(Agenda::getDate, Comparator.nullsLast(Date::compareTo))
                        .thenComparing(Agenda::getBegin, Comparator.nullsLast(Time::compareTo)))
                .map(AgendaEventDto::from)
                .toList();
        return ResponseEntity.ok(sorted);
    }
}
