package com.lifelane.repository;

import com.lifelane.model.DispatchDecision;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Spring Data JPA repository for {@link DispatchDecision} entities.
 */
@Repository
public interface DispatchDecisionRepository extends JpaRepository<DispatchDecision, Long> {

    /**
     * Finds the dispatch decision for the given emergency request ID.
     *
     * <p>At most one decision exists per request due to the UNIQUE constraint
     * on {@code emergency_request_id}.</p>
     *
     * @param emergencyRequestId the ID of the {@link com.lifelane.model.EmergencyRequest}
     * @return an {@link Optional} containing the decision, or empty if none exists
     */
    Optional<DispatchDecision> findByEmergencyRequest_Id(Long emergencyRequestId);
}
