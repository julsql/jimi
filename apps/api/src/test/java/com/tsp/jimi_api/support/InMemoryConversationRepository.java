package com.tsp.jimi_api.support;

import com.tsp.jimi_api.entities.Conversation;
import com.tsp.jimi_api.repositories.ConversationRepository;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Map-backed {@link ConversationRepository} for tests. Only the methods JIMI
 * actually uses are implemented; the rest throw, to surface accidental use.
 */
public class InMemoryConversationRepository implements ConversationRepository {

    private final Map<String, Conversation> store = new LinkedHashMap<>();

    @Override
    public Optional<Conversation> findByIdAndUserId(final String id, final String userId) {
        return Optional.ofNullable(store.get(id))
                .filter(c -> c.getUserId().equals(userId));
    }

    @Override
    public int deleteByUserId(final String userId) {
        int before = store.size();
        store.values().removeIf(c -> c.getUserId().equals(userId));
        return before - store.size();
    }

    @Override
    public <S extends Conversation> S save(final S entity) {
        // Timestamps are handled by JPA lifecycle callbacks in production; the
        // logic under test doesn't read them, so we just store the entity.
        store.put(entity.getId(), entity);
        return entity;
    }

    @Override
    public Optional<Conversation> findById(final String id) {
        return Optional.ofNullable(store.get(id));
    }

    @Override
    public boolean existsById(final String id) {
        return store.containsKey(id);
    }

    @Override
    public long count() {
        return store.size();
    }

    // --- unused CrudRepository surface -------------------------------------

    @Override
    public <S extends Conversation> Iterable<S> saveAll(final Iterable<S> entities) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Iterable<Conversation> findAll() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Iterable<Conversation> findAllById(final Iterable<String> ids) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void deleteById(final String id) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void delete(final Conversation entity) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void deleteAllById(final Iterable<? extends String> ids) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void deleteAll(final Iterable<? extends Conversation> entities) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void deleteAll() {
        throw new UnsupportedOperationException();
    }
}
