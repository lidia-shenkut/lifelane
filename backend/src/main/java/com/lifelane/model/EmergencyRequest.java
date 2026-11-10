package com.lifelane.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.OffsetDateTime;

/**
 * JPA entity for the {@code emergency_requests} table.
 *
 * <p>Valid {@code urgencyLevel} values: {@code low}, {@code medium},
 * {@code high}, {@code critical}.</p>
 *
 * <p>Valid {@code status} values: {@code open}, {@code dispatched},
 * {@code closed}.</p>
 */
@Entity
@Table(name = "emergency_requests")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmergencyRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "latitude", nullable = false)
    private Double latitude;

    @Column(name = "longitude", nullable = false)
    private Double longitude;

    @Column(name = "urgency_level", nullable = false, length = 20)
    private String urgencyLevel;

    @Column(name = "required_care_type", nullable = false, length = 100)
    private String requiredCareType;

    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private String status = "open";

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false,
            columnDefinition = "TIMESTAMPTZ")
    private OffsetDateTime createdAt;
}
