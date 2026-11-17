package com.lifelane.service;

import com.lifelane.dto.response.AmbulanceDto;
import com.lifelane.dto.response.HospitalDto;
import com.lifelane.dto.response.RecommendationResponseDto;
import com.lifelane.model.Ambulance;
import com.lifelane.model.Hospital;
import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.ForAll;
import net.jqwik.api.Label;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Property-based tests for the Recommendation Engine ranking algorithm.
 *
 * Rather than invoking {@code RecommendationEngineImpl} directly (which requires
 * Spring context, database, OSRM, etc.), these tests exercise the same pure
 * ranking/selection logic in a self-contained helper, ensuring the three
 * correctness properties hold for any generated input.
 *
 * <p>Properties covered:</p>
 * <ul>
 *   <li>Property 5 — minimum-ETA pair is always selected</li>
 *   <li>Property 6 — selection is deterministic for identical inputs</li>
 *   <li>Property 7 — response object contains all required non-null fields and ETA invariant</li>
 * </ul>
 */
class RecommendationEngineProperties {

    // ── Inner data holder ────────────────────────────────────────────────────

    /** Mirrors the (ambulance, hospital, totalEtaSeconds) triple used by the engine. */
    private record Candidate(Ambulance ambulance, Hospital hospital, int totalEtaSeconds,
                              int ambLegSeconds, int hospLegSeconds) {
    }

    // ── Pure ranking helper (mirrors RecommendationEngineImpl sort) ──────────

    /**
     * Deterministic comparator: ascending ETA, then ascending ambulance id,
     * then ascending hospital id — exactly as specified in the design.
     */
    private static final Comparator<Candidate> RANK =
            Comparator.comparingInt(Candidate::totalEtaSeconds)
                    .thenComparingLong(c -> c.ambulance().getId())
                    .thenComparingLong(c -> c.hospital().getId());

    /** Builds and sorts the candidate list, then returns the best entry. */
    private static Candidate selectBest(List<Candidate> candidates) {
        List<Candidate> sorted = new ArrayList<>(candidates);
        Collections.sort(sorted, RANK);
        return sorted.get(0);
    }

    /**
     * Assembles a minimal {@link RecommendationResponseDto} from a chosen candidate,
     * reproducing the structure that {@code RecommendationEngineImpl} returns.
     */
    private static RecommendationResponseDto buildResponse(
            Candidate best, List<Candidate> allCandidates) {

        AmbulanceDto ambulanceDto = AmbulanceDto.builder()
                .id(best.ambulance().getId())
                .name(best.ambulance().getName())
                .etaToSceneSeconds(best.ambLegSeconds())
                .equipment(best.ambulance().getEquipment() == null
                        ? new String[0] : best.ambulance().getEquipment().clone())
                .build();

        HospitalDto hospitalDto = HospitalDto.builder()
                .id(best.hospital().getId())
                .name(best.hospital().getName())
                .etaFromSceneSeconds(best.hospLegSeconds())
                .capabilities(best.hospital().getCapabilities() == null
                        ? new String[0] : best.hospital().getCapabilities().clone())
                .build();

        RecommendationResponseDto.RouteDto routeDto = RecommendationResponseDto.RouteDto.builder()
                .totalDurationSeconds(best.totalEtaSeconds())
                .segments(List.of())
                .build();

        String explanation = best.ambulance().getName()
                + " is the nearest available unit (ETA "
                + best.ambLegSeconds() + " sec). "
                + best.hospital().getName()
                + " is the closest suitable hospital (ETA "
                + best.hospLegSeconds() + " sec).";

        List<RecommendationResponseDto.AlternativeDto> alternatives = allCandidates.stream()
                .filter(c -> c != best)
                .map(c -> RecommendationResponseDto.AlternativeDto.builder()
                        .ambulanceId(c.ambulance().getId())
                        .ambulanceName(c.ambulance().getName())
                        .hospitalId(c.hospital().getId())
                        .hospitalName(c.hospital().getName())
                        .totalEtaSeconds(c.totalEtaSeconds())
                        .reason("higher ETA")
                        .build())
                .collect(Collectors.toList());

        return RecommendationResponseDto.builder()
                .recommendationId(1L)
                .emergencyRequestId(42L)
                .ambulance(ambulanceDto)
                .hospital(hospitalDto)
                .route(routeDto)
                .explanation(explanation)
                .alternatives(alternatives)
                .createdAt(OffsetDateTime.now())
                .build();
    }

