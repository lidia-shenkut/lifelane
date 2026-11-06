package com.lifelane.controller;

import com.lifelane.dto.request.DecisionRequestDto;
import com.lifelane.dto.response.ErrorResponseDto;
import com.lifelane.exception.DuplicateDecisionException;
import com.lifelane.model.Ambulance;
import com.lifelane.model.DispatchDecision;
import com.lifelane.model.DispatchRecommendation;
import com.lifelane.model.EmergencyRequest;
import com.lifelane.model.Hospital;
import com.lifelane.repository.AmbulanceRepository;
import com.lifelane.repository.DispatchDecisionRepository;
import com.lifelane.repository.DispatchRecommendationRepository;
import com.lifelane.repository.EmergencyRequestRepository;
import com.lifelane.repository.HospitalRepository;
import com.lifelane.service.AuditService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.NoSuchElementException;

/**
 * REST controller for recording dispatcher accept/override decisions.
 *
 * <p>Endpoint: {@code POST /api/emergencies/{id}/decision}</p>
 *
 * <p>Business rules enforced here:
 * <ul>
 *   <li>Action {@code "override"} requires non-null {@code ambulanceId} and {@code hospitalId} → 400</li>
 *   <li>Duplicate decision for the same request → 409 (caught from DB {@code UNIQUE} constraint)</li>
 * </ul>
 * </p>
 */
@RestController
@RequestMapping("/api/emergencies")
@RequiredArgsConstructor
@Tag(name = "Decisions", description = "Dispatcher accept/override decision recording")
public class DecisionController {

    private final EmergencyRequestRepository emergencyRequestRepository;
    private final DispatchRecommendationRepository recommendationRepository;
    private final DispatchDecisionRepository decisionRepository;
    private final AmbulanceRepository ambulanceRepository;
    private final HospitalRepository hospitalRepository;
    private final AuditService auditService;

    /**
     * Records an accept or override decision for an emergency request.
     *
     * @param id  the emergency request ID
     * @param dto the decision payload
     * @throws org.springframework.web.bind.MethodArgumentNotValidException if {@code action} is blank → 400
     * @throws NoSuchElementException if request/recommendation/ambulance/hospital not found → 404
     * @throws DuplicateDecisionException if a decision was already recorded → 409
     */
    @Operation(summary = "Record dispatch decision",
               description = "Accepts or overrides the system recommendation for the given emergency request.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Decision recorded"),
        @ApiResponse(responseCode = "400", description = "Validation error or missing override fields"),
        @ApiResponse(responseCode = "404", description = "Emergency request or referenced resource not found"),
        @ApiResponse(responseCode = "409", description = "Decision already recorded for this request")
    })
    @PostMapping("/{id}/decision")
    public ResponseEntity<?> recordDecision(
            @PathVariable Long id,
            @Valid @RequestBody DecisionRequestDto dto) {

        // ── Cross-field validation for override ─────────────────────────────
        if ("override".equalsIgnoreCase(dto.getAction())) {
            if (dto.getAmbulanceId() == null || dto.getHospitalId() == null) {
                ErrorResponseDto error = ErrorResponseDto.builder()
                        .status(400)
                        .message("Validation failed")
                        .errors(Collections.singletonList(
                                ErrorResponseDto.FieldErrorDto.builder()
                                        .field("ambulanceId/hospitalId")
                                        .message("ambulanceId and hospitalId are required for override action")
                                        .build()))
                        .build();
                return ResponseEntity.badRequest().body(error);
            }
        }

        // ── Load emergency request ───────────────────────────────────────────
        EmergencyRequest request = emergencyRequestRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException(
                        "Emergency request not found with id: " + id));

        // ── Load the latest recommendation for this request ─────────────────
        DispatchRecommendation recommendation = recommendationRepository
                .findByEmergencyRequest_Id(id)
                .stream()
                .findFirst()
                .orElseThrow(() -> new NoSuchElementException(
                        "No recommendation found for emergency request id: " + id));

        // ── Resolve chosen ambulance and hospital ────────────────────────────
        Ambulance chosenAmbulance;
        Hospital chosenHospital;

        if ("override".equalsIgnoreCase(dto.getAction())) {
            chosenAmbulance = ambulanceRepository.findById(dto.getAmbulanceId())
                    .orElseThrow(() -> new NoSuchElementException(
                            "Ambulance not found with id: " + dto.getAmbulanceId()));
            chosenHospital = hospitalRepository.findById(dto.getHospitalId())
                    .orElseThrow(() -> new NoSuchElementException(
                            "Hospital not found with id: " + dto.getHospitalId()));
        } else {
            // accept — use the recommended resources
            chosenAmbulance = recommendation.getRecommendedAmbulance();
            chosenHospital = recommendation.getRecommendedHospital();
        }

        // ── Persist decision — DB UNIQUE constraint catches duplicates ───────
        DispatchDecision decision = DispatchDecision.builder()
                .emergencyRequest(request)
                .recommendation(recommendation)
                .action(dto.getAction().toLowerCase())
                .chosenAmbulance(chosenAmbulance)
                .chosenHospital(chosenHospital)
                .dispatcherComment(dto.getComment())
                .build();

        DispatchDecision saved;
        try {
            saved = decisionRepository.save(decision);
        } catch (DataIntegrityViolationException ex) {
            throw new DuplicateDecisionException(
                    "Decision already recorded for this request");
        }

        // ── Audit ────────────────────────────────────────────────────────────
        auditService.recordDecision(saved);

        // ── Build response ───────────────────────────────────────────────────
        DecisionResponseDto response = DecisionResponseDto.builder()
                .decisionId(saved.getId())
                .emergencyRequestId(id)
                .action(saved.getAction())
                .chosenAmbulanceId(saved.getChosenAmbulance().getId())
                .chosenHospitalId(saved.getChosenHospital().getId())
                .comment(saved.getDispatcherComment())
                .createdAt(saved.getCreatedAt())
                .build();

        return ResponseEntity.ok(response);
    }

    // ── Inner response DTO (decision-specific, not reused elsewhere) ─────────

    @lombok.Value
    @lombok.Builder
    @lombok.AllArgsConstructor
    static class DecisionResponseDto {
        Long decisionId;
        Long emergencyRequestId;
        String action;
        Long chosenAmbulanceId;
        Long chosenHospitalId;
        String comment;
        OffsetDateTime createdAt;
    }
}
