package com.tsp.jimi_api.services;

import com.tsp.jimi_api.enums.ConversationStatus;
import com.tsp.jimi_api.records.CalendarEvent;
import com.tsp.jimi_api.records.ChatApiRequest;
import com.tsp.jimi_api.records.ChatApiResponse;
import com.tsp.jimi_api.records.ConfirmRequest;
import com.tsp.jimi_api.services.calendar.CalendarService;
import com.tsp.jimi_api.support.FakeCalendarProvider;
import com.tsp.jimi_api.support.FakeLlmClient;
import com.tsp.jimi_api.support.InMemoryConversationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Behaviour of the chat orchestration, with the safety guarantees as the
 * headline cases: a destructive intent NEVER mutates the calendar until the
 * user confirms, and the model is not consulted at confirmation time.
 */
class ChatServiceTest {

    private FakeLlmClient llm;
    private FakeCalendarProvider provider;
    private ChatService chatService;

    @BeforeEach
    void setUp() {
        llm = new FakeLlmClient();
        provider = new FakeCalendarProvider();
        CalendarService calendarService = new CalendarService(List.of(provider));
        ConversationService conversationService =
                new ConversationService(new InMemoryConversationRepository());
        chatService = new ChatService(llm, conversationService, calendarService);
    }

    private ChatApiResponse send(final String message) {
        return chatService.handle(new ChatApiRequest("u1", message, null));
    }

    @Test
    void noCalendarConnected_returnsNeedsConnection_andWritesNothing() {
        provider.connected = false;
        llm.enqueue("""
                { "category": "CREATE", "new_value": {"title": "Lunch", "start": "2026-06-20T13:00"} }
                """);

        ChatApiResponse response = send("Lunch tomorrow at 1pm");

        assertThat(response.status()).isEqualTo(ConversationStatus.NEEDS_CONNECTION);
        assertThat(provider.created).isEmpty();
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
        assertThat(provider.created).hasSize(1);
        assertThat(provider.created.get(0).title()).isEqualTo("Lunch with Alex");
        assertThat(provider.created.get(0).location()).isEqualTo("Café X");
        assertThat(response.eventUrl()).isEqualTo("https://calendar/new-1");
    }

    @Test
    void incompleteCreate_asksForMissingFields_andWritesNothing() {
        llm.enqueue("""
                { "category": "CREATE", "new_value": {"start": "2026-06-20T13:00"},
                  "missing_fields": ["title"], "response": "What should I call it?" }
                """);

        ChatApiResponse response = send("Something tomorrow at 1pm");

        assertThat(response.status()).isEqualTo(ConversationStatus.AWAITING_INFO);
        assertThat(response.missingFields()).contains("title");
        assertThat(response.conversationId()).isNotNull();
        assertThat(provider.created).isEmpty();
    }

    @Test
    void delete_returnsConfirmation_andDoesNotTouchTheCalendarYet() {
        provider.events = List.of(new CalendarEvent("evt-9", "Dentist", "2026-06-20T09:00",
                null, false, null, List.of(), null, null, "url/evt-9"));
        llm.enqueue("""
                { "category": "DELETE", "old_value": {"title": "Dentist"},
                  "response": "Sure." }
                """);

        ChatApiResponse response = send("Cancel my dentist appointment");

        assertThat(response.status()).isEqualTo(ConversationStatus.AWAITING_CONFIRMATION);
        assertThat(response.conversationId()).isNotNull();
        assertThat(response.message()).contains("Delete").contains("Dentist");
        // The guarantee: nothing was deleted just by asking.
        assertThat(provider.deleted).isEmpty();
    }

    @Test
    void confirmingDelete_executesOnTheExactEvent_withoutCallingTheLlmAgain() {
        provider.events = List.of(new CalendarEvent("evt-9", "Dentist", "2026-06-20T09:00",
                null, false, null, List.of(), null, null, "url/evt-9"));
        llm.enqueue("""
                { "category": "DELETE", "old_value": {"title": "Dentist"} }
                """);

        String conversationId = send("Cancel my dentist appointment").conversationId();

        // No further LLM reply is enqueued: confirm must not consult the model.
        ChatApiResponse done = chatService.confirm(new ConfirmRequest("u1", conversationId, true));

        assertThat(done.status()).isEqualTo(ConversationStatus.COMPLETED);
        assertThat(provider.deleted).containsExactly("evt-9");
    }

    @Test
    void decliningDelete_executesNothing() {
        provider.events = List.of(new CalendarEvent("evt-9", "Dentist", "2026-06-20T09:00",
                null, false, null, List.of(), null, null, "url/evt-9"));
        llm.enqueue("""
                { "category": "DELETE", "old_value": {"title": "Dentist"} }
                """);

        String conversationId = send("Cancel my dentist appointment").conversationId();
        ChatApiResponse cancelled = chatService.confirm(new ConfirmRequest("u1", conversationId, false));

        assertThat(cancelled.status()).isEqualTo(ConversationStatus.COMPLETED);
        assertThat(provider.deleted).isEmpty();
    }

    @Test
    void delete_withNoMatch_asksForClarification_neverDeletes() {
        provider.events = List.of(new CalendarEvent("evt-9", "Dentist", "2026-06-20T09:00",
                null, false, null, List.of(), null, null, "url/evt-9"));
        llm.enqueue("""
                { "category": "DELETE", "old_value": {"title": "Haircut"} }
                """);

        ChatApiResponse response = send("Cancel my haircut");

        assertThat(response.status()).isEqualTo(ConversationStatus.COMPLETED);
        assertThat(response.conversationId()).isNull();
        assertThat(provider.deleted).isEmpty();
    }
}