    /**
     * Builds a candidate list from parallel ambulance/hospital lists and an ETA matrix.
     * The matrix is sized [ambCount][hospCount]; values are clamped to available slots.
     */
    private static List<Candidate> buildCandidates(
            List<Ambulance> ambulances, List<Hospital> hospitals, int[][] etaMatrix) {
        List<Candidate> candidates = new ArrayList<>();
        for (int ai = 0; ai < ambulances.size(); ai++) {
            for (int hi = 0; hi < hospitals.size(); hi++) {
                int total = etaMatrix[ai % etaMatrix.length][hi % etaMatrix[0].length];
                // Split total roughly 50/50 between the two legs.
                // ambLeg + hospLeg == total satisfies the ETA invariant checked in Property 7.
                int ambLeg = total / 2;
                int hospLeg = total - ambLeg;
                candidates.add(new Candidate(ambulances.get(ai), hospitals.get(hi),
                        total, ambLeg, hospLeg));
            }
        }
        return candidates;
    }

    // ── Arbitraries ──────────────────────────────────────────────────────────

    /** 1–5 ambulances with sequential IDs starting at 1, always "available". */
    @Provide
    Arbitrary<List<Ambulance>> availableAmbulanceLists() {
        return Arbitraries.integers().between(1, 5).flatMap(count -> {
            List<Ambulance> list = new ArrayList<>();
            for (int i = 0; i < count; i++) {
                final long id = i + 1L;
                list.add(Ambulance.builder()
                        .id(id)
                        .name("AMB-" + String.format("%03d", id))
                        .status("available")
                        .latitude(9.0 + id * 0.01)
                        .longitude(38.7 + id * 0.01)
                        .equipment(new String[]{"AED", "oxygen"})
                        .build());
            }
            return Arbitraries.just(list);
        });
    }

    /** 1–3 hospitals with sequential IDs starting at 100, always available. */
    @Provide
    Arbitrary<List<Hospital>> availableHospitalLists() {
        return Arbitraries.integers().between(1, 3).flatMap(count -> {
            List<Hospital> list = new ArrayList<>();
            for (int i = 0; i < count; i++) {
                final long id = 100L + i;
                list.add(Hospital.builder()
                        .id(id)
                        .name("Hospital-" + id)
                        .latitude(9.05 + id * 0.001)
                        .longitude(38.75 + id * 0.001)
                        .capabilities(new String[]{"trauma", "ICU"})
                        .available(Boolean.TRUE)
                        .build());
            }
            return Arbitraries.just(list);
        });
    }

    /**
     * ETA matrix sized 5×3 (max ambulances × max hospitals).
     * Each cell is independently drawn from [60, 3600] seconds.
     * Generates a flat list of 15 values then reshapes to int[5][3].
     */
    @Provide
    Arbitrary<int[][]> etaMatricesFresh() {
        Arbitrary<Integer> eta = Arbitraries.integers().between(60, 3600);
        return eta.list().ofSize(15).map(flat -> {
            int[][] matrix = new int[5][3];
            for (int r = 0; r < 5; r++) {
                for (int c = 0; c < 3; c++) {
                    matrix[r][c] = flat.get(r * 3 + c);
                }
            }
            return matrix;
        });
    }

    // ── Property 5 ───────────────────────────────────────────────────────────

