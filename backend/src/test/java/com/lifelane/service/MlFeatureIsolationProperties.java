package com.lifelane.service;

import com.lifelane.config.MlFeatureProperties;
import com.lifelane.dto.response.RecommendationResponseDto;
import com.lifelane.model.Ambulance;
import com.lifelane.model.DispatchRecommendation;
import com.lifelane.model.EmergencyRequest;
import com.lifelane.model.Hospital;
import com.lifelane.repository.DispatchRecommendationRepository;
import com.lifelane.repository.EmergencyRequestRepository;
import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.ForAll;
import net.jqwik.api.Label;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

/**
 * Property 15: ML predictor is not invoked when feature flag is disabled.
 * Validates: Requirements 14.4
 */
class MlFeatureIsolationProperties {

    @Property(tries = 100)
    @Label("Feature: lifelane-mini, Property 15: ML predictor is not invoked when feature flag is disabled")
    void mlPredictorNotInvokedWhenFlagDisabled(
            @ForAll("etaDurations") int leg1Duration,
            @ForAll("etaDurations") int leg2Duration) {

        int rawOsrmTotalDuration = leg1Duration + leg2Duration;

        // Mock dependencies
        EmergencyRequestRepository requestRepo = mock(EmergencyRequestRepository.class);
        AmbulanceService ambulanceService = mock(AmbulanceService.class);
        HospitalService hospitalService = mock(HospitalService.class);
        RoutingService routingService = mock(RoutingService.class);
        RoadEventService roadEventService = mock(RoadEventService.class);
        DispatchRecommendationRepository recRepo = mock(DispatchRecommendationRepository.class);
        AuditService auditService = mock(AuditService.class);
        FeatureExtractor featureExtractor = mock(FeatureExtractor.class);
        MlPredictorService mlPredictorService = mock(MlPredictorService.class);

        MlFeatureProperties mlFeatureProperties = new MlFeatureProperties();
        mlFeatureProperties.setEnabled(false);

        EmergencyRequest request = new EmergencyRequest();
        request.setId(1L);
        request.setLatitude(9.01);
        request.setLongitude(38.75);
        request.setUrgencyLevel("1");
        request.setRequiredCareType("trauma");
        when(requestRepo.findById(1L)).thenReturn(Optional.of(request));

        Ambulance ambulance = Ambulance.builder()
                .id(10L)
                .name("AMB-1")
                .latitude(9.02)
                .longitude(38.76)
                .status("available")
                .equipment(new String[]{"oxygen"})
                .build();
        when(ambulanceService.findEligible(any())).thenReturn(List.of(ambulance));

        Hospital hospital = Hospital.builder()
                .id(20L)
                .name("HOSP-1")
                .latitude(9.03)
                .longitude(38.77)
                .available(true)
                .capabilities(new String[]{"trauma"})
                .build();
        when(hospitalService.findEligible(any())).thenReturn(List.of(hospital));

        when(roadEventService.findActive()).thenReturn(List.of());

        RouteResult routeResult = new RouteResult(
                List.of(
                        new RouteLegResult("ambulance_to_scene", leg1Duration, "geom1"),
                        new RouteLegResult("scene_to_hospital", leg2Duration, "geom2")
                ),
                rawOsrmTotalDuration
        );
        when(routingService.computeTwoLegRoute(any(), any(), any(), any())).thenReturn(routeResult);

        RecommendationEngineImpl engine = new RecommendationEngineImpl(
                requestRepo,
                ambulanceService,
                hospitalService,
                routingService,
                roadEventService,
                recRepo,
                auditService,
                mlFeatureProperties,
                featureExtractor
        );
        ReflectionTestUtils.setField(engine, "mlPredictorService", mlPredictorService);

        RecommendationResponseDto response = engine.recommend(1L);

        // Verify ML predictor was NEVER called
        verify(mlPredictorService, never()).predict(any());

        // Verify returned total ETA matches raw OSRM duration
        assertEquals(rawOsrmTotalDuration, response.getRoute().getTotalDurationSeconds());
    }

    @Provide
    Arbitrary<Integer> etaDurations() {
        return Arbitraries.integers().between(60, 1800);
    }
}
