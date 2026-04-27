package com.tsp.jimi_api.configurations;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Strongly-typed config for the chosen LLM provider.
 *
 * Bound from "llm.*" in application.yml. Defaults target Mistral AI's free
 * tier; the underlying client only needs an OpenAI-compatible endpoint, so
 * switching to Groq, OpenRouter, Ollama, etc. is just a matter of changing
 * url/model/apiKey.
 */
@Configuration
@ConfigurationProperties(prefix = "llm")
public class LlmProperties {

    private String url = "https://api.mistral.ai/v1/chat/completions";
    private String model = "mistral-small-latest";
    private String apiKey = "";
    private double temperature = 0.2;
    private boolean jsonMode = true;

    public String getUrl() {
        return url;
    }

    public void setUrl(final String url) {
        this.url = url;
    }

    public String getModel() {
        return model;
    }

    public void setModel(final String model) {
        this.model = model;
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(final String apiKey) {
        this.apiKey = apiKey;
    }

    public double getTemperature() {
        return temperature;
    }

    public void setTemperature(final double temperature) {
        this.temperature = temperature;
    }

    public boolean isJsonMode() {
        return jsonMode;
    }

    public void setJsonMode(final boolean jsonMode) {
        this.jsonMode = jsonMode;
    }
}
