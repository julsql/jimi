package com.tsp.jimi_api.support;

import com.tsp.jimi_api.entities.AppUser;
import com.tsp.jimi_api.repositories.AppUserRepository;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Map-backed {@link AppUserRepository} for tests.
 */
public class InMemoryAppUserRepository implements AppUserRepository {

    private final Map<String, AppUser> store = new LinkedHashMap<>();

    @Override
    public List<AppUser> findByLastSeenAtBefore(final Instant threshold) {
        return store.values().stream().filter(u -> u.getLastSeenAt().isBefore(threshold)).toList();
    }

    @Override
    public int deleteByUserId(final String userId) {
        return store.remove(userId) != null ? 1 : 0;
    }

    @Override
    public <S extends AppUser> S save(final S entity) {
        store.put(entity.getUserId(), entity);
        return entity;
    }

    @Override
    public Optional<AppUser> findById(final String userId) {
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
    public void delete(final AppUser entity) {
        store.remove(entity.getUserId());
    }

    @Override
    public Iterable<AppUser> findAll() {
        return new ArrayList<>(store.values());
    }

    @Override
    public <S extends AppUser> Iterable<S> saveAll(final Iterable<S> entities) {
        List<S> out = new ArrayList<>();
        entities.forEach(e -> out.add(save(e)));
        return out;
    }

    @Override
    public Iterable<AppUser> findAllById(final Iterable<String> ids) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void deleteAllById(final Iterable<? extends String> ids) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void deleteAll(final Iterable<? extends AppUser> entities) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void deleteAll() {
        store.clear();
    }
}
