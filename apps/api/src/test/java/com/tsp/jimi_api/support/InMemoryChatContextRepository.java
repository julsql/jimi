package com.tsp.jimi_api.support;

import com.tsp.jimi_api.entities.ChatContext;
import com.tsp.jimi_api.repositories.ChatContextRepository;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Map-backed {@link ChatContextRepository} for tests (rolling per-user memory).
 */
public class InMemoryChatContextRepository implements ChatContextRepository {

    private final Map<String, ChatContext> store = new LinkedHashMap<>();

    @Override
    public int deleteByUserId(final String userId) {
        return store.remove(userId) != null ? 1 : 0;
    }

    @Override
    public <S extends ChatContext> S save(final S entity) {
        store.put(entity.getUserId(), entity);
        return entity;
    }

    @Override
    public Optional<ChatContext> findById(final String userId) {
        return Optional.ofNullable(store.get(userId));
    }

    @Override
    public boolean existsById(final String userId) {
        return store.containsKey(userId);
    }

    @Override
    public long count() {
        return store.size();
    }

    @Override
    public void deleteById(final String userId) {
        store.remove(userId);
    }

    @Override
    public void delete(final ChatContext entity) {
        store.remove(entity.getUserId());
    }

    @Override
    public Iterable<ChatContext> findAll() {
        return new ArrayList<>(store.values());
    }

    @Override
    public <S extends ChatContext> Iterable<S> saveAll(final Iterable<S> entities) {
        java.util.List<S> out = new ArrayList<>();
        entities.forEach(e -> out.add(save(e)));
        return out;
    }

    @Override
    public Iterable<ChatContext> findAllById(final Iterable<String> ids) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void deleteAllById(final Iterable<? extends String> ids) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void deleteAll(final Iterable<? extends ChatContext> entities) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void deleteAll() {
        store.clear();
    }
}
