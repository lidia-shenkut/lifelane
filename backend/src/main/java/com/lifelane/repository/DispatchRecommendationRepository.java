package com.lifelane.repository;

import com.lifelane.model.DispatchRecommendation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Spring Data JPA repository for {@link DispatchRecommendation} entities.
 */
@Repository
public interface DispatchRecommendationRepository extends JpaRepository<DispatchRecommendation, Long> {

    /**
     * Finds all recommendations associated with the given emergency request ID.
     *
     * @param emergencyRequestId the ID of the {@link com.lifelane.model.EmergencyRequest}
     * @return list of dispatch recommendations for that request
     */
    List<DispatchRecommendation> findByEmergencyRequest_Id(Long emergencyRequestId);
}
