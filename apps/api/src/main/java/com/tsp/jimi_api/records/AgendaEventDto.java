package com.tsp.jimi_api.records;

import com.tsp.jimi_api.entities.Agenda;
import com.tsp.jimi_api.enums.Type;

import java.sql.Date;
import java.sql.Time;

/**
 * Read-only view of one agenda event, returned by GET /agenda.
 *
 * Times are serialized as ISO strings (date as YYYY-MM-DD, time as HH:mm:ss)
 * so the frontend never has to parse Java SQL types.
 */
public record AgendaEventDto(
        Long id,
        String date,
        String beginTime,
        String endTime,
        Type type,
        String title) {

    public static AgendaEventDto from(final Agenda a) {
        return new AgendaEventDto(
                a.getId(),
                toString(a.getDate()),
                toString(a.getBegin()),
                toString(a.getEnd()),
                a.getType(),
                a.getTitle());
    }

    private static String toString(final Date d) {
        return d == null ? null : d.toString();
    }

    private static String toString(final Time t) {
        return t == null ? null : t.toString();
    }
}
