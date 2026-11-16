package com.lifelane.service;

import com.lifelane.dto.response.AmbulanceDto;
import com.lifelane.model.Ambulance;

import java.util.List;
import java.util.Set;

/**
 * Service interface for ambulance fleet operations.
 *
 * <p>Implemented in Phase 4 (task 4.1). A concrete bean must be registered
 * for {@link com.lifelane.controller.AmbulanceController} to start.</p>
 */
public interface AmbulanceService {

    /** Returns all ambulances as entity objects. */
    List<Ambulance> findAll();

    /**
     * Returns all ambulances whose status is {@code "available"} and whose
     * equipment list is a superset of {@code requiredEquipment}.
     */
    List<Ambulance> findEligible(Set<String> requiredEquipment);

    /**
     * Returns all ambulances mapped to response DTOs suitable for the
     * {@code GET /api/ambulances} endpoint.
     */
    List<AmbulanceDto> findAllDtos();
}
