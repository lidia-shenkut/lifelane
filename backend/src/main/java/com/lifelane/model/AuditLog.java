package com.lifelane.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.OffsetDateTime;

/**
 * JPA entity for the {@code audit_logs} table.
 *
 * <p>Every recommendation generation and dispatch decision is recorded here
 * as a tamper-evident audit trail. Foreign keys are stored as raw IDs (not
 * as {@code @ManyToOne} associations) to keep the audit table independent of
 * referential integrity — logs must survive even if related records are later
 * modified.</p>
 *
 * <p>Valid {@code eventType} values: {@code RECOMMENDATION}, {@code DECISION}.</p>
 */
@Entity
@Table(name = "audit_logs")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "event_type", nullable = false, length = 30)
    private String eventType;

    @Column(name = "emergency_request_id", nullable = false)
    private Long emergencyRequestId;

    /** Nullable — present for RECOMMENDATION events, absent for DECISION-only entries. */
    @Column(name = "recommendation_id")
    private Long recommendationId;

    /** Nullable — present for DECISION events. */
    @Column(name = "decision_id")
    private Long decisionId;

    /** Full serialized payload stored as JSONB. */
    @Column(name = "payload_json", nullable = false, columnDefinition = "jsonb")
    private String payloadJson;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false,
            columnDefinition = "TIMESTAMPTZ")
    private OffsetDateTime createdAt;
}
