package com.lifelane.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.OffsetDateTime;


@Entity
@Table(name = "dispatch_decisions")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DispatchDecision {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "emergency_request_id", nullable = false, unique = true)
    private EmergencyRequest emergencyRequest;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recommendation_id", nullable = false)
    private DispatchRecommendation recommendation;

    @Column(name = "action", nullable = false, length = 10)
    private String action;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chosen_ambulance_id", nullable = false)
    private Ambulance chosenAmbulance;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chosen_hospital_id", nullable = false)
    private Hospital chosenHospital;

    /** Optional free-text comment from the dispatcher. */
    @Column(name = "dispatcher_comment", columnDefinition = "TEXT")
    private String dispatcherComment;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false,
            columnDefinition = "TIMESTAMPTZ")
    private OffsetDateTime createdAt;
}
