package com.tsp.jimi_api.support;

import com.tsp.jimi_api.services.llm.LlmClient;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;

/**
 * {@link LlmClient} that returns pre-scripted JSON replies in order, so tests
 * drive {@code ChatService} deterministically without any network call.
 */
public class FakeLlmClient implements LlmClient {

    private final Deque<String> replies = new ArrayDeque<>();

    public FakeLlmClient enqueue(final String reply) {
        replies.add(reply);
        return this;
    }

    @Override
    public String complete(final String systemPrompt, final List<ChatMessage> history) {
        if (replies.isEmpty()) {
            throw new IllegalStateException("FakeLlmClient: no scripted reply left");
        }
        return replies.poll();
    }
}
