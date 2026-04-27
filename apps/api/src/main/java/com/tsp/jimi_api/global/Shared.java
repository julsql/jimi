package com.tsp.jimi_api.global;

import com.tsp.jimi_api.records.Error;
import org.slf4j.Logger;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.sql.Date;
import java.sql.Time;
import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Tiny utilities shared across the API: JSON indentation, error responses,
 * and SQL date/time parsers used by the LLM JSON parser.
 *
 * Prompts moved to {@link Prompts}.
 */
public final class Shared {

    public static final int INDENT = 4;

    private Shared() {
    }

    public static ResponseEntity<String> raiseError(final String error, final String reason, final Logger logger) {
        logger.error(reason);
        return ResponseEntity.badRequest()
                .contentType(MediaType.APPLICATION_JSON)
                .body(new Error(error, reason).toJson().toString(INDENT));
    }

    public static Time getTime(final String timeString) {
        LocalTime localTime = LocalTime.parse(timeString);
        return Time.valueOf(localTime);
    }

    public static Date getDate(final String dateString) {
        LocalDate localDate = LocalDate.parse(dateString);
        return Date.valueOf(localDate);
    }
}
