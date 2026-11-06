package com.lifelane.controller;

import com.lifelane.dto.request.EmergencyRequestDto;
import com.lifelane.dto.response.EmergencyResponseDto;
import com.lifelane.dto.response.RecommendationResponseDto;
import com.lifelane.model.EmergencyRequest;
import com.lifelane.repository.EmergencyRequestRepository;
import com.lifelane.service.RecommendationEngine;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for emergency request creation and recommendation generation.
 *
 * <p>Endpoints:
 * <ul>
 *   <li>{@code POST /api/emergencies} — create a new emergency request</li>
 *   <li>{@code POST /api/emergencies/{id}/recommendations} — generate a dispatch recommendation</li>
 * </ul>
 * </p>
 */
@RestController
@RequestMapping("/api/emergencies")
@RequiredArgsConstructor
@Tag(name = "Emergencies", description = "Emergency request management and recommendation generation")
public class EmergencyController {

    private final EmergencyRequestRepository emergencyRequestRepository;
    private final RecommendationEngine recommendationEngine;

    /**
     * Creates a new emergency request from the validated DTO and returns 201.
     *
     * <p>Bean Validation on {@link EmergencyRequestDto} is triggered by {@code @Valid}.
     * Any validation failure is handled by {@link com.lifelane.exception.GlobalExceptionHandler}
     * returning HTTP 400 with field-level errors.</p>
     */
    @Operation(summary = "Create emergency request",
               description = "Submits a new emergency request with location, urgency level, and required care type.")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Emergency request created"),
        @ApiResponse(responseCode = "400", description = "Validation error — missing or out-of-range fields")
    })
    @PostMapping
    public ResponseEntity<EmergencyResponseDto> createEmergency(
            @Valid @RequestBody EmergencyRequestDto dto) {

        EmergencyRequest entity = EmergencyRequest.builder()
                .latitude(dto.getLatitude())
                .longitude(dto.getLongitude())
                .urgencyLevel(dto.getUrgencyLevel())
                .requiredCareType(dto.getRequiredCareType())
                .status("open")
                .build();

        EmergencyRequest saved = emergencyRequestRepository.save(entity);

        EmergencyResponseDto response = EmergencyResponseDto.builder()
                .id(saved.getId())
                .latitude(saved.getLatitude())
                .longitude(saved.getLongitude())
                .urgencyLevel(saved.getUrgencyLevel())
                .requiredCareType(saved.getRequiredCareType())
                .status(saved.getStatus())
                .createdAt(saved.getCreatedAt())
                .build();

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Triggers the recommendation engine for the given emergency request.
     *
     * <p>Returns the best ambulance/hospital pair, route details, explanation,
     * and ranked alternatives. Delegates entirely to {@link RecommendationEngine}.</p>
     *
     * @param id the ID of the existing {@link EmergencyRequest}
     * @throws java.util.NoSuchElementException if the request ID is not found → 404
     * @throws com.lifelane.exception.NoEligibleAmbulanceException → 422
     * @throws com.lifelane.exception.NoEligibleHospitalException → 422
     * @throws com.lifelane.exception.RoutingServiceException → 502
     */
    @Operation(summary = "Generate dispatch recommendation",
               description = "Runs the recommendation engine for the given emergency request ID.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Recommendation generated"),
        @ApiResponse(responseCode = "404", description = "Emergency request not found"),
        @ApiResponse(responseCode = "422", description = "No eligible ambulances or hospitals"),
        @ApiResponse(responseCode = "502", description = "Routing service unavailable")
    })
    @PostMapping("/{id}/recommendations")
    public ResponseEntity<RecommendationResponseDto> generateRecommendation(
            @PathVariable Long id) {

        RecommendationResponseDto recommendation = recommendationEngine.recommend(id);
        return ResponseEntity.ok(recommendation);
    }
}
