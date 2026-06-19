package com.tsp.jimi_api.services;

import com.tsp.jimi_api.enums.ConversationStatus;
import com.tsp.jimi_api.records.CalendarEvent;
import com.tsp.jimi_api.records.ChatApiRequest;
import com.tsp.jimi_api.records.ChatApiResponse;
import com.tsp.jimi_api.records.ConfirmRequest;
import com.tsp.jimi_api.services.calendar.CalendarService;
import com.tsp.jimi_api.services.calendar.local.LocalDbCalendarProvider;
import com.tsp.jimi_api.support.FakeCalendarProvider;
import com.tsp.jimi_api.support.FakeLlmClient;
import com.tsp.jimi_api.support.InMemoryAgendaRepository;
import com.tsp.jimi_api.support.InMemoryConversationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Behaviour of the chat orchestration in both modes:
 *   - calendar mode (calendarMode=true): connected calendar + confirmation; a
 *     destructive intent NEVER mutates the calendar until the user confirms.
 *   - legacy mode (calendarMode=false): JIMI's own DB, direct CRUD, no
 *     confirmation — the pre-pivot contract deployed apps rely on.
 */
class ChatServiceTest {

    private FakeLlmClient llm;
    private FakeCalendarProvider external;
    private InMemoryAgendaRepository agendaRepo;
    private LocalDbCalendarProvider local;
    private ChatService chatService;

    @BeforeEach
    void setUp() {
        llm = new FakeLlmClient();
        external = new FakeCalendarProvider();
        agendaRepo = new InMemoryAgendaRepository();
        local = new LocalDbCalendarProvider(agendaRepo);
        CalendarService calendarService = new CalendarService(List.of(external, local), local);
        ConversationService conversationService =
                new ConversationService(new InMemoryConversationRepository());
        chatService = new ChatService(llm, conversationService, calendarService);
    }

    private ChatApiResponse send(final String message) {
        return chatService.handle(new ChatApiRequest("u1", message, null, true));
    }

    private ChatApiResponse sendLegacy(final String message) {
        return chatService.handle(new ChatApiRequest("u1", message, null, false));
    }

    // ----- calendar mode -----------------------------------------------------

    @Test
    void noCalendarConnected_returnsNeedsConnection_andWritesNothing() {
        external.connected = false;
        llm.enqueue("""
                { "category": "CREATE", "new_value": {"title": "Lunch", "start": "2026-06-20T13:00"} }
                """);

        ChatApiResponse response = send("Lunch tomorrow at 1pm");

        assertThat(response.status()).isEqualTo(ConversationStatus.NEEDS_CONNECTION);
        assertThat(external.created).isEmpty();
    }

    @Test
    void completeCreate_writesStraightToTheCalendar_noConfirmationNeeded() {
        llm.enqueue("""
                { "category": "CREATE",
                  "new_value": {"title": "Lunch with Alex", "start": "2026-06-20T13:00",
                                "end": "2026-06-20T14:00", "location": "Café X"},
                  "response": "Added it! 🍽️" }
                """);

        ChatApiResponse response = send("Lunch with Alex tomorrow 1-2pm at Café X");

        assertThat(response.status()).isEqualTo(ConversationStatus.COMPLETED);
        assertThat(external.created).hasSize(1);
        assertThat(external.created.get(0).title()).isEqualTo("Lunch with Alex");
        assertThat(response.eventUrl()).isEqualTo("https://calendar/new-1");
    }

    @Test
    void delete_returnsConfirmation_andDoesNotTouchTheCalendarYet() {
        external.events = List.of(new CalendarEvent("evt-9", "Dentist", "2026-06-20T09:00",
                null, false, null, List.of(), null, null, "url/evt-9"));
        llm.enqueue("""
                { "category": "DELETE", "old_value": {"title": "Dentist"}, "response": "Sure." }
                """);

        ChatApiResponse response = send("Cancel my dentist appointment");

        assertThat(response.status()).isEqualTo(ConversationStatus.AWAITING_CONFIRMATION);
        assertThat(response.conversationId()).isNotNull();
        assertThat(response.message()).contains("Delete").contains("Dentist");
        assertThat(external.deleted).isEmpty();
    }

    @Test
    void confirmingDelete_executesOnTheExactEvent_withoutCallingTheLlmAgain() {
        external.events = List.of(new CalendarEvent("evt-9", "Dentist", "2026-06-20T09:00",
                null, false, null, List.of(), null, null, "url/evt-9"));
        llm.enqueue("""
                { "category": "DELETE", "old_value": {"title": "Dentist"} }
                """);

        String conversationId = send("Cancel my dentist appointment").conversationId();
        ChatApiResponse done = chatService.confirm(new ConfirmRequest("u1", conversationId, true));

        assertThat(done.status()).isEqualTo(ConversationStatus.COMPLETED);
        assertThat(external.deleted).containsExactly("evt-9");
    }

    @Test
    void decliningDelete_executesNothing() {
        external.events = List.of(new CalendarEvent("evt-9", "Dentist", "2026-06-20T09:00",
                null, false, null, List.of(), null, null, "url/evt-9"));
        llm.enqueue("""
                { "category": "DELETE", "old_value": {"title": "Dentist"} }
                """);

        String conversationId = send("Cancel my dentist appointment").conversationId();
        ChatApiResponse cancelled = chatService.confirm(new ConfirmRequest("u1", conversationId, false));

        assertThat(cancelled.status()).isEqualTo(ConversationStatus.COMPLETED);
        assertThat(external.deleted).isEmpty();
    }

    // ----- legacy mode (local DB, no connection, direct CRUD) ---------------

    @Test
    void legacyCreate_writesToTheLocalDb_withoutAnyConnection() {
        external.connected = false; // no calendar connected at all
        llm.enqueue("""
                { "category": "CREATE",
                  "new_value": {"title": "Standup", "start": "2026-06-20T09:00", "end": "2026-06-20T09:15"},
                  "response": "Added to your agenda." }
                """);

        ChatApiResponse response = sendLegacy("Standup tomorrow 9am");

        assertThat(response.status()).isEqualTo(ConversationStatus.COMPLETED);
        assertThat(agendaRepo.count()).isEqualTo(1);
        assertThat(external.created).isEmpty();
    }

    @Test
    void legacyDelete_executesDirectly_noConfirmation() {
        // Seed an event in the legacy DB.
        llm.enqueue("""
                { "category": "CREATE", "new_value": {"title": "Dentist", "start": "2026-06-20T09:00"},
                  "response": "ok" }
                """);
        sendLegacy("Dentist tomorrow 9am");
        assertThat(agendaRepo.count()).isEqualTo(1);

        llm.enqueue("""
                { "category": "DELETE", "old_value": {"title": "Dentist"}, "response": "Removed it." }
                """);
        ChatApiResponse response = sendLegacy("Delete my dentist appointment");

        assertThat(response.status()).isEqualTo(ConversationStatus.COMPLETED);
        assertThat(agendaRepo.count()).isZero();
    }
}
