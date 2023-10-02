
package com.tsp.jimi_api.controllers;

import com.tsp.jimi_api.entities.Agenda;
import com.tsp.jimi_api.enums.Categories;
import com.tsp.jimi_api.global.Shared;
import com.tsp.jimi_api.records.*;


import com.tsp.jimi_api.repositories.AgendaRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.StreamSupport;

import static com.tsp.jimi_api.global.Shared.INDENT;
import static com.tsp.jimi_api.global.Shared.CONTEXT;
import static com.tsp.jimi_api.global.Shared.CONTEXT_GET;

/**
 * The type Chat controller.
 */
@RestController
public class ChatController {

    /**
     * The Agenda repository.
     */
    private final AgendaRepository agendaRepository;

    /**
     * The Logger.
     */
    private final Logger logger = LoggerFactory.getLogger(ChatController.class);

    /**
     * The Openai api key.
     */
    @Value("${openai.api.key}")
    private String openaiApiKey;

    /**
     * The Model.
     */
    @Value("${openai.model}")
    private String model;

    /**
     * The Api url.
     */
    @Value("${openai.api.url}")
    private String apiUrl;

    /**
     * Instantiates a new Chat controller.
     *
     * @param agendaRepository The agenda repository
     */
    public ChatController(final AgendaRepository agendaRepository) {
        this.agendaRepository = agendaRepository;
    }

