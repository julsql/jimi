package com.tsp.jimi_api.services;

import com.tsp.jimi_api.entities.ChatContext;
import com.tsp.jimi_api.entities.Conversation;
import com.tsp.jimi_api.enums.Categories;
import com.tsp.jimi_api.enums.ConversationStatus;
import com.tsp.jimi_api.records.EventDraft;
import com.tsp.jimi_api.records.EventExtraction;
import com.tsp.jimi_api.records.ProposedAction;
import com.tsp.jimi_api.repositories.ChatContextRepository;
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
 * Manages multi-turn state that spans more than one HTTP exchange:
 *   - incomplete CREATE drafts (waiting for missing fields), and
 *   - destructive actions (EDIT/DELETE) waiting for the user to confirm.
 *
 * State is keyed by a UUID handed back to the frontend. On the next call the
 * frontend echoes that UUID; we re-load the history/draft to resume. No
 * calendar content is ever stored here — only the user's own message history
 * and the parameters of the action they are mid-way through.
 */
@Service
public class ConversationService {

    /** How many recent messages of rolling context to keep/replay per user. */
    private static final int CONTEXT_WINDOW = 10;

    private final ConversationRepository conversationRepository;
    private final ChatContextRepository chatContextRepository;

    public ConversationService(final ConversationRepository conversationRepository,
                               final ChatContextRepository chatContextRepository) {
        this.conversationRepository = conversationRepository;
        this.chatContextRepository = chatContextRepository;
    }

    /**
     * The user's rolling conversation memory (recent turns), so the LLM keeps
     * the thread across messages. Returns a mutable, possibly empty list.
     */
    public List<LlmClient.ChatMessage> loadContext(final String userId) {
        return chatContextRepository.findById(userId)
                .map(c -> deserializeHistory(c.getHistoryJson()))
                .orElseGet(ArrayList::new);
    }

    /**
     * Appends the latest (user, assistant) exchange to the user's rolling memory,
     * trimmed to the most recent {@link #CONTEXT_WINDOW} messages.
     */
    public void recordContext(final String userId, final String userMessage,
                              final String assistantMessage) {
        List<LlmClient.ChatMessage> history = loadContext(userId);
        history.add(new LlmClient.ChatMessage("user", userMessage));
        history.add(new LlmClient.ChatMessage("assistant", assistantMessage));
        if (history.size() > CONTEXT_WINDOW) {
            history = new ArrayList<>(history.subList(history.size() - CONTEXT_WINDOW, history.size()));
        }
        ChatContext context = chatContextRepository.findById(userId).orElseGet(() -> new ChatContext(userId));
        context.setHistoryJson(serializeHistory(history));
        chatContextRepository.save(context);
    }

    /**
     * Loads an in-progress (AWAITING_INFO) draft for a user, or empty.
     */
    public Optional<Conversation> load(final String conversationId, final String userId) {
        return loadAny(conversationId, userId)
                .filter(c -> c.getStatus() == ConversationStatus.AWAITING_INFO);
    }

    /**
     * Loads a pending confirmation (AWAITING_CONFIRMATION) for a user, or empty.
     */
    public Optional<Conversation> loadPending(final String conversationId, final String userId) {
        return loadAny(conversationId, userId)
                .filter(c -> c.getStatus() == ConversationStatus.AWAITING_CONFIRMATION);
    }

    private Optional<Conversation> loadAny(final String conversationId, final String userId) {
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
     * Persists an incomplete CREATE draft. Creates a new row when conversation
     * is null, otherwise updates the existing one.
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
     * Persists a destructive action awaiting explicit confirmation. The exact
     * target event ids live inside {@code action} so execution can act on them
     * without ever re-consulting the LLM.
     */
    public Conversation persistPendingAction(
            final Conversation existing,
            final String userId,
            final ProposedAction action,
            final List<LlmClient.ChatMessage> history) {

        Conversation conversation = existing != null
                ? existing
                : new Conversation(UUID.randomUUID().toString(), userId);

        conversation.setCategory(action.category());
        conversation.setStatus(ConversationStatus.AWAITING_CONFIRMATION);
        conversation.setDraftJson(action.toJson().toString());
        conversation.setHistoryJson(serializeHistory(history));

        return conversationRepository.save(conversation);
    }

    /**
     * Marks a conversation as completed once its action has succeeded, so the
     * frontend drops its conversationId.
     */
    public void complete(final Conversation conversation) {
        setStatus(conversation, ConversationStatus.COMPLETED);
    }

    /**
     * Marks a pending action as declined by the user; nothing was executed.
     */
    public void cancel(final Conversation conversation) {
        setStatus(conversation, ConversationStatus.CANCELLED);
    }

    private void setStatus(final Conversation conversation, final ConversationStatus status) {
        if (conversation == null) {
            return;
        }
        conversation.setStatus(status);
        conversationRepository.save(conversation);
    }

    /**
     * Authoritative list of missing fields for a CREATE, combining the LLM's
     * own "missing_fields" with a server-side check of the two hard
     * requirements (title, start) so a sloppy LLM can't slip a half-empty
     * event through.
     */
    public List<String> computeMissingFields(final EventExtraction extraction) {
        List<String> missing = new ArrayList<>(extraction.getMissingFields());
        if (extraction.getCategory() == Categories.CREATE) {
            EventDraft v = extraction.getNewValue();
            addIfMissing(missing, "title", isBlank(v.title()));
            addIfMissing(missing, "start", isBlank(v.start()));
        }
        return missing;
    }

    private void addIfMissing(final List<String> missing, final String field, final boolean condition) {
        if (condition && !missing.contains(field)) {
            missing.add(field);
        }
    }

    private static boolean isBlank(final String s) {
        return s == null || s.isBlank();
    }

    private List<LlmClient.ChatMessage> readHistory(final Conversation conversation) {
        if (conversation == null) {
            return new ArrayList<>();
        }
        return deserializeHistory(conversation.getHistoryJson());
    }

    private List<LlmClient.ChatMessage> deserializeHistory(final String historyJson) {
        List<LlmClient.ChatMessage> history = new ArrayList<>();
        if (historyJson == null || historyJson.isBlank()) {
            return history;
        }
        JSONArray array = new JSONArray(historyJson);
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
