package com.tsp.jimi_api.services.calendar.caldav;

import com.tsp.jimi_api.records.CalendarEvent;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CalDavCalendarProviderTest {

    private final CalDavCalendarProvider provider = new CalDavCalendarProvider(null, null);

    @Test
    void parsesEveryVeventOutOfAMultistatusReport() {
        String xml = """
                <?xml version="1.0" encoding="utf-8"?>
                <d:multistatus xmlns:d="DAV:" xmlns:c="urn:ietf:params:xml:ns:caldav">
                  <d:response>
                    <d:href>/calendars/home/a.ics</d:href>
                    <d:propstat><d:prop>
                      <c:calendar-data>BEGIN:VCALENDAR
                BEGIN:VEVENT
                UID:a
                SUMMARY:First
                DTSTART:20260620T090000
                END:VEVENT
                END:VCALENDAR</c:calendar-data>
                    </d:prop></d:propstat>
                  </d:response>
                  <d:response>
                    <d:href>/calendars/home/b.ics</d:href>
                    <d:propstat><d:prop>
                      <c:calendar-data>BEGIN:VCALENDAR
                BEGIN:VEVENT
                UID:b
                SUMMARY:Second &amp; last
                DTSTART:20260621T100000
                END:VEVENT
                END:VCALENDAR</c:calendar-data>
                    </d:prop></d:propstat>
                  </d:response>
                </d:multistatus>""";

        List<CalendarEvent> events = provider.parseReport(xml);

        assertThat(events).hasSize(2);
        assertThat(events.get(0).id()).isEqualTo("a");
        assertThat(events.get(0).title()).isEqualTo("First");
        assertThat(events.get(1).id()).isEqualTo("b");
        assertThat(events.get(1).title()).isEqualTo("Second & last");
    }

    @Test
    void returnsEmptyListForNullOrEmptyReport() {
        assertThat(provider.parseReport(null)).isEmpty();
        assertThat(provider.parseReport("<d:multistatus/>")).isEmpty();
    }
}