    /**
     * Chat request to comunicate with the chatbot JIMI.
     *
     * @param userMessages the messages sent by the user
     * @return 200 OK.
     */
    @Operation(summary = "Send a message to jimi and Get the response.")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "Conversation sucessfully made", content = {
            @Content(mediaType = "application/json", schema = @Schema(implementation = UserAnswer.class))}),
            @ApiResponse(responseCode = "400", description = "Communication failed", content = {
                    @Content(mediaType = "application/json", examples = {
                            @ExampleObject(name = "User id error", description = "Missing or incorrect type of provided user id", value = """
                                    {
                                        "error": "Communication with Open AI failed.",
                                        "reason": "Incorrect or missing user id."
                                    }
                                    """),
                            })})})
    @PostMapping("/chat")
    @ResponseStatus(HttpStatus.OK)
    public ResponseEntity<String> chat(@RequestBody final UserMessages userMessages) {
        String error = "Communication with Open AI failed.";
        String reason = "";

        if (userMessages.userId() == null) {
            reason = "Incorrect or missing user id.";
        } else if (userMessages.userMessages() == null) {
            reason = "Incorrect or missing user messages.";
        }
        if (!reason.isEmpty()) {
            return Shared.raiseError(error, reason, logger);
        }

        String responseJson = askOpenAi(userMessages.userMessages(), CONTEXT);

        if (responseJson == null) {
            reason = "No response";
            return Shared.raiseError(error, reason, logger);
        }
        ChatResponse response = new ChatResponse(responseJson);

        if (response.getChoices() == null || response.getChoices().isEmpty()) {
            reason = "No response";
            return Shared.raiseError(error, reason, logger);
        }
        System.out.println(response.getChoices().get(0).getMessage().getContent());

        OpenAIEventInfo openAIEventInfo = new OpenAIEventInfo(userMessages.userMessages(), response.getChoices().get(0).getMessage().getContent(), userMessages.userId());

        if (openAIEventInfo.getCategory() == null) {
            openAIEventInfo.setCategory(Categories.OTHER);
        }

        UserAnswer answer = performAction(openAIEventInfo, agendaRepository);
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(answer.toJson().toString(INDENT));
    }

    /**
     * Ask open ai string.
     *
     * @param prompts the prompts
     * @param context the context
     * @return the string
     */
    public String askOpenAi(final List<UserMessage> prompts, final String context) {
        try {
            RestTemplate restTemplate = new RestTemplate();
            ChatRequest request = new ChatRequest(model, prompts, context);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + openaiApiKey);

            System.out.println("The Request sent to OpenAI: ");
            System.out.println(request.toJson().toString(INDENT));

            HttpEntity<String> requestEntity = new HttpEntity<>(request.toJson().toString(), headers);

            ResponseEntity<String> responseEntity = restTemplate.postForEntity(apiUrl, requestEntity, String.class);

            return responseEntity.getBody();
        } catch (Exception e) {
            // Handle the exception or log the error message
            e.printStackTrace();
            return "Error: " + e.getMessage();
        }
    }


    /**
     * Perform action string.
     *
     * @param info             the info
     * @param agendaRepository the agenda repository
     * @return the string
     */
    private UserAnswer performAction(final OpenAIEventInfo info, final AgendaRepository agendaRepository) {
        String response;
        response = switch (info.getCategory()) {
            case CREATE -> createEvent(info, agendaRepository);
            case EDIT -> editEvent(info, agendaRepository);
            case DELETE -> deleteEvent(info, agendaRepository);
            case GET -> getEvent(info, agendaRepository);
            case OTHER -> other(info);
        };
        return new UserAnswer(response);
    }

    /**
     * Create event string.
     *
     * @param info             the info
     * @param agendaRepository the agenda repository
     * @return the string
     */
    private String createEvent(final OpenAIEventInfo info, final AgendaRepository agendaRepository) {

        Agenda event = new Agenda(info.getNewValue().getDate(), info.getNewValue().getBeginTime(),
                info.getNewValue().getEndTime(), info.getNewValue().getType(),
                info.getNewValue().getTitle(), info.getUserId());

        agendaRepository.save(event);
        return info.getResponse();
    }

    /**
     * Edit event string.
     *
     * @param info             the info
     * @param agendaRepository the agenda repository
     * @return the string
     */
    private String editEvent(final OpenAIEventInfo info, final AgendaRepository agendaRepository) {
        List<Agenda> filteredEvents = filter(info, agendaRepository);

        for (Agenda event: filteredEvents) {
            if (info.getNewValue().getTitle() != null) {
                event.setTitle(info.getNewValue().getTitle());
            }
            if (info.getNewValue().getType() != null) {
                event.setType(info.getNewValue().getType());
            }
            if (info.getNewValue().getEndTime() != null) {
                event.setEnd(info.getNewValue().getEndTime());
            }
            if (info.getNewValue().getBeginTime() != null) {
                event.setBegin(info.getNewValue().getBeginTime());
            }
            if (info.getNewValue().getDate() != null) {
                event.setDate(info.getNewValue().getDate());
            }
            agendaRepository.save(event);
        }
        return info.getResponse();
    }

    /**
     * Delete event string.
     *
     * @param info             the info
     * @param agendaRepository the agenda repository
     * @return the string
     */
    private String deleteEvent(final OpenAIEventInfo info, final AgendaRepository agendaRepository) {
        List<Agenda> filteredEvents = filter(info, agendaRepository);

        agendaRepository.deleteAll(filteredEvents);
        return info.getResponse();
    }

    /**
     * Gets event.
     *
     * @param info             the info
     * @param agendaRepository the agenda repository
     * @return the event
     */
    private String getEvent(final OpenAIEventInfo info, final AgendaRepository agendaRepository) {

        Iterable<Agenda> events = agendaRepository.findByUserId(info.getUserId());
        StringBuilder agendaMessage = new StringBuilder("My agenda is: ");
        for (Agenda event: events) {
            //String eventString = "\"" + event.getTitle() + "\" is a " + event.getType() + " event planned the " + event.getDate() + " from " + event.getBegin() + " to " + event.getEnd();
            String eventString = "On " + event.getDate() + " I have \"" + event.getTitle() + "\"" + " from " + event.getBegin() + " to " + event.getEnd() + ", it is a " + event.getType() + " event";
            agendaMessage.append(eventString).append(". ");
        }

        List<UserMessage> prompts = new ArrayList<>();
        UserMessage agenda = new UserMessage("Agenda", agendaMessage.toString());
        System.out.println(agenda);
        prompts.add(agenda);
        prompts.add(info.getPrompts().get(info.getPrompts().size() - 1));

        String responseJson = askOpenAi(prompts, CONTEXT_GET);

        if (responseJson == null) {
            return "No response";
        }
        ChatResponse response = new ChatResponse(responseJson);

        if (response.getChoices() == null || response.getChoices().isEmpty()) {
            return "No response";
        }
        String answer = response.getChoices().get(0).getMessage().getContent();
        System.out.println(answer);

        JSONObject answerJson = new JSONObject(answer);
        return (String) answerJson.get("answer");
    }

    /**
     * Other string.
     *
     * @param info             the info
     * @return the string
     */
    private String other(final OpenAIEventInfo info) {
        return info.getResponse();
    }

    /**
     * Filter list.
     *
     * @param info             the info
     * @param agendaRepository the agenda repository
     * @return the list
     */
    private static List<Agenda> filter(final OpenAIEventInfo info, final AgendaRepository agendaRepository) {
        Iterable<Agenda> events = agendaRepository.findByUserId(info.getUserId());

        Predicate<Agenda> titlePredicate = agenda -> info.getOldValue().getTitle() == null || info.getOldValue().getTitle().equals(agenda.getTitle());
        Predicate<Agenda> datePredicate = agenda -> info.getOldValue().getDate() == null || info.getOldValue().getDate().equals(agenda.getDate());
        Predicate<Agenda> beginTimePredicate = agenda -> info.getOldValue().getBeginTime() == null || info.getOldValue().getBeginTime().equals(agenda.getBegin());
        Predicate<Agenda> endTimePredicate = agenda -> info.getOldValue().getEndTime() == null || info.getOldValue().getEndTime().equals(agenda.getEnd());
        Predicate<Agenda> typePredicate = agenda -> info.getOldValue().getType() == null || info.getOldValue().getType().equals(agenda.getType());

        return StreamSupport.stream(events.spliterator(), false)
                .filter(titlePredicate)
                .filter(datePredicate)
                .filter(beginTimePredicate)
                .filter(endTimePredicate)
                .filter(typePredicate)
                .toList();
    }

}
