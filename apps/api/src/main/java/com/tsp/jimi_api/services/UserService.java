package com.tsp.jimi_api.services;

import com.tsp.jimi_api.repositories.AgendaRepository;
import com.tsp.jimi_api.repositories.AppUserRepository;
import com.tsp.jimi_api.repositories.ChatContextRepository;
import com.tsp.jimi_api.repositories.ConversationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * User-scoped operations — a full wipe of the data JIMI holds.
 *
 * <p>Removes the legacy agenda events, the in-progress conversation state, and
 * revokes + deletes every linked calendar account (so the OAuth tokens can no
 * longer be used).
 */
@Service
public class UserService {

    private final AgendaRepository agendaRepository;
    private final ConversationRepository conversationRepository;
    private final ChatContextRepository chatContextRepository;
    private final AppUserRepository appUserRepository;
    private final CalendarAccountService calendarAccountService;

    public UserService(final AgendaRepository agendaRepository,
                       final ConversationRepository conversationRepository,
                       final ChatContextRepository chatContextRepository,
                       final AppUserRepository appUserRepository,
                       final CalendarAccountService calendarAccountService) {
        this.agendaRepository = agendaRepository;
        this.conversationRepository = conversationRepository;
        this.chatContextRepository = chatContextRepository;
        this.appUserRepository = appUserRepository;
        this.calendarAccountService = calendarAccountService;
    }

    /**
     * Removes everything tied to {@code userId}: agenda events, conversations,
     * conversation memory, the activity record, and the linked calendar accounts
     * (revoking their OAuth tokens). Returns the total rows removed.
     */
    @Transactional
    public int deleteAllUserData(final String userId) {
        int agenda = agendaRepository.deleteByUserId(userId);
        int accounts = calendarAccountService.unlinkAll(userId);
        int conversations = conversationRepository.deleteByUserId(userId);
        int context = chatContextRepository.deleteByUserId(userId);
        int user = appUserRepository.deleteByUserId(userId);
        return agenda + accounts + conversations + context + user;
    }
}
