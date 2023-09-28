package com.tsp.jimi_api.repositories;

import com.tsp.jimi_api.entities.Agenda;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.data.relational.core.mapping.Table;

/**
 * The interface Elevator repository.
 */
@Table("Agenda")
@Repository
public interface AgendaRepository extends CrudRepository<Agenda, Long> {
    /**
     * Find by title elevator.
     *
     * @param userId the user id
     * @return the agenda
     */
    @Query(value = "SELECT a FROM Agenda as a WHERE a.userId = :userId order by a.date")
    Iterable<Agenda> findByUserId(@Param("userId") String userId);
}
