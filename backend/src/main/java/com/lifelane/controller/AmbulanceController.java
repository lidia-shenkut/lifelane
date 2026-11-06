package com.lifelane.controller;

import com.lifelane.dto.response.AmbulanceDto;
import com.lifelane.service.AmbulanceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * REST controller that exposes the ambulance fleet to the dashboard.
 *
 * <p>Endpoint: {@code GET /api/ambulances}</p>
 */
@RestController
@RequestMapping("/api/ambulances")
@RequiredArgsConstructor
@Tag(name = "Ambulances", description = "Ambulance fleet status and equipment listings")
public class AmbulanceController {

    private final AmbulanceService ambulanceService;

    /**
     * Returns the current status and equipment list of every ambulance.
     */
    @Operation(summary = "List all ambulances",
               description = "Returns the full ambulance fleet with status and equipment.")
    @ApiResponse(responseCode = "200", description = "Ambulance list returned")
    @GetMapping
    public ResponseEntity<List<AmbulanceDto>> getAllAmbulances() {
        return ResponseEntity.ok(ambulanceService.findAllDtos());
    }
}
