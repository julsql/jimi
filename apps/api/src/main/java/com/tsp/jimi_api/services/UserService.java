package com.tsp.jimi_api.services;

import com.tsp.jimi_api.repositories.ConversationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * User-scoped operations — a full wipe of the data JIMI holds.
 *
 * <p>Since the pivot to live calendars, JIMI stores no calendar events of its
 * own. A wipe removes the in-progress conversation state AND revokes + deletes
 * every linked calendar account (so the OAuth tokens can no longer be used).
 */
@Service
public class UserService {

    private final ConversationRepository conversationRepository;
    private final CalendarAccountService calendarAccountService;

    public UserService(final ConversationRepository conversationRepository,
                       final CalendarAccountService calendarAccountService) {
        this.conversationRepository = conversationRepository;
        this.calendarAccountService = calendarAccountService;
    }

    /**
     * Revokes + removes every linked calendar account and deletes every
     * conversation row tied to {@code userId}. Returns the total rows removed.
     */
    @Transactional
    public int deleteAllUserData(final String userId) {
        int accounts = calendarAccountService.unlinkAll(userId);
        int conversations = conversationRepository.deleteByUserId(userId);
        return accounts + conversations;
    }
}
