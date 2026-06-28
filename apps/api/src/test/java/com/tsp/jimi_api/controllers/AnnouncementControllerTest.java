package com.tsp.jimi_api.controllers;

import com.tsp.jimi_api.records.AnnouncementDto;
import com.tsp.jimi_api.records.AnnouncementRequest;
import com.tsp.jimi_api.enums.AnnouncementLevel;
import com.tsp.jimi_api.services.AnnouncementService;
import com.tsp.jimi_api.support.InMemoryAnnouncementRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

class AnnouncementControllerTest {

    private static final String TOKEN = "s3cret";

    private AnnouncementController controller;

    @BeforeEach
    void setUp() {
        controller = new AnnouncementController(
                new AnnouncementService(new InMemoryAnnouncementRepository()), TOKEN);
    }

    @Test
    void latestReturns204WhenEmpty() {
        assertThat(controller.latest().getStatusCode().value()).isEqualTo(204);
    }

    @Test
    void createThenLatestReturnsTheAnnouncement() {
        controller.create(TOKEN, new AnnouncementRequest("Hello", AnnouncementLevel.WARNING));

        ResponseEntity<AnnouncementDto> latest = controller.latest();
        assertThat(latest.getStatusCode().value()).isEqualTo(200);
        assertThat(latest.getBody()).isNotNull();
        assertThat(latest.getBody().message()).isEqualTo("Hello");
        assertThat(latest.getBody().level()).isEqualTo(AnnouncementLevel.WARNING);
    }

    @Test
    void createRejectsWrongToken() {
        ResponseEntity<?> res = controller.create("nope", new AnnouncementRequest("Hi", null));
        assertThat(res.getStatusCode().value()).isEqualTo(403);
        assertThat(controller.latest().getStatusCode().value()).isEqualTo(204);
    }

    @Test
    void createRejectsBlankMessage() {
        ResponseEntity<?> res = controller.create(TOKEN, new AnnouncementRequest("  ", null));
        assertThat(res.getStatusCode().value()).isEqualTo(400);
    }

    @Test
    void deleteRetiresAnnouncement() {
        controller.create(TOKEN, new AnnouncementRequest("Bye soon", AnnouncementLevel.INFO));
        Long id = controller.latest().getBody().id();

        ResponseEntity<?> res = controller.deactivate(TOKEN, id);

        assertThat(res.getStatusCode().value()).isEqualTo(204);
        assertThat(controller.latest().getStatusCode().value()).isEqualTo(204);
    }

    @Test
    void deleteRejectsWrongToken() {
        controller.create(TOKEN, new AnnouncementRequest("Stay", AnnouncementLevel.INFO));
        Long id = controller.latest().getBody().id();

        ResponseEntity<?> res = controller.deactivate("bad", id);

        assertThat(res.getStatusCode().value()).isEqualTo(403);
        assertThat(controller.latest().getStatusCode().value()).isEqualTo(200);
    }

    @Test
    void adminDisabledWhenNoTokenConfigured() {
        AnnouncementController noAuth = new AnnouncementController(
                new AnnouncementService(new InMemoryAnnouncementRepository()), "");

        ResponseEntity<?> res = noAuth.create("", new AnnouncementRequest("X", null));
        assertThat(res.getStatusCode().value()).isEqualTo(403);
    }
}
