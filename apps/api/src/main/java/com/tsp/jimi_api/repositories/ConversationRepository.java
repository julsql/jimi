package com.tsp.jimi_api.repositories;

import com.tsp.jimi_api.entities.Conversation;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository for in-progress event-creation conversations.
 */
@Repository
public interface ConversationRepository extends CrudRepository<Conversation, String> {

    Optional<Conversation> findByIdAndUserId(String id, String userId);
}
