package com.lifelane.service;

import com.lifelane.dto.response.RecommendationResponseDto;
import com.lifelane.dto.response.RecommendationResponseDto.AlternativeDto;
import com.lifelane.dto.response.RouteSegmentDto;
import com.lifelane.exception.NoEligibleAmbulanceException;
import com.lifelane.exception.NoEligibleHospitalException;
import com.lifelane.model.Ambulance;
import com.lifelane.model.DispatchRecommendation;
import com.lifelane.model.Hospital;
import com.lifelane.model.EmergencyRequest;
import com.lifelane.model.RoadEvent;
import com.lifelane.repository.DispatchRecommendationRepository;
import com.lifelane.repository.EmergencyRequestRepository;
import com.lifelane.config.MlFeatureProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Implementation of the recommendation engine (Task 7.1) and explanation builder (Task 7.2).
 *
 * The algorithm follows the design specification:
 *   1. Load the emergency request.
 *   2. Filter ambulances and hospitals, throwing domain exceptions if none are eligible.
 *   3. Fetch active road events.
 *   4. Compute pair‑wise two‑leg routes using {@link RoutingService}.
 *   5. Sort candidates by total ETA, then ambulance id, then hospital id (stable deterministic order).
 *   6. Persist the chosen {@link DispatchRecommendation} and audit it.
 *   7. Assemble a human‑readable explanation.
 */
@Service
@RequiredArgsConstructor
public class RecommendationEngineImpl implements RecommendationEngine {

    private final EmergencyRequestRepository emergencyRequestRepository;
    private final AmbulanceService ambulanceService;
    private final HospitalService hospitalService;
    private final RoutingService routingService;
    private final RoadEventService roadEventService;
    private final DispatchRecommendationRepository recommendationRepository;
    private final AuditService auditService;
    private final MlFeatureProperties mlFeatureProperties;
    private final FeatureExtractor featureExtractor;

    @Autowired(required = false)
    private MlPredictorService mlPredictorService;

