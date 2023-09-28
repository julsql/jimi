package com.tsp.jimi_api.records;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * The type Chat request.
 */
public class ChatRequest {

    /**
     * The Model.
     */
    private String model;
    /**
     * The Messages.
     */
    private List<Message> messages;
    /**
     * The N.
     */
    private int n;
    /**
     * The Temperature.
     */
    private double temperature;


    /**
     * Instantiates a new Chat request.
     *
     * @param model   the model
     * @param prompts the prompts
     * @param context the context
     */
    public ChatRequest(final String model, final List<UserMessage> prompts, final String context) {
        this.model = model;

        this.messages = new ArrayList<>();
        this.messages.add(new Message("system", context));
        for (UserMessage prompt : prompts) {
            this.messages.add(new Message("user", prompt.sender(), prompt.message()));
        }
    }

    /**
     * To json json object.
     *
     * @return the json object
     */
    public JSONObject toJson() {
        JSONObject json = new JSONObject();
        json.put("model", model);
        JSONArray messageArray = new JSONArray();
        for (Message message : messages) {
            messageArray.put(message.toJson());
        }
        json.put("messages", messageArray);
        return json;
    }

    // getters and setters


    /**
     * Gets model.
     *
     * @return the model
     */
    public String getModel() {
        return model;
    }

    /**
     * Sets model.
     *
     * @param model the model
     */
    public void setModel(final String model) {
        this.model = model;
    }

    /**
     * Gets messages.
     *
     * @return the messages
     */
    public List<Message> getMessages() {
        return messages;
    }

    /**
     * Sets messages.
     *
     * @param messages the messages
     */
    public void setMessages(final List<Message> messages) {
        this.messages = messages;
    }

    /**
     * Gets n.
     *
     * @return the n
     */
    public int getN() {
        return n;
    }

    /**
     * Sets n.
     *
     * @param n the n
     */
    public void setN(final int n) {
        this.n = n;
    }

    /**
     * Gets temperature.
     *
     * @return the temperature
     */
    public double getTemperature() {
        return temperature;
    }

    /**
     * Sets temperature.
     *
     * @param temperature the temperature
     */
    public void setTemperature(final double temperature) {
        this.temperature = temperature;
    }
}