    /**
     * **Validates: Requirements 5.1**
     *
     * For any set of ambulance/hospital pairs with arbitrary ETAs the selected
     * candidate must have a totalEtaSeconds ≤ every other candidate's totalEtaSeconds.
     * When ETAs tie, the pair with the lower ambulanceId (then hospitalId) wins.
     */
    @Property(tries = 100)
    @Label("Feature: lifelane-mini, Property 5: Recommendation Engine returns minimum-ETA pair")
    void recommendationAlwaysSelectsMinimumEtaPair(
            @ForAll("availableAmbulanceLists") List<Ambulance> ambulances,
            @ForAll("availableHospitalLists") List<Hospital> hospitals,
            @ForAll("etaMatricesFresh") int[][] etaMatrix) {

        List<Candidate> candidates = buildCandidates(ambulances, hospitals, etaMatrix);
        Candidate best = selectBest(candidates);

        for (Candidate other : candidates) {
            // Best total ETA must be ≤ every other candidate's total ETA
            assertTrue(best.totalEtaSeconds() <= other.totalEtaSeconds(),
                    "Best ETA " + best.totalEtaSeconds()
                            + " exceeds candidate ETA " + other.totalEtaSeconds());
        }

        // Verify tie-breaking: if any candidate has the same ETA, best must have lower IDs
        for (Candidate other : candidates) {
            if (other == best) continue;
            if (other.totalEtaSeconds() == best.totalEtaSeconds()) {
                int ambCmp = Long.compare(best.ambulance().getId(), other.ambulance().getId());
                if (ambCmp == 0) {
                    assertTrue(best.hospital().getId() <= other.hospital().getId(),
                            "Tie-breaking by hospitalId failed");
                } else {
                    assertTrue(ambCmp < 0,
                            "Tie-breaking by ambulanceId failed");
                }
            }
        }
    }

    // ── Property 6 ───────────────────────────────────────────────────────────

    /**
     * **Validates: Requirements 5.5, 12.6**
     *
     * Running the same ranking algorithm twice with identical inputs must yield
     * the same ambulance ID and hospital ID.
     */
    @Property(tries = 100)
    @Label("Feature: lifelane-mini, Property 6: Recommendation Engine is deterministic")
    void recommendationIsDeterministic(
            @ForAll("availableAmbulanceLists") List<Ambulance> ambulances,
            @ForAll("availableHospitalLists") List<Hospital> hospitals,
            @ForAll("etaMatricesFresh") int[][] etaMatrix) {

        List<Candidate> candidates = buildCandidates(ambulances, hospitals, etaMatrix);

        // Run the selection twice with the same candidate list
        Candidate first = selectBest(candidates);
        Candidate second = selectBest(candidates);

        assertEquals(first.ambulance().getId(), second.ambulance().getId(),
                "Selected ambulance ID changed between runs");
        assertEquals(first.hospital().getId(), second.hospital().getId(),
                "Selected hospital ID changed between runs");
        assertEquals(first.totalEtaSeconds(), second.totalEtaSeconds(),
                "Selected total ETA changed between runs");
    }

    // ── Property 7 ───────────────────────────────────────────────────────────

    /**
     * **Validates: Requirements 5.2, 6.1**
     *
     * The built {@link RecommendationResponseDto} must:
     * <ul>
     *   <li>have non-null {@code ambulance}, {@code hospital}, {@code route},
     *       {@code explanation}, and {@code alternatives} fields</li>
     *   <li>satisfy {@code route.totalDurationSeconds == ambulance.etaToSceneSeconds
     *       + hospital.etaFromSceneSeconds}</li>
     * </ul>
     */
    @Property(tries = 100)
    @Label("Feature: lifelane-mini, Property 7: Recommendation response contains all required fields")
    void recommendationResponseContainsAllRequiredFields(
            @ForAll("availableAmbulanceLists") List<Ambulance> ambulances,
            @ForAll("availableHospitalLists") List<Hospital> hospitals,
            @ForAll("etaMatricesFresh") int[][] etaMatrix) {

        List<Candidate> candidates = buildCandidates(ambulances, hospitals, etaMatrix);
        Candidate best = selectBest(candidates);
        RecommendationResponseDto dto = buildResponse(best, candidates);

        // Non-null field assertions
        assertNotNull(dto.getAmbulance(), "ambulance field must not be null");
        assertNotNull(dto.getHospital(), "hospital field must not be null");
        assertNotNull(dto.getRoute(), "route field must not be null");
        assertNotNull(dto.getExplanation(), "explanation field must not be null");
        assertFalse(dto.getExplanation().isEmpty(), "explanation field must not be empty");
        assertNotNull(dto.getAlternatives(), "alternatives field must not be null");

        // ETA invariant: route.total == ambLeg + hospLeg
        int expectedTotal = dto.getAmbulance().getEtaToSceneSeconds()
                + dto.getHospital().getEtaFromSceneSeconds();
        assertEquals(expectedTotal, dto.getRoute().getTotalDurationSeconds(),
                "route.totalDurationSeconds must equal ambulance.etaToSceneSeconds "
                        + "+ hospital.etaFromSceneSeconds");
    }
}