    @Override
    public RecommendationResponseDto recommend(Long emergencyRequestId) {
        // 1. Load request
        EmergencyRequest request = emergencyRequestRepository.findById(emergencyRequestId)
                .orElseThrow(() -> new java.util.NoSuchElementException("Emergency request not found: " + emergencyRequestId));

        // 2. Filter eligible resources (no equipment requirement in model)
        List<Ambulance> eligibleAmbulances = ambulanceService.findEligible(Set.of());
        if (eligibleAmbulances.isEmpty()) {
            throw new NoEligibleAmbulanceException("No ambulances satisfy required equipment");
        }
        List<Hospital> eligibleHospitals = hospitalService.findEligible(request.getRequiredCareType());
        if (eligibleHospitals.isEmpty()) {
            throw new NoEligibleHospitalException("No hospitals support required care type");
        }

        // 3. Active road events
        List<RoadEvent> activeEvents = roadEventService.findActive();

        // 4. Compute pair‑wise routes
        Coordinate incidentLoc = new Coordinate(request.getLatitude(), request.getLongitude());
        List<Candidate> candidates = new ArrayList<>();
        for (Ambulance amb : eligibleAmbulances) {
            for (Hospital hosp : eligibleHospitals) {
                Coordinate ambLoc = new Coordinate(amb.getLatitude(), amb.getLongitude());
                Coordinate hospLoc = new Coordinate(hosp.getLatitude(), hosp.getLongitude());
                RouteResult route = routingService.computeTwoLegRoute(
                        ambLoc,
                        incidentLoc,
                        hospLoc,
                        activeEvents);

                int effectiveEta = route.totalDurationSeconds();
                if (mlFeatureProperties != null && mlFeatureProperties.isEnabled() && mlPredictorService != null) {
                    FeatureVector features = featureExtractor.extract(route, request, activeEvents);
                    effectiveEta = mlPredictorService.predict(features);
                }

                candidates.add(new Candidate(amb, hosp, route, effectiveEta));
            }
        }

        // 5. Deterministic sorting
        Comparator<Candidate> comparator = Comparator
                .comparingInt((Candidate c) -> c.effectiveEtaSeconds)
                .thenComparingLong(c -> c.ambulance.getId())
                .thenComparingLong(c -> c.hospital.getId());
        Collections.sort(candidates, comparator);

        // 6. Choose best candidate
        Candidate best = candidates.get(0);

        int bestAmbEta = getLegDuration(best.route, "ambulance_to_scene");
        int bestHospEta = getLegDuration(best.route, "scene_to_hospital");
        String routeGeometry = buildRouteGeometry(best.route);
        String explanation = buildExplanation(best, candidates, request);

        // 7. Persist recommendation and audit
        DispatchRecommendation dr = new DispatchRecommendation();
        dr.setEmergencyRequest(request);
        dr.setRecommendedAmbulance(best.ambulance);
        dr.setRecommendedHospital(best.hospital);
        dr.setAmbulanceEtaSeconds(bestAmbEta);
        dr.setHospitalEtaSeconds(bestHospEta);
        dr.setTotalEtaSeconds(best.effectiveEtaSeconds);
        dr.setRouteGeometry(routeGeometry);
        dr.setExplanation(explanation);
        dr.setCreatedAt(OffsetDateTime.now());
        recommendationRepository.save(dr);
        auditService.recordRecommendation(dr, request.getId());

        // 8. Build response DTO
        RecommendationResponseDto dto = RecommendationResponseDto.builder()
                .recommendationId(dr.getId())
                .emergencyRequestId(request.getId())
                .ambulance(mapAmbulanceDto(best.ambulance, bestAmbEta))
                .hospital(mapHospitalDto(best.hospital, bestHospEta))
                .route(mapRouteDto(best.route))
                .explanation(explanation)
                .alternatives(buildAlternatives(best, candidates))
                .createdAt(dr.getCreatedAt())
                .build();
        return dto;
    }

    /** Returns the duration in seconds for the named leg, or 0 if not found. */
    private int getLegDuration(RouteResult route, String legName) {
        return route.legs().stream()
                .filter(l -> legName.equals(l.leg()))
                .mapToInt(RouteLegResult::durationSeconds)
                .findFirst()
                .orElse(0);
    }

    /** Concatenates leg geometries separated by a pipe character. */
    private String buildRouteGeometry(RouteResult route) {
        return route.legs().stream()
                .map(RouteLegResult::geometry)
                .collect(Collectors.joining("|"));
    }

    /** Helper to map an Ambulance to its DTO representation. */
    private com.lifelane.dto.response.AmbulanceDto mapAmbulanceDto(Ambulance amb, int etaSeconds) {
        return com.lifelane.dto.response.AmbulanceDto.builder()
                .id(amb.getId())
                .name(amb.getName())
                .etaToSceneSeconds(etaSeconds)
                .equipment(amb.getEquipment() == null ? new String[0] : amb.getEquipment().clone())
                .build();
    }

    private com.lifelane.dto.response.HospitalDto mapHospitalDto(Hospital hosp, int etaSeconds) {
        return com.lifelane.dto.response.HospitalDto.builder()
                .id(hosp.getId())
                .name(hosp.getName())
                .etaFromSceneSeconds(etaSeconds)
                .capabilities(hosp.getCapabilities() == null ? new String[0] : hosp.getCapabilities().clone())
                .build();
    }

    private RecommendationResponseDto.RouteDto mapRouteDto(RouteResult route) {
        return RecommendationResponseDto.RouteDto.builder()
                .totalDurationSeconds(route.totalDurationSeconds())
                .segments(route.legs().stream()
                        .map(leg -> new RouteSegmentDto(
                                leg.leg(),
                                leg.durationSeconds(),
                                leg.geometry()))
                        .collect(Collectors.toList()))
                .build();
    }

