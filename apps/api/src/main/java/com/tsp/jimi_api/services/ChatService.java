package com.tsp.jimi_api.services;

import com.tsp.jimi_api.entities.Conversation;
import com.tsp.jimi_api.enums.Categories;
import com.tsp.jimi_api.enums.ConversationStatus;
import com.tsp.jimi_api.global.Prompts;
import com.tsp.jimi_api.records.CalendarEvent;
import com.tsp.jimi_api.records.ChatApiRequest;
import com.tsp.jimi_api.records.ChatApiResponse;
import com.tsp.jimi_api.records.ConfirmRequest;
import com.tsp.jimi_api.records.EventDraft;
import com.tsp.jimi_api.records.EventExtraction;
import com.tsp.jimi_api.records.ProposedAction;
import com.tsp.jimi_api.services.calendar.CalendarProvider;
import com.tsp.jimi_api.services.calendar.CalendarService;
import com.tsp.jimi_api.services.llm.LlmClient;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Orchestrates one POST /chat round-trip (and POST /chat/confirm).
 *
 * <p>Two modes, chosen by {@code request.calendarMode()}:
 * <ul>
 *   <li><b>legacy</b> (false — the default, what deployed apps send): events live
 *       in JIMI's own DB ({@code LocalDbCalendarProvider}). CREATE/EDIT/DELETE
 *       happen directly, no NEEDS_CONNECTION, no confirmation step — the
 *       pre-pivot contract.</li>
 *   <li><b>calendar</b> (true): acts on the user's connected calendar. If none is
 *       connected → NEEDS_CONNECTION. EDIT/DELETE return AWAITING_CONFIRMATION
 *       and only run after the user confirms via {@link #confirm} — the LLM is
 *       never in the destructive execution path.</li>
 * </ul>
 */
@Service
public class ChatService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ChatService.class);

    private static final String NEEDS_CONNECTION_MESSAGE =
            "I'd love to help! First connect the calendar you'd like me to manage "
                    + "(Google, Apple or Outlook) and we're good to go. 📅";

    private final LlmClient llmClient;
    private final ConversationService conversationService;
    private final CalendarService calendarService;

    public ChatService(final LlmClient llmClient,
                       final ConversationService conversationService,
                       final CalendarService calendarService) {
        this.llmClient = llmClient;
        this.conversationService = conversationService;
        this.calendarService = calendarService;
    }

    public ChatApiResponse handle(final ChatApiRequest request) {
        Conversation existing = conversationService
                .load(request.conversationId(), request.userId())
                .orElse(null);

        List<LlmClient.ChatMessage> history =
                conversationService.appendUserMessage(existing, request.message());

        String rawJson = llmClient.complete(Prompts.extraction(), history);
        EventExtraction extraction = new EventExtraction(rawJson);
        Categories category = extraction.getCategory();
        boolean calendarMode = request.calendarMode();

        LOGGER.info("[chat] userId={} category={} calendarMode={} rawExtraction={}",
                request.userId(), category, calendarMode, rawJson);

        // OTHER never touches the agenda — answer and stop.
        if (category == Categories.OTHER) {
            conversationService.complete(existing);
            return completed(extraction.getResponse(), null);
        }

        // Calendar mode requires a connected external calendar; legacy mode uses
        // the local DB and is always available.
        if (calendarMode && !calendarService.hasExternalConnected(request.userId())) {
            return new ChatApiResponse(null, ConversationStatus.NEEDS_CONNECTION,
                    NEEDS_CONNECTION_MESSAGE, List.of());
        }
        CalendarProvider provider = calendarService.resolveFor(request.userId(), calendarMode);

        return switch (category) {
            case CREATE -> handleCreate(provider, extraction, history, existing, request, rawJson);
            case GET -> handleGet(provider, extraction, request.userId(), existing);
            case EDIT, DELETE -> calendarMode
                    ? handleDestructiveWithConfirmation(provider, extraction, history, existing, request.userId())
                    : handleDestructiveDirect(provider, extraction, request.userId(), existing);
            default -> completed(extraction.getResponse(), null);
        };
    }

    /**
     * Executes (or declines) a previously proposed EDIT/DELETE on a connected
     * calendar. Reads the exact event ids the user confirmed — the LLM is not
     * consulted here. Only reachable in calendar mode.
     */
    public ChatApiResponse confirm(final ConfirmRequest request) {
        Conversation pending = conversationService
                .loadPending(request.conversationId(), request.userId())
                .orElseThrow(() -> new IllegalStateException("No pending action to confirm."));

        if (!request.confirmed()) {
            conversationService.cancel(pending);
            return completed("Okay, I won't make any changes. ✅", null);
        }

        ProposedAction action = ProposedAction.fromJson(pending.getDraftJson());
        CalendarProvider provider = calendarService.resolveExternal(request.userId());
        String eventUrl = execute(provider, action, request.userId());
        conversationService.complete(pending);

        String done = action.category() == Categories.DELETE
                ? "Done — I've removed it from your calendar. 🗑️"
                : "Done — your calendar is updated. ✅";
        return completed(done, eventUrl);
    }

    private String execute(final CalendarProvider provider, final ProposedAction action, final String userId) {
        String lastUrl = null;
        for (String eventId : action.eventIds()) {
            if (action.category() == Categories.DELETE) {
                provider.delete(userId, eventId);
            } else {
                lastUrl = provider.update(userId, eventId, action.changes()).url();
            }
        }
        return lastUrl;
    }

    private ChatApiResponse handleCreate(final CalendarProvider provider,
                                         final EventExtraction extraction,
                                         final List<LlmClient.ChatMessage> history,
                                         final Conversation existing,
                                         final ChatApiRequest request,
                                         final String rawJson) {
        List<String> missing = conversationService.computeMissingFields(extraction);
        if (!missing.isEmpty()) {
            Conversation saved = conversationService.persistDraft(
                    existing, request.userId(), extraction, history, rawJson);
            return new ChatApiResponse(saved.getId(), ConversationStatus.AWAITING_INFO,
                    extraction.getResponse(), missing);
        }

        CalendarEvent created = provider.create(request.userId(), extraction.getNewValue());
        conversationService.complete(existing);
        return completed(extraction.getResponse(), created.url());
    }

    private ChatApiResponse handleGet(final CalendarProvider provider,
                                      final EventExtraction extraction,
                                      final String userId,
                                      final Conversation existing) {
        conversationService.complete(existing);
        List<CalendarEvent> events = calendarService.upcomingEvents(provider, userId);
        if (events.isEmpty()) {
            return completed("You don't have any events scheduled. 🎉", null);
        }
        return completed(summarise(events, extraction.getResponse()), null);
    }

    /**
     * Calendar mode: resolve the target event(s) and ask the user to confirm.
     * Nothing is written here.
     */
    private ChatApiResponse handleDestructiveWithConfirmation(final CalendarProvider provider,
                                                              final EventExtraction extraction,
                                                              final List<LlmClient.ChatMessage> history,
                                                              final Conversation existing,
                                                              final String userId) {
        List<CalendarEvent> matches = calendarService.matchEvents(provider, userId, extraction.getOldValue());

        if (matches.isEmpty()) {
            return completed("I couldn't find a matching event. Could you tell me the "
                    + "title or date so I'm sure I act on the right one?", null);
        }
        if (matches.size() > 1) {
            return completed(disambiguation(matches), null);
        }

        CalendarEvent target = matches.get(0);
        ProposedAction action = new ProposedAction(
                extraction.getCategory(),
                List.of(target.id()),
                extraction.getNewValue(),
                summariseAction(extraction.getCategory(), target, extraction.getNewValue()));

        Conversation saved = conversationService.persistPendingAction(existing, userId, action, history);
        return new ChatApiResponse(saved.getId(), ConversationStatus.AWAITING_CONFIRMATION,
                action.summary(), List.of(), target.url());
    }

    /**
     * Legacy mode: edit/delete the matching event(s) in JIMI's own DB directly
     * and return COMPLETED — the pre-pivot behaviour deployed apps expect.
     */
    private ChatApiResponse handleDestructiveDirect(final CalendarProvider provider,
                                                    final EventExtraction extraction,
                                                    final String userId,
                                                    final Conversation existing) {
        List<CalendarEvent> matches = calendarService.matchEvents(provider, userId, extraction.getOldValue());
        for (CalendarEvent target : matches) {
            if (extraction.getCategory() == Categories.DELETE) {
                provider.delete(userId, target.id());
            } else {
                provider.update(userId, target.id(), extraction.getNewValue());
            }
        }
        conversationService.complete(existing);
        return completed(extraction.getResponse(), null);
    }

    private String disambiguation(final List<CalendarEvent> matches) {
        StringBuilder sb = new StringBuilder("I found several matching events. "
                + "Which one do you mean?\n");
        for (CalendarEvent e : matches) {
            sb.append("\n- ").append(e.describe());
        }
        return sb.toString();
    }

    private String summariseAction(final Categories category,
                                   final CalendarEvent target,
                                   final EventDraft changes) {
        if (category == Categories.DELETE) {
            return "Delete " + target.describe() + "? This can't be undone.";
        }
        StringBuilder sb = new StringBuilder("Update ").append(target.describe()).append("?");
        JSONObject diff = changes == null ? new JSONObject() : changes.toJson();
        if (diff.length() != 0) {
            sb.append("\nNew details: ").append(diff);
        }
        return sb.toString();
    }

    /**
     * Second LLM pass for GET: hand it the events plus the question and ask for a
     * friendly answer built only from those events.
     */
    private String summarise(final List<CalendarEvent> events, final String userQuestion) {
        StringBuilder block = new StringBuilder("My calendar events are:");
        for (CalendarEvent e : events) {
            block.append("\n- ").append(e.describe());
        }
        List<LlmClient.ChatMessage> followUp = new ArrayList<>();
        followUp.add(new LlmClient.ChatMessage("user", block.toString()));
        followUp.add(new LlmClient.ChatMessage("user", "User question: " + userQuestion));

        String rawJson = llmClient.complete(Prompts.agendaSummary(), followUp);
        try {
            return new JSONObject(rawJson).optString("answer", userQuestion);
        } catch (Exception e) {
            return Optional.ofNullable(rawJson).orElse(userQuestion);
        }
    }

    private ChatApiResponse completed(final String message, final String eventUrl) {
        return new ChatApiResponse(null, ConversationStatus.COMPLETED, message, List.of(), eventUrl);
    }
}
