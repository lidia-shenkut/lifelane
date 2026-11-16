package com.lifelane.service;

import com.lifelane.dto.response.RecommendationResponseDto;

/**
 * Core recommendation engine that produces the optimal ambulance/hospital
 * dispatch pair for an emergency request.
 *
 * <p>Implemented by {@code RecommendationEngineImpl} in Phase 7.
 * A stub placeholder is registered as a Spring bean until then so that
 * {@link com.lifelane.controller.EmergencyController} can be loaded by Spring.</p>
 */
public interface RecommendationEngine {

    /**
     * Computes a dispatch recommendation for the given emergency request.
     *
     * @param emergencyRequestId ID of an existing {@link com.lifelane.model.EmergencyRequest}
     * @return the recommendation response DTO
     * @throws java.util.NoSuchElementException                      if the request is not found
     * @throws com.lifelane.exception.NoEligibleAmbulanceException   if no ambulances pass filtering
     * @throws com.lifelane.exception.NoEligibleHospitalException    if no hospitals pass filtering
     * @throws com.lifelane.exception.RoutingServiceException        if OSRM is unreachable
     */
    RecommendationResponseDto recommend(Long emergencyRequestId);
}
