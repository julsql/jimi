package com.tsp.jimi_api.services;

import com.tsp.jimi_api.repositories.AgendaRepository;
import com.tsp.jimi_api.repositories.ConversationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * User-scoped operations that span more than one table — currently a full
 * data wipe (agenda events + in-progress conversations).
 */
@Service
public class UserService {

    private final AgendaRepository agendaRepository;
    private final ConversationRepository conversationRepository;

    public UserService(final AgendaRepository agendaRepository,
                       final ConversationRepository conversationRepository) {
        this.agendaRepository = agendaRepository;
        this.conversationRepository = conversationRepository;
    }

    /**
     * Removes every row tied to {@code userId}. Returns the total number of
     * rows deleted across all tables.
     */
    @Transactional
    public int deleteAllUserData(final String userId) {
        int agendaDeleted = agendaRepository.deleteByUserId(userId);
        int conversationsDeleted = conversationRepository.deleteByUserId(userId);
        return agendaDeleted + conversationsDeleted;
    }
}
