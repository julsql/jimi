package com.tsp.jimi_api.support;

import com.tsp.jimi_api.entities.CalendarAccount;
import com.tsp.jimi_api.repositories.CalendarAccountRepository;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Map-backed {@link CalendarAccountRepository} for tests. Only the methods the
 * service uses are implemented; the rest throw to surface accidental use.
 */
public class InMemoryCalendarAccountRepository implements CalendarAccountRepository {

    private final Map<Long, CalendarAccount> store = new LinkedHashMap<>();
    private final AtomicLong ids = new AtomicLong(1);

    @Override
    public Optional<CalendarAccount> findByUserIdAndProvider(final String userId, final String provider) {
        return store.values().stream()
                .filter(a -> a.getUserId().equals(userId) && a.getProvider().equals(provider))
                .findFirst();
    }

    @Override
    public List<CalendarAccount> findByUserId(final String userId) {
        return store.values().stream().filter(a -> a.getUserId().equals(userId)).toList();
    }

    @Override
    public boolean existsByUserIdAndProvider(final String userId, final String provider) {
        return findByUserIdAndProvider(userId, provider).isPresent();
    }

    @Override
    public int deleteByUserId(final String userId) {
        int before = store.size();
        store.values().removeIf(a -> a.getUserId().equals(userId));
        return before - store.size();
    }

    @Override
    public <S extends CalendarAccount> S save(final S entity) {
        if (entity.getId() == null) {
            setId(entity, ids.getAndIncrement());
        }
        store.put(entity.getId(), entity);
        return entity;
    }

    @Override
    public void delete(final CalendarAccount entity) {
        store.remove(entity.getId());
    }

    private static void setId(final CalendarAccount entity, final Long id) {
        try {
            var field = CalendarAccount.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(entity, id);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }

    // --- unused CrudRepository surface -------------------------------------

    @Override
    public <S extends CalendarAccount> Iterable<S> saveAll(final Iterable<S> entities) {
        List<S> out = new ArrayList<>();
        entities.forEach(e -> out.add(save(e)));
        return out;
    }

    @Override
    public Optional<CalendarAccount> findById(final Long id) {
        return Optional.ofNullable(store.get(id));
    }

    @Override
    public boolean existsById(final Long id) {
        return store.containsKey(id);
    }

    @Override
    public Iterable<CalendarAccount> findAll() {
        return new ArrayList<>(store.values());
    }

    @Override
    public Iterable<CalendarAccount> findAllById(final Iterable<Long> ids) {
        throw new UnsupportedOperationException();
    }

    @Override
    public long count() {
        return store.size();
    }

    @Override
    public void deleteById(final Long id) {
        store.remove(id);
    }

    @Override
    public void deleteAllById(final Iterable<? extends Long> ids) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void deleteAll(final Iterable<? extends CalendarAccount> entities) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void deleteAll() {
        store.clear();
    }
}
