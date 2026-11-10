package com.lifelane.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.OffsetDateTime;

/**
 * JPA entity for the {@code dispatch_recommendations} table.
 *
 * <p>Stores the AI-generated recommendation for an emergency request,
 * including the selected ambulance, hospital, ETAs, route geometry,
 * human-readable explanation, and ranked alternatives as JSON.</p>
 */
@Entity
@Table(name = "dispatch_recommendations")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DispatchRecommendation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "emergency_request_id", nullable = false)
    private EmergencyRequest emergencyRequest;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recommended_ambulance_id", nullable = false)
    private Ambulance recommendedAmbulance;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recommended_hospital_id", nullable = false)
    private Hospital recommendedHospital;

    @Column(name = "ambulance_eta_seconds", nullable = false)
    private Integer ambulanceEtaSeconds;

    @Column(name = "hospital_eta_seconds", nullable = false)
    private Integer hospitalEtaSeconds;

    @Column(name = "total_eta_seconds", nullable = false)
    private Integer totalEtaSeconds;

    /** Polyline JSON for the two-leg route. Nullable. */
    @Column(name = "route_geometry", columnDefinition = "TEXT")
    private String routeGeometry;

    @Column(name = "explanation", nullable = false, columnDefinition = "TEXT")
    private String explanation;

    /** JSONB column: ranked list of alternative ambulance/hospital pairs. Nullable. */
    @Column(name = "alternatives_json", columnDefinition = "jsonb")
    private String alternativesJson;

    /** Snapshot label of the road-event scenario at recommendation time. Nullable. */
    @Column(name = "road_event_scenario", length = 100)
    private String roadEventScenario;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false,
            columnDefinition = "TIMESTAMPTZ")
    private OffsetDateTime createdAt;
}
