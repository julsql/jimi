package com.tsp.jimi_api.services.llm;

import com.tsp.jimi_api.configurations.LlmProperties;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.List;

/**
 * Calls any OpenAI-compatible /chat/completions endpoint.
 *
 * Defaults target Mistral AI's free tier (see {@link LlmProperties}). The
 * exact same code works against Groq, OpenRouter, Together, Ollama (in v1
 * mode), or OpenAI itself — only url / model / apiKey change.
 *
 * Get a free Mistral key at https://console.mistral.ai (phone verification
 * required, no credit card) and put it in .env as MISTRAL_API_KEY.
 */
@Component
public class OpenAiCompatibleLlmClient implements LlmClient {

    private final LlmProperties properties;
    private final RestTemplate restTemplate;

    public OpenAiCompatibleLlmClient(final LlmProperties properties) {
        this.properties = properties;
        this.restTemplate = new RestTemplate();
    }

    @Override
    public String complete(final String systemPrompt, final List<ChatMessage> history) {
        if (properties.getApiKey() == null || properties.getApiKey().isBlank()) {
            throw new IllegalStateException(
                    "LLM API key is missing. Set MISTRAL_API_KEY (or LLM_API_KEY) in your .env file.");
        }

        JSONObject body = new JSONObject();
        body.put("model", properties.getModel());
        body.put("temperature", properties.getTemperature());
        if (properties.isJsonMode()) {
            body.put("response_format", new JSONObject().put("type", "json_object"));
        }

        JSONArray messages = new JSONArray();
        messages.put(new JSONObject().put("role", "system").put("content", systemPrompt));
        for (ChatMessage message : history) {
            messages.put(new JSONObject().put("role", message.role()).put("content", message.content()));
        }
        body.put("messages", messages);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + properties.getApiKey());

        HttpEntity<String> request = new HttpEntity<>(body.toString(), headers);
        try {
            ResponseEntity<String> response = restTemplate.postForEntity(properties.getUrl(), request, String.class);
            return extractContent(response.getBody());
        } catch (HttpClientErrorException e) {
            throw new LlmException("LLM call failed: " + e.getStatusCode() + " " + e.getResponseBodyAsString(), e);
        }
    }

    private String extractContent(final String responseBody) {
        if (responseBody == null) {
            throw new LlmException("Empty LLM response.");
        }
        JSONObject json = new JSONObject(responseBody);
        JSONArray choices = json.optJSONArray("choices");
        if (choices == null || choices.isEmpty()) {
            throw new LlmException("LLM response had no choices: " + responseBody);
        }
        JSONObject message = choices.getJSONObject(0).getJSONObject("message");
        return message.getString("content");
    }
}
