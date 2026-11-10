package com.lifelane.repository;

import com.lifelane.model.EmergencyRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for {@link EmergencyRequest} entities.
 *
 * <p>Inherits standard CRUD operations from {@link JpaRepository}.</p>
 */
@Repository
public interface EmergencyRequestRepository extends JpaRepository<EmergencyRequest, Long> {
}
