package com.lifelane.repository;

import com.lifelane.model.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for {@link AuditLog} entities.
 *
 * <p>Inherits standard CRUD operations from {@link JpaRepository}.</p>
 */
@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
}
