package com.lifelane.repository;

import com.lifelane.model.Ambulance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Spring Data JPA repository for {@link Ambulance} entities.
 */
@Repository
public interface AmbulanceRepository extends JpaRepository<Ambulance, Long> {

    /**
     * Finds all ambulances with the given status (e.g., "available", "busy", "out-of-service").
     *
     * @param status the status string to filter by
     * @return list of ambulances matching the given status
     */
    List<Ambulance> findByStatus(String status);
}
