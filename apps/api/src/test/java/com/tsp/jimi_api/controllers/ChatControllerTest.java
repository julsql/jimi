package com.tsp.jimi_api.controllers;

import com.tsp.jimi_api.enums.ConversationStatus;
import com.tsp.jimi_api.records.ChatApiRequest;
import com.tsp.jimi_api.records.ChatApiResponse;
import com.tsp.jimi_api.services.ChatService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Charge la vraie config MVC + Jackson (application.yml) : verifie que le JSON des
 * apps deja deployees reste accepte apres le passage a Spring Boot 4 / Jackson 3.
 */
@WebMvcTest(ChatController.class)
class ChatControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ChatService chatService;

    @Test
    void chatWithoutCalendarModeDefaultsToFalse() throws Exception {
        when(chatService.handle(any())).thenReturn(
                new ChatApiResponse("c1", ConversationStatus.COMPLETED, "ok", List.of()));

        mockMvc.perform(post("/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userId\":\"u1\",\"message\":\"hello\"}"))
                .andExpect(status().isOk());

        ArgumentCaptor<ChatApiRequest> request = ArgumentCaptor.forClass(ChatApiRequest.class);
        verify(chatService).handle(request.capture());
        assertThat(request.getValue().calendarMode()).isFalse();
        assertThat(request.getValue().timezone()).isNull();
    }

    @Test
    void chatWithCalendarModeIsForwarded() throws Exception {
        when(chatService.handle(any())).thenReturn(
                new ChatApiResponse("c1", ConversationStatus.COMPLETED, "ok", List.of()));

        mockMvc.perform(post("/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userId\":\"u1\",\"message\":\"hello\",\"calendarMode\":true}"))
                .andExpect(status().isOk());

        ArgumentCaptor<ChatApiRequest> request = ArgumentCaptor.forClass(ChatApiRequest.class);
        verify(chatService).handle(request.capture());
        assertThat(request.getValue().calendarMode()).isTrue();
    }
}
