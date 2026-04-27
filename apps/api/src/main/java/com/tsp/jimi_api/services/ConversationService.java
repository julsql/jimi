package com.tsp.jimi_api.services;

import com.tsp.jimi_api.entities.Conversation;
import com.tsp.jimi_api.enums.Categories;
import com.tsp.jimi_api.enums.ConversationStatus;
import com.tsp.jimi_api.records.EventExtraction;
import com.tsp.jimi_api.repositories.ConversationRepository;
import com.tsp.jimi_api.services.llm.LlmClient;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Manages multi-turn drafts that span more than one HTTP exchange.
 *
 * When the LLM cannot fully extract an event, we persist:
 *   - the partial draft (last LLM JSON output, so we can keep merging)
 *   - the raw conversation history (system + user + assistant)
 * keyed by a UUID we hand back to the frontend. On the next call the
 * frontend echoes that UUID; we re-load the history and append the new
 * user message before the next LLM call.
 */
@Service
public class ConversationService {

    private final ConversationRepository conversationRepository;

    public ConversationService(final ConversationRepository conversationRepository) {
        this.conversationRepository = conversationRepository;
    }

    /**
     * Loads a conversation for a given user, or returns empty if not found
     * or owned by someone else (defensive multi-tenant check).
     */
    public Optional<Conversation> load(final String conversationId, final String userId) {
        if (conversationId == null || conversationId.isBlank()) {
            return Optional.empty();
        }
        return conversationRepository.findByIdAndUserId(conversationId, userId);
    }

    /**
     * Appends a user message to a stored history (or starts a fresh one).
     */
    public List<LlmClient.ChatMessage> appendUserMessage(final Conversation conversation, final String userMessage) {
        List<LlmClient.ChatMessage> history = readHistory(conversation);
        history.add(new LlmClient.ChatMessage("user", userMessage));
        return history;
    }

    /**
     * Persists a draft when more info is needed. Creates a new row when
     * conversation is null, otherwise updates the existing one.
     */
    public Conversation persistDraft(
            final Conversation existing,
            final String userId,
            final EventExtraction extraction,
            final List<LlmClient.ChatMessage> history,
            final String assistantRawJson) {

        Conversation conversation = existing != null
                ? existing
                : new Conversation(UUID.randomUUID().toString(), userId);

        conversation.setCategory(extraction.getCategory());
        conversation.setStatus(ConversationStatus.AWAITING_INFO);
        conversation.setDraftJson(extraction.getNewValue().toJson().toString());

        List<LlmClient.ChatMessage> withAssistant = new ArrayList<>(history);
        withAssistant.add(new LlmClient.ChatMessage("assistant", assistantRawJson));
        conversation.setHistoryJson(serializeHistory(withAssistant));

        return conversationRepository.save(conversation);
    }

    /**
     * Marks a conversation as completed once the underlying agenda action
     * has succeeded, so the frontend can drop its conversationId.
     */
    public void complete(final Conversation conversation) {
        if (conversation == null) {
            return;
        }
        conversation.setStatus(ConversationStatus.COMPLETED);
        conversationRepository.save(conversation);
    }

    /**
     * True when the LLM signalled the draft is still incomplete, OR when
     * the category is CREATE and required event fields are unset.
     *
     * We trust the LLM's missing_fields list first, then double-check
     * server-side for CREATE so a sloppy LLM can't slip through with a
     * half-empty event.
     */
    public boolean needsMoreInfo(final EventExtraction extraction) {
        return !computeMissingFields(extraction).isEmpty();
    }

    /**
     * Authoritative list of missing fields exposed to the frontend.
     *
     * Combines:
     *   - what the LLM flagged in "missing_fields"
     *   - what the server detects on its own for CREATE (so a sloppy LLM that
     *     forgets to flag the type can't slip a half-empty event through).
     *
     * The frontend uses this list to drive its UX (which input to focus,
     * which question to render).
     */
    public List<String> computeMissingFields(final EventExtraction extraction) {
        List<String> missing = new ArrayList<>(extraction.getMissingFields());
        if (extraction.getCategory() == Categories.CREATE) {
            EventExtraction.EventData v = extraction.getNewValue();
            addIfMissing(missing, "date", v.getDate() == null);
            addIfMissing(missing, "begin_time", v.getBeginTime() == null);
            addIfMissing(missing, "end_time", v.getEndTime() == null);
            addIfMissing(missing, "title", v.getTitle() == null);
            addIfMissing(missing, "type", v.getType() == null);
        }
        return missing;
    }

    private void addIfMissing(final List<String> missing, final String field, final boolean condition) {
        if (condition && !missing.contains(field)) {
            missing.add(field);
        }
    }

    private List<LlmClient.ChatMessage> readHistory(final Conversation conversation) {
        List<LlmClient.ChatMessage> history = new ArrayList<>();
        if (conversation == null || conversation.getHistoryJson() == null) {
            return history;
        }
        JSONArray array = new JSONArray(conversation.getHistoryJson());
        for (int i = 0; i < array.length(); i++) {
            JSONObject item = array.getJSONObject(i);
            history.add(new LlmClient.ChatMessage(item.getString("role"), item.getString("content")));
        }
        return history;
    }

    private String serializeHistory(final List<LlmClient.ChatMessage> history) {
        JSONArray array = new JSONArray();
        for (LlmClient.ChatMessage message : history) {
            array.put(new JSONObject().put("role", message.role()).put("content", message.content()));
        }
        return array.toString();
    }
}
