package com.lifelane.repository;

import com.lifelane.model.RoadEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Spring Data JPA repository for {@link RoadEvent} entities.
 */
@Repository
public interface RoadEventRepository extends JpaRepository<RoadEvent, Long> {

    /**
     * Finds all road events with the given status (e.g., "active", "inactive").
     *
     * @param status the status string to filter by
     * @return list of road events matching the given status
     */
    List<RoadEvent> findByStatus(String status);
}
