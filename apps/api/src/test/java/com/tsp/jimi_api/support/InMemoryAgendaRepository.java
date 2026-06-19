package com.tsp.jimi_api.support;

import com.tsp.jimi_api.entities.Agenda;
import com.tsp.jimi_api.repositories.AgendaRepository;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Map-backed {@link AgendaRepository} for tests (legacy local-DB provider).
 */
public class InMemoryAgendaRepository implements AgendaRepository {

    private final Map<Long, Agenda> store = new LinkedHashMap<>();
    private final AtomicLong ids = new AtomicLong(1);

    @Override
    public Iterable<Agenda> findByUserId(final String userId) {
        return store.values().stream().filter(a -> userId.equals(a.getUserId())).toList();
    }

    @Override
    public int deleteByUserId(final String userId) {
        int before = store.size();
        store.values().removeIf(a -> userId.equals(a.getUserId()));
        return before - store.size();
    }

    @Override
    public <S extends Agenda> S save(final S entity) {
        if (entity.getId() == null) {
            entity.setId(ids.getAndIncrement());
        }
        store.put(entity.getId(), entity);
        return entity;
    }

    @Override
    public Optional<Agenda> findById(final Long id) {
        return Optional.ofNullable(store.get(id));
    }

    @Override
    public void delete(final Agenda entity) {
        store.remove(entity.getId());
    }

    @Override
    public boolean existsById(final Long id) {
        return store.containsKey(id);
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
    public Iterable<Agenda> findAll() {
        return new ArrayList<>(store.values());
    }

    // --- unused CrudRepository surface -------------------------------------

    @Override
    public <S extends Agenda> Iterable<S> saveAll(final Iterable<S> entities) {
        List<S> out = new ArrayList<>();
        entities.forEach(e -> out.add(save(e)));
        return out;
    }

    @Override
    public Iterable<Agenda> findAllById(final Iterable<Long> ids) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void deleteAllById(final Iterable<? extends Long> ids) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void deleteAll(final Iterable<? extends Agenda> entities) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void deleteAll() {
        store.clear();
    }
}
