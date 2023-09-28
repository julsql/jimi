package com.tsp.jimi_api.records;

import org.json.JSONObject;

/**
 * The type Message.
 */
public class Message {

    /**
     * The Role.
     */
    private String role;
    /**
     * The Content.
     */
    private String content;

    // constructor, getters and setters

    /**
     * Instantiates a new Message.
     *
     * @param role    the role
     * @param content the content
     */
    public Message(final String role, final String content) {
        this.role = role;
        this.content = content;
    }

    /**
     * Instantiates a new Message.
     *
     * @param role    the role
     * @param sender  the sender
     * @param content the content
     */
    public Message(final String role, final String sender, final String content) {
        this.role = role;
        this.content = sender + " Prompt: " + content;
    }

    /**
     * To json json object.
     *
     * @return the json object
     */
    public JSONObject toJson() {
        JSONObject json = new JSONObject();
        json.put("role", role);
        json.put("content", content);
        return json;
    }

    /**
     * Gets role.
     *
     * @return the role
     */
    public String getRole() {
        return role;
    }

    /**
     * Gets content.
     *
     * @return the content
     */
    public String getContent() {
        return content;
    }

    /**
     * Sets role.
     *
     * @param role the role
     */
    public void setRole(final String role) {
        this.role = role;
    }

    /**
     * Sets content.
     *
     * @param content the content
     */
    public void setContent(final String content) {
        this.content = content;
    }


}