    /** Creates a human‑readable explanation string for the chosen candidate. */
    private String buildExplanation(Candidate best, List<Candidate> allCandidates, EmergencyRequest request) {
        StringBuilder sb = new StringBuilder();
        sb.append(best.ambulance.getName())
                .append(" is the nearest available unit with ")
                .append(String.join(", ", best.ambulance.getEquipment()))
                .append(" equipment (ETA ")
                .append(getLegDuration(best.route, "ambulance_to_scene"))
                .append(" sec). ");
        sb.append(best.hospital.getName())
                .append(" is the closest suitable hospital (ETA ")
                .append(getLegDuration(best.route, "scene_to_hospital"))
                .append(" sec). ");

        List<Ambulance> allAmbulances = ambulanceService.findAll();
        List<Hospital> allHospitals = hospitalService.findAll();

        List<Ambulance> excludedAmbulances = allAmbulances.stream()
                .filter(a -> !best.ambulance.equals(a))
                .collect(Collectors.toList());
        if (!excludedAmbulances.isEmpty()) {
            sb.append("Excluded ambulances: ");
            for (Ambulance a : excludedAmbulances) {
                sb.append(a.getName())
                        .append(" (")
                        .append(ambulanceEligibilityReason(a, Set.of()))
                        .append("), ");
            }
        }

        List<Hospital> excludedHospitals = allHospitals.stream()
                .filter(h -> !best.hospital.equals(h))
                .collect(Collectors.toList());
        if (!excludedHospitals.isEmpty()) {
            sb.append("Excluded hospitals: ");
            for (Hospital h : excludedHospitals) {
                sb.append(h.getName())
                        .append(" (")
                        .append(hospitalEligibilityReason(h, request.getRequiredCareType()))
                        .append("), ");
            }
        }

        String explanation = sb.toString().trim();
        if (explanation.endsWith(",")) {
            explanation = explanation.substring(0, explanation.length() - 1);
        }
        return explanation;
    }

    /** Reason why an ambulance is not eligible for the current request. */
    private String ambulanceEligibilityReason(Ambulance amb, Set<String> requiredEquipment) {
        if (!"available".equalsIgnoreCase(amb.getStatus())) {
            return "busy";
        }
        String[] equip = amb.getEquipment();
        if (requiredEquipment != null && !requiredEquipment.isEmpty()) {
            if (equip == null || !Arrays.asList(equip).containsAll(requiredEquipment)) {
                return "missing equipment";
            }
        }
        return "ineligible";
    }

    /** Reason why a hospital is not eligible for the current request. */
    private String hospitalEligibilityReason(Hospital hosp, String careType) {
        if (!Boolean.TRUE.equals(hosp.getAvailable())) {
            return "unavailable";
        }
        String[] caps = hosp.getCapabilities();
        if (careType != null && (caps == null || !Arrays.asList(caps).contains(careType))) {
            return "missing capability";
        }
        return "ineligible";
    }

    private List<AlternativeDto> buildAlternatives(Candidate best, List<Candidate> all) {
        return all.stream()
                .filter(c -> c != best)
                .map(c -> AlternativeDto.builder()
                        .ambulanceId(c.ambulance.getId())
                        .ambulanceName(c.ambulance.getName())
                        .hospitalId(c.hospital.getId())
                        .hospitalName(c.hospital.getName())
                        .totalEtaSeconds(c.effectiveEtaSeconds)
                        .reason("higher ETA")
                        .build())
                .collect(Collectors.toList());
    }

    /** Simple holder for a candidate pairing. */
    private static class Candidate {
        final Ambulance ambulance;
        final Hospital hospital;
        final RouteResult route;
        final int effectiveEtaSeconds;
        Candidate(Ambulance a, Hospital h, RouteResult r, int effectiveEtaSeconds) {
            this.ambulance = a;
            this.hospital = h;
            this.route = r;
            this.effectiveEtaSeconds = effectiveEtaSeconds;
        }
    }
}
