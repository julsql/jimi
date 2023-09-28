package com.tsp.jimi_api.records;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * The type Chat response.
 */
public class ChatResponse {

    /**
     * The Choices.
     */
    private List<Choice> choices;

    /**
     * Instantiates a new Chat response.
     *
     * @param choices the choices
     */
    public ChatResponse(final List<Choice> choices) {
        this.choices = choices;
    }

    /**
     * Instantiates a new Chat response.
     *
     * @param jsonString the json string
     */
    public ChatResponse(final String jsonString) {
        JSONObject json = new JSONObject(jsonString);
        JSONArray choicesArray = json.getJSONArray("choices");

        List<ChatResponse.Choice> choices = new ArrayList<>();

        for (int i = 0; i < choicesArray.length(); i++) {
            JSONObject choiceObj = choicesArray.getJSONObject(i);
            int index = choiceObj.getInt("index");
            JSONObject messageObj = choiceObj.getJSONObject("message");
            String role = messageObj.getString("role");
            String content = messageObj.getString("content");

            ChatResponse.Choice choice = new ChatResponse.Choice(index, new Message(role, content));
            choices.add(choice);
        }
        this.choices = choices;
    }

    /**
     * The type Choice.
     */
    public static class Choice {

        /**
         * The Index.
         */
        private int index;
        /**
         * The Message.
         */
        private Message message;

        // constructors, getters and setters

        /**
         * Instantiates a new Choice.
         *
         * @param index   the index
         * @param message the message
         */
        public Choice(final int index, final Message message) {
            this.index = index;
            this.message = message;
        }

        /**
         * Gets index.
         *
         * @return the index
         */
        public int getIndex() {
            return index;
        }

        /**
         * Sets index.
         *
         * @param index the index
         */
        public void setIndex(final int index) {
            this.index = index;
        }

        /**
         * Gets message.
         *
         * @return the message
         */
        public Message getMessage() {
            return message;
        }

        /**
         * Sets message.
         *
         * @param message the message
         */
        public void setMessage(final Message message) {
            this.message = message;
        }


    }

    /**
     * Gets choices.
     *
     * @return the choices
     */
    public List<Choice> getChoices() {
        return choices;
    }

    /**
     * Sets choices.
     *
     * @param choices the choices
     */
    public void setChoices(final List<Choice> choices) {
        this.choices = choices;
    }
}
