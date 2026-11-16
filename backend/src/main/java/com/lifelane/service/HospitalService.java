package com.lifelane.service;

import com.lifelane.dto.response.HospitalDto;
import com.lifelane.model.Hospital;

import java.util.List;

/**
 * Service interface for hospital data operations.
 *
 * <p>Implemented in Phase 4 (task 4.3). A concrete bean must be registered
 * for {@link com.lifelane.controller.HospitalController} to start.</p>
 */
public interface HospitalService {

    /** Returns all hospitals as entity objects. */
    List<Hospital> findAll();

    /**
     * Returns all hospitals that are available and whose capabilities list
     * contains {@code careType}.
     */
    List<Hospital> findEligible(String careType);

    /**
     * Returns all hospitals mapped to response DTOs suitable for the
     * {@code GET /api/hospitals} endpoint.
     */
    List<HospitalDto> findAllDtos();
}
