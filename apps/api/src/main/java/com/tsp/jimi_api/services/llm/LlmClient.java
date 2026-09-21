package com.tsp.jimi_api.services.llm;

import java.util.List;

/**
 * Provider-agnostic chat completion client.
 *
 * Implementations: {@link GroqLlmClient} for free Groq inference.
 * To swap providers, register another bean implementing this interface.
 */
public interface LlmClient {

    /**
     * Single message in a chat completion request.
     *
     * @param role    "system", "user" or "assistant"
     * @param content message content
     */
    record ChatMessage(String role, String content) {
    }

    /**
     * Calls the LLM and returns the raw assistant content.
     *
     * @param systemPrompt the system instructions
     * @param history      ordered conversation history (oldest first)
     * @return assistant message content (expected JSON when json mode is on)
     */
    String complete(String systemPrompt, List<ChatMessage> history);
}
