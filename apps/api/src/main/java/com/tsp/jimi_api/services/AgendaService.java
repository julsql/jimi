package com.tsp.jimi_api.services;

import com.tsp.jimi_api.entities.Agenda;
import com.tsp.jimi_api.records.EventExtraction;
import com.tsp.jimi_api.records.EventExtraction.EventData;
import com.tsp.jimi_api.repositories.AgendaRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.function.Predicate;
import java.util.stream.StreamSupport;

/**
 * CRUD operations on {@link Agenda} entities driven by extracted LLM data.
 *
 * Holds the business logic that used to live in ChatController#createEvent /
 * editEvent / deleteEvent / filter. Pure DB operations - no LLM calls.
 */
@Service
public class AgendaService {

    private final AgendaRepository agendaRepository;

    public AgendaService(final AgendaRepository agendaRepository) {
        this.agendaRepository = agendaRepository;
    }

    public Agenda create(final EventExtraction info, final String userId) {
        EventData data = info.getNewValue();
        Agenda event = new Agenda(
                data.getDate(),
                data.getBeginTime(),
                data.getEndTime(),
                data.getType(),
                data.getTitle(),
                userId);
        return agendaRepository.save(event);
    }

    public List<Agenda> edit(final EventExtraction info, final String userId) {
        List<Agenda> matches = filter(info, userId);
        EventData newValue = info.getNewValue();
        for (Agenda event : matches) {
            if (newValue.getTitle() != null) {
                event.setTitle(newValue.getTitle());
            }
            if (newValue.getType() != null) {
                event.setType(newValue.getType());
            }
            if (newValue.getEndTime() != null) {
                event.setEnd(newValue.getEndTime());
            }
            if (newValue.getBeginTime() != null) {
                event.setBegin(newValue.getBeginTime());
            }
            if (newValue.getDate() != null) {
                event.setDate(newValue.getDate());
            }
            agendaRepository.save(event);
        }
        return matches;
    }

    public List<Agenda> delete(final EventExtraction info, final String userId) {
        List<Agenda> matches = filter(info, userId);
        agendaRepository.deleteAll(matches);
        return matches;
    }

    public Iterable<Agenda> findByUserId(final String userId) {
        return agendaRepository.findByUserId(userId);
    }

    public boolean hasAnyEvent(final String userId) {
        return agendaRepository.findByUserId(userId).iterator().hasNext();
    }

    public String renderAgenda(final String userId) {
        Iterable<Agenda> events = agendaRepository.findByUserId(userId);
        StringBuilder builder = new StringBuilder("My agenda is: ");
        for (Agenda event : events) {
            builder.append("On ").append(event.getDate())
                    .append(" I have \"").append(event.getTitle()).append("\"")
                    .append(" from ").append(event.getBegin())
                    .append(" to ").append(event.getEnd())
                    .append(", it is a ").append(event.getType()).append(" event. ");
        }
        return builder.toString();
    }

    private List<Agenda> filter(final EventExtraction info, final String userId) {
        Iterable<Agenda> events = agendaRepository.findByUserId(userId);
        EventData oldValue = info.getOldValue();

        Predicate<Agenda> matchesTitle = a -> oldValue.getTitle() == null || oldValue.getTitle().equals(a.getTitle());
        Predicate<Agenda> matchesDate = a -> oldValue.getDate() == null || oldValue.getDate().equals(a.getDate());
        Predicate<Agenda> matchesBegin = a -> oldValue.getBeginTime() == null || oldValue.getBeginTime().equals(a.getBegin());
        Predicate<Agenda> matchesEnd = a -> oldValue.getEndTime() == null || oldValue.getEndTime().equals(a.getEnd());
        Predicate<Agenda> matchesType = a -> oldValue.getType() == null || oldValue.getType().equals(a.getType());

        return StreamSupport.stream(events.spliterator(), false)
                .filter(matchesTitle)
                .filter(matchesDate)
                .filter(matchesBegin)
                .filter(matchesEnd)
                .filter(matchesType)
                .toList();
    }
}
