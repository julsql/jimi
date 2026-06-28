package com.tsp.jimi_api.support;

import com.tsp.jimi_api.entities.Announcement;
import com.tsp.jimi_api.repositories.AnnouncementRepository;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Map-backed {@link AnnouncementRepository} for tests. Mimics the DB by
 * assigning auto-increment ids on save and resolving the derived query by
 * {@code createdAt} desc with the id as a stable tiebreak.
 */
public class InMemoryAnnouncementRepository implements AnnouncementRepository {

    private final Map<Long, Announcement> store = new LinkedHashMap<>();
    private final AtomicLong seq = new AtomicLong(0);

    @Override
    public Optional<Announcement> findFirstByActiveTrueOrderByCreatedAtDesc() {
        return store.values().stream()
                .filter(Announcement::isActive)
                .max(Comparator.comparing(Announcement::getCreatedAt)
                        .thenComparing(Announcement::getId));
    }

    @Override
    public <S extends Announcement> S save(final S entity) {
        if (entity.getId() == null) {
            entity.setId(seq.incrementAndGet());
        }
        store.put(entity.getId(), entity);
        return entity;
    }

    @Override
    public Optional<Announcement> findById(final Long id) {
        return Optional.ofNullable(store.get(id));
    }

    @Override
    public boolean existsById(final Long id) {
        return store.containsKey(id);
    }

    @Override
    public Iterable<Announcement> findAll() {
        return new ArrayList<>(store.values());
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
    public void delete(final Announcement entity) {
        store.remove(entity.getId());
    }

    @Override
    public void deleteAll() {
        store.clear();
    }

    @Override
    public <S extends Announcement> Iterable<S> saveAll(final Iterable<S> entities) {
        List<S> out = new ArrayList<>();
        entities.forEach(e -> out.add(save(e)));
        return out;
    }

    @Override
    public Iterable<Announcement> findAllById(final Iterable<Long> ids) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void deleteAllById(final Iterable<? extends Long> ids) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void deleteAll(final Iterable<? extends Announcement> entities) {
        throw new UnsupportedOperationException();
    }
}
