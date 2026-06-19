package com.tsp.jimi_api.services;

import com.tsp.jimi_api.repositories.ConversationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * User-scoped operations — currently a full wipe of the data JIMI holds.
 *
 * <p>Since the pivot to live calendars, JIMI stores no calendar events of its
 * own; the only user data here is in-progress conversation state. (Linked
 * calendar accounts and their OAuth tokens are revoked + deleted separately,
 * added with the OAuth work.)
 */
@Service
public class UserService {

    private final ConversationRepository conversationRepository;

    public UserService(final ConversationRepository conversationRepository) {
        this.conversationRepository = conversationRepository;
    }

    /**
     * Removes every conversation row tied to {@code userId}. Returns the number
     * of rows deleted.
     */
    @Transactional
    public int deleteAllUserData(final String userId) {
        return conversationRepository.deleteByUserId(userId);
    }
}
