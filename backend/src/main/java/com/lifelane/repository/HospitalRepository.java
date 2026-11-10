package com.lifelane.repository;

import com.lifelane.model.Hospital;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Spring Data JPA repository for {@link Hospital} entities.
 */
@Repository
public interface HospitalRepository extends JpaRepository<Hospital, Long> {

    /**
     * Finds all hospitals where {@code available} is {@code true}.
     *
     * @return list of available hospitals
     */
    List<Hospital> findByAvailableTrue();
}
