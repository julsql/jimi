package com.tsp.jimi_api.services;

import com.tsp.jimi_api.entities.Conversation;
import com.tsp.jimi_api.enums.ConversationStatus;
import com.tsp.jimi_api.global.Prompts;
import com.tsp.jimi_api.records.ChatApiRequest;
import com.tsp.jimi_api.records.ChatApiResponse;
import com.tsp.jimi_api.records.EventExtraction;
import com.tsp.jimi_api.services.llm.LlmClient;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Orchestrates one POST /chat round-trip.
 *
 * Flow:
 *   1. Resume any existing draft conversation for this user (if conversationId given).
 *   2. Append the new user message to the history.
 *   3. Ask the LLM to extract structured event data.
 *   4. If the draft is still incomplete - persist it and return AWAITING_INFO.
 *   5. Otherwise - dispatch to AgendaService (CREATE/EDIT/DELETE/GET/OTHER),
 *      mark the conversation COMPLETED if any, and return the assistant message.
 */
@Service
public class ChatService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ChatService.class);

    private final LlmClient llmClient;
    private final ConversationService conversationService;
    private final AgendaService agendaService;

    public ChatService(final LlmClient llmClient,
                       final ConversationService conversationService,
                       final AgendaService agendaService) {
        this.llmClient = llmClient;
        this.conversationService = conversationService;
        this.agendaService = agendaService;
    }

    public ChatApiResponse handle(final ChatApiRequest request) {
        Conversation existing = conversationService
                .load(request.conversationId(), request.userId())
                .filter(c -> c.getStatus() == ConversationStatus.AWAITING_INFO)
                .orElse(null);

        List<LlmClient.ChatMessage> history = conversationService.appendUserMessage(existing, request.message());

        String rawJson = llmClient.complete(Prompts.extraction(), history);
        EventExtraction extraction = new EventExtraction(rawJson);

        LOGGER.info("[chat] userId={} category={} hasEvents={} rawExtraction={}",
                request.userId(),
                extraction.getCategory(),
                agendaService.hasAnyEvent(request.userId()),
                rawJson);

        List<String> missingFields = conversationService.computeMissingFields(extraction);
        if (!missingFields.isEmpty()) {
            Conversation saved = conversationService.persistDraft(
                    existing, request.userId(), extraction, history, rawJson);
            return new ChatApiResponse(
                    saved.getId(),
                    ConversationStatus.AWAITING_INFO,
                    extraction.getResponse(),
                    missingFields);
        }

        String assistantMessage = dispatch(extraction, request.userId());
        conversationService.complete(existing);

        return new ChatApiResponse(
                null,
                ConversationStatus.COMPLETED,
                assistantMessage,
                List.of());
    }

    private String dispatch(final EventExtraction extraction, final String userId) {
        return switch (extraction.getCategory()) {
            case CREATE -> {
                agendaService.create(extraction, userId);
                yield extraction.getResponse();
            }
            case EDIT -> {
                agendaService.edit(extraction, userId);
                yield extraction.getResponse();
            }
            case DELETE -> {
                agendaService.delete(extraction, userId);
                yield extraction.getResponse();
            }
            case GET -> answerAgendaQuestion(extraction, userId);
            case OTHER -> extraction.getResponse();
        };
    }

    /**
     * For GET we make a second LLM call: feed it the user's full agenda plus
     * the original question, and ask it to phrase a friendly answer. This is
     * the same two-pass pattern as the original implementation, just isolated.
     *
     * Short-circuits when the agenda is empty: skipping the LLM call here
     * stops it from hallucinating fake events when the user has none yet.
     */
    private String answerAgendaQuestion(final EventExtraction extraction, final String userId) {
        if (!agendaService.hasAnyEvent(userId)) {
            return "You don't have any events scheduled yet.";
        }

        String agendaText = agendaService.renderAgenda(userId);
        List<LlmClient.ChatMessage> followUp = new ArrayList<>();
        followUp.add(new LlmClient.ChatMessage("user", agendaText));
        followUp.add(new LlmClient.ChatMessage("user", "User question: " + extraction.getResponse()));

        String rawJson = llmClient.complete(Prompts.agendaSummary(), followUp);
        try {
            return new JSONObject(rawJson).optString("answer", extraction.getResponse());
        } catch (Exception e) {
            return Optional.ofNullable(rawJson).orElse(extraction.getResponse());
        }
    }
}
