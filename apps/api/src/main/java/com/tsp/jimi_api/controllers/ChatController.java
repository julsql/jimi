package com.tsp.jimi_api.controllers;

import com.tsp.jimi_api.global.Shared;
import com.tsp.jimi_api.records.ChatApiRequest;
import com.tsp.jimi_api.records.ChatApiResponse;
import com.tsp.jimi_api.records.ConfirmRequest;
import com.tsp.jimi_api.services.ChatService;
import com.tsp.jimi_api.services.llm.LlmException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * Thin REST entry point for the JIMI chatbot.
 *
 * <p>All business logic lives in {@link ChatService}; this controller only
 * validates the request, calls the service and shapes the HTTP response.
 *
 * <ul>
 *   <li>{@code POST /chat} — natural-language turn (may need info or confirmation).</li>
 *   <li>{@code POST /chat/confirm} — execute or discard a proposed edit/delete.</li>
 * </ul>
 */
@RestController
public class ChatController {

    private static final Logger LOGGER = LoggerFactory.getLogger(ChatController.class);
    private static final String CHAT_ERROR = "Chat request failed.";
    private static final String CONFIRM_ERROR = "Confirmation failed.";

    private final ChatService chatService;

    public ChatController(final ChatService chatService) {
        this.chatService = chatService;
    }

    @Operation(summary = "Send a message to JIMI and get the assistant's response.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Conversation step processed successfully", content = {
                    @Content(mediaType = "application/json", schema = @Schema(implementation = ChatApiResponse.class))}),
            @ApiResponse(responseCode = "400", description = "Invalid request", content = {
                    @Content(mediaType = "application/json", examples = {
                            @ExampleObject(name = "Missing user id", value = """
                                    {
                                        "error": "Chat request failed.",
                                        "reason": "Incorrect or missing user id."
                                    }
                                    """),
                            @ExampleObject(name = "LLM unavailable", value = """
                                    {
                                        "error": "Chat request failed.",
                                        "reason": "LLM call failed: 401 Unauthorized"
                                    }
                                    """)
                    })})})
    @PostMapping(value = "/chat", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> chat(@RequestBody final ChatApiRequest request) {
        if (request == null || request.userId() == null || request.userId().isBlank()) {
            return Shared.raiseError(CHAT_ERROR, "Incorrect or missing user id.", LOGGER);
        }
        if (request.message() == null || request.message().isBlank()) {
            return Shared.raiseError(CHAT_ERROR, "Incorrect or missing message.", LOGGER);
        }

        try {
            ChatApiResponse response = chatService.handle(request);
            return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(response.toJsonString());
        } catch (LlmException e) {
            return Shared.raiseError(CHAT_ERROR, e.getMessage(), LOGGER);
        } catch (Exception e) {
            return Shared.raiseError(CHAT_ERROR, e.getClass().getSimpleName() + ": " + e.getMessage(), LOGGER);
        }
    }

    @Operation(summary = "Confirm or decline a destructive action (edit/delete) JIMI proposed.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Action executed or discarded", content = {
                    @Content(mediaType = "application/json", schema = @Schema(implementation = ChatApiResponse.class))}),
            @ApiResponse(responseCode = "400", description = "Invalid request or no pending action", content = {
                    @Content(mediaType = "application/json")})})
    @PostMapping(value = "/chat/confirm", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> confirm(@RequestBody final ConfirmRequest request) {
        if (request == null || request.userId() == null || request.userId().isBlank()) {
            return Shared.raiseError(CONFIRM_ERROR, "Incorrect or missing user id.", LOGGER);
        }
        if (request.conversationId() == null || request.conversationId().isBlank()) {
            return Shared.raiseError(CONFIRM_ERROR, "Incorrect or missing conversation id.", LOGGER);
        }

        try {
            ChatApiResponse response = chatService.confirm(request);
            return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(response.toJsonString());
        } catch (IllegalStateException e) {
            return Shared.raiseError(CONFIRM_ERROR, e.getMessage(), LOGGER);
        } catch (Exception e) {
            return Shared.raiseError(CONFIRM_ERROR, e.getClass().getSimpleName() + ": " + e.getMessage(), LOGGER);
        }
    }
}
