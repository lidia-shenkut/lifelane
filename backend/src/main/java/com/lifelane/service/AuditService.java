package com.lifelane.service;

import com.lifelane.model.DispatchDecision;
import com.lifelane.model.DispatchRecommendation;

/**
 * Service interface for persisting audit trail entries.
 *
 * <p>Implemented in Phase 4 (task 4.6). A concrete bean must be registered
 * for {@link com.lifelane.controller.DecisionController} to start.</p>
 */
public interface AuditService {

    /**
     * Persists an audit log entry for a generated recommendation.
     *
     * @param rec       the saved recommendation entity
     * @param requestId the ID of the associated emergency request
     * @throws org.springframework.dao.DataAccessException on DB failure (logged at ERROR; rethrown as HTTP 500)
     */
    void recordRecommendation(DispatchRecommendation rec, Long requestId);

    /**
     * Persists an audit log entry for a dispatcher accept/override decision.
     *
     * @param decision the saved decision entity
     * @throws org.springframework.dao.DataAccessException on DB failure (logged at ERROR; rethrown as HTTP 500)
     */
    void recordDecision(DispatchDecision decision);
}
