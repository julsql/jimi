package com.tsp.jimi_api.services;

import com.tsp.jimi_api.entities.Conversation;
import com.tsp.jimi_api.enums.Categories;
import com.tsp.jimi_api.enums.ConversationStatus;
import com.tsp.jimi_api.global.Messages;
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

import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Orchestrates one POST /chat round-trip (and POST /chat/confirm).
 *
 * <p>Two modes, chosen by {@code request.calendarMode()}: <b>legacy</b> (false,
 * the default) uses JIMI's own DB with direct CRUD; <b>calendar</b> (true) acts
 * on the user's connected calendar with NEEDS_CONNECTION + confirmation.
 *
 * <p>Cross-cutting: each turn is resolved in the user's {@code timezone} (for
 * "today"/"now" and new-event defaults), replies follow the user's language,
 * and a rolling per-user context is fed to the LLM so follow-ups like
 * "delete it" resolve to the event just discussed.
 */
@Service
public class ChatService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ChatService.class);

    private final LlmClient llmClient;
    private final ConversationService conversationService;
    private final CalendarService calendarService;
    private final UserActivityService userActivityService;

    public ChatService(final LlmClient llmClient,
                       final ConversationService conversationService,
                       final CalendarService calendarService,
                       final UserActivityService userActivityService) {
        this.llmClient = llmClient;
        this.conversationService = conversationService;
        this.calendarService = calendarService;
        this.userActivityService = userActivityService;
    }

    public ChatApiResponse handle(final ChatApiRequest request) {
        userActivityService.touch(request.userId());
        ZoneId zone = zoneOf(request.timezone());

        Conversation existing = conversationService
                .load(request.conversationId(), request.userId())
                .orElse(null);

        // Resuming a draft uses that draft's own thread; otherwise feed the
        // rolling per-user context so the LLM keeps the conversation.
        List<LlmClient.ChatMessage> history;
        if (existing != null) {
            history = conversationService.appendUserMessage(existing, request.message());
        } else {
            history = conversationService.loadContext(request.userId());
            history.add(new LlmClient.ChatMessage("user", request.message()));
        }

        String rawJson = llmClient.complete(Prompts.extraction(zone), history);
        EventExtraction extraction = new EventExtraction(rawJson);
        Categories category = extraction.getCategory();
        boolean calendarMode = request.calendarMode();

        LOGGER.info("[chat] userId={} category={} calendarMode={} lang={} tz={} rawExtraction={}",
                request.userId(), category, calendarMode, extraction.getLanguage(), zone, rawJson);

        ChatApiResponse response = route(request, extraction, category, calendarMode, zone, existing, history, rawJson);

        // Remember this turn so the next message has context.
        conversationService.recordContext(request.userId(), request.message(), response.message());
        return response;
    }

    private ChatApiResponse route(final ChatApiRequest request, final EventExtraction extraction,
                                  final Categories category, final boolean calendarMode, final ZoneId zone,
                                  final Conversation existing, final List<LlmClient.ChatMessage> history,
                                  final String rawJson) {
        String lang = extraction.getLanguage();

        if (category == Categories.OTHER) {
            conversationService.complete(existing);
            return completed(extraction.getResponse(), null);
        }

        if (calendarMode && !calendarService.hasExternalConnected(request.userId())) {
            return new ChatApiResponse(null, ConversationStatus.NEEDS_CONNECTION,
                    Messages.get(lang, Messages.Key.NEEDS_CONNECTION), List.of());
        }
        CalendarProvider provider = calendarService.resolveFor(request.userId(), calendarMode);

        return switch (category) {
            case CREATE -> handleCreate(provider, extraction, history, existing, request, rawJson);
            case GET -> handleGet(provider, extraction, request.userId(), existing, zone);
            case EDIT, DELETE -> calendarMode
                    ? handleDestructiveWithConfirmation(provider, extraction, history, existing, request.userId())
                    : handleDestructiveDirect(provider, extraction, request, existing);
            default -> completed(extraction.getResponse(), null);
        };
    }

    /**
     * Executes (or declines) a previously proposed EDIT/DELETE on a connected
     * calendar. Reads the exact event ids the user confirmed — the LLM is not
     * consulted here. Only reachable in calendar mode.
     */
    public ChatApiResponse confirm(final ConfirmRequest request) {
        userActivityService.touch(request.userId());
        Conversation pending = conversationService
                .loadPending(request.conversationId(), request.userId())
                .orElseThrow(() -> new IllegalStateException("No pending action to confirm."));

        ProposedAction action = ProposedAction.fromJson(pending.getDraftJson());
        String lang = action.language();

        if (!request.confirmed()) {
            conversationService.cancel(pending);
            return completed(Messages.get(lang, Messages.Key.CANCELLED), null);
        }

        CalendarProvider provider = calendarService.resolveExternal(request.userId());
        String eventUrl = execute(provider, action, request.userId());
        conversationService.complete(pending);

        Messages.Key key = action.category() == Categories.DELETE ? Messages.Key.DELETED : Messages.Key.UPDATED;
        return completed(Messages.get(lang, key), eventUrl);
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

        EventDraft draft = extraction.getNewValue().withDefaultTimezone(request.timezone());
        CalendarEvent created = provider.create(request.userId(), draft);
        conversationService.complete(existing);
        return completed(extraction.getResponse(), created.url());
    }

    private ChatApiResponse handleGet(final CalendarProvider provider,
                                      final EventExtraction extraction,
                                      final String userId,
                                      final Conversation existing,
                                      final ZoneId zone) {
        conversationService.complete(existing);
        List<CalendarEvent> events = calendarService.upcomingEvents(provider, userId);
        if (events.isEmpty()) {
            return completed(Messages.get(extraction.getLanguage(), Messages.Key.NO_EVENTS), null);
        }
        return completed(summarise(events, extraction.getResponse(), zone), null);
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
        String lang = extraction.getLanguage();
        List<CalendarEvent> matches = calendarService.matchEvents(provider, userId, extraction.getOldValue());

        if (matches.isEmpty()) {
            return completed(Messages.get(lang, Messages.Key.NO_MATCH), null);
        }

        // Confirm over ALL matched events in one go (so "delete them"/"both"
        // works). The user sees every event listed and confirms once; execution
        // acts on exactly these ids.
        List<String> eventIds = matches.stream().map(CalendarEvent::id).toList();
        ProposedAction action = new ProposedAction(
                extraction.getCategory(),
                eventIds,
                extraction.getNewValue(),
                summariseAction(lang, extraction.getCategory(), matches, extraction.getNewValue()),
                lang);

        Conversation saved = conversationService.persistPendingAction(existing, userId, action, history);
        String eventUrl = matches.size() == 1 ? matches.get(0).url() : null;
        return new ChatApiResponse(saved.getId(), ConversationStatus.AWAITING_CONFIRMATION,
                action.summary(), List.of(), eventUrl);
    }

    /**
     * Legacy mode: edit/delete the matching event(s) in JIMI's own DB directly
     * and return COMPLETED — the pre-pivot behaviour deployed apps expect.
     */
    private ChatApiResponse handleDestructiveDirect(final CalendarProvider provider,
                                                    final EventExtraction extraction,
                                                    final ChatApiRequest request,
                                                    final Conversation existing) {
        List<CalendarEvent> matches =
                calendarService.matchEvents(provider, request.userId(), extraction.getOldValue());
        for (CalendarEvent target : matches) {
            if (extraction.getCategory() == Categories.DELETE) {
                provider.delete(request.userId(), target.id());
            } else {
                provider.update(request.userId(), target.id(),
                        extraction.getNewValue().withDefaultTimezone(request.timezone()));
            }
        }
        conversationService.complete(existing);
        return completed(extraction.getResponse(), null);
    }

    /**
     * Confirmation prompt for one OR several target events. For a single event
     * it names it; for several it states the count and lists them, so the user
     * sees exactly what they're confirming.
     */
    private String summariseAction(final String lang, final Categories category,
                                   final List<CalendarEvent> targets, final EventDraft changes) {
        boolean single = targets.size() == 1;
        StringBuilder sb = new StringBuilder();
        if (category == Categories.DELETE) {
            sb.append(single
                    ? Messages.get(lang, Messages.Key.DELETE_CONFIRM, targets.get(0).describe())
                    : Messages.get(lang, Messages.Key.DELETE_CONFIRM_MANY, targets.size()));
        } else {
            sb.append(single
                    ? Messages.get(lang, Messages.Key.UPDATE_CONFIRM, targets.get(0).describe())
                    : Messages.get(lang, Messages.Key.UPDATE_CONFIRM_MANY, targets.size()));
        }
        if (!single) {
            for (CalendarEvent e : targets) {
                sb.append("\n- ").append(e.describe());
            }
        }
        if (category != Categories.DELETE) {
            JSONObject diff = changes == null ? new JSONObject() : changes.toJson();
            if (diff.length() != 0) {
                sb.append("\n").append(Messages.get(lang, Messages.Key.UPDATE_DETAILS, diff.toString()));
            }
        }
        return sb.toString();
    }

    /**
     * Second LLM pass for GET: hand it the events plus the question and ask for a
     * friendly answer (in the user's language) built only from those events.
     */
    private String summarise(final List<CalendarEvent> events, final String userQuestion, final ZoneId zone) {
        StringBuilder block = new StringBuilder("My calendar events are:");
        for (CalendarEvent e : events) {
            block.append("\n- ").append(e.describe());
        }
        List<LlmClient.ChatMessage> followUp = new ArrayList<>();
        followUp.add(new LlmClient.ChatMessage("user", block.toString()));
        followUp.add(new LlmClient.ChatMessage("user", "User question: " + userQuestion));

        String rawJson = llmClient.complete(Prompts.agendaSummary(zone), followUp);
        try {
            return new JSONObject(rawJson).optString("answer", userQuestion);
        } catch (Exception e) {
            return Optional.ofNullable(rawJson).orElse(userQuestion);
        }
    }

    private static ZoneId zoneOf(final String timezone) {
        if (timezone != null && !timezone.isBlank()) {
            try {
                return ZoneId.of(timezone.trim());
            } catch (Exception ignored) {
                // fall through to the server default
            }
        }
        return ZoneId.systemDefault();
    }

    private ChatApiResponse completed(final String message, final String eventUrl) {
        return new ChatApiResponse(null, ConversationStatus.COMPLETED, message, List.of(), eventUrl);
    }
}
