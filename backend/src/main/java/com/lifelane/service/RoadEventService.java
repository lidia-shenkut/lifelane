package com.lifelane.service;

import com.lifelane.model.RoadEvent;

import java.util.List;

/**
 * Service interface for road-event simulation management.
 *
 * <p>Implemented in Phase 4 (task 4.5). A concrete bean must be registered
 * for {@link com.lifelane.controller.RoadEventController} to start.</p>
 */
public interface RoadEventService {

    /**
     * Updates the status of a road event.
     *
     * @param id     the road event ID
     * @param status the new status string: {@code "active"} or {@code "inactive"}
     * @return the updated {@link RoadEvent} entity
     * @throws java.util.NoSuchElementException if the road event is not found
     */
    RoadEvent setStatus(Long id, String status);

    /**
     * Returns all road events whose status is {@code "active"}.
     */
    List<RoadEvent> findActive();
}
