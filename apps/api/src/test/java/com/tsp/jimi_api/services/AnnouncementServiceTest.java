package com.tsp.jimi_api.services;

import com.tsp.jimi_api.entities.Announcement;
import com.tsp.jimi_api.enums.AnnouncementLevel;
import com.tsp.jimi_api.support.InMemoryAnnouncementRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AnnouncementServiceTest {

    private AnnouncementService service;

    @BeforeEach
    void setUp() {
        service = new AnnouncementService(new InMemoryAnnouncementRepository());
    }

    @Test
    void createReturnsAnActiveAnnouncementWithAnId() {
        Announcement created = service.create("Service back up", AnnouncementLevel.INFO);

        assertThat(created.getId()).isNotNull();
        assertThat(created.isActive()).isTrue();
        assertThat(created.getMessage()).isEqualTo("Service back up");
        assertThat(created.getLevel()).isEqualTo(AnnouncementLevel.INFO);
        assertThat(created.getCreatedAt()).isNotNull();
    }

    @Test
    void getLatestActiveReturnsEmptyWhenNoneExist() {
        assertThat(service.getLatestActive()).isEmpty();
    }

    @Test
    void getLatestActiveReturnsTheMostRecentlyCreated() {
        service.create("Old notice", AnnouncementLevel.INFO);
        Announcement latest = service.create("New notice", AnnouncementLevel.WARNING);

        Optional<Announcement> result = service.getLatestActive();

        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo(latest.getId());
        assertThat(result.get().getMessage()).isEqualTo("New notice");
    }

    @Test
    void deactivateRemovesItFromTheLatestActive() {
        Announcement created = service.create("Maintenance tonight", AnnouncementLevel.CRITICAL);

        boolean deactivated = service.deactivate(created.getId());

        assertThat(deactivated).isTrue();
        assertThat(service.getLatestActive()).isEmpty();
    }

    @Test
    void deactivateUnknownIdReturnsFalse() {
        assertThat(service.deactivate(999L)).isFalse();
    }

    @Test
    void deactivatingTheLatestFallsBackToThePreviousActiveOne() {
        Announcement first = service.create("First", AnnouncementLevel.INFO);
        Announcement second = service.create("Second", AnnouncementLevel.INFO);

        service.deactivate(second.getId());

        assertThat(service.getLatestActive()).isPresent();
        assertThat(service.getLatestActive().get().getId()).isEqualTo(first.getId());
    }

    @Test
    void createRejectsBlankMessage() {
        assertThatThrownBy(() -> service.create("   ", AnnouncementLevel.INFO))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void createDefaultsToInfoWhenLevelMissing() {
        Announcement created = service.create("No level", null);
        assertThat(created.getLevel()).isEqualTo(AnnouncementLevel.INFO);
    }
}
