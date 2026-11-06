package com.lifelane.controller;

import com.lifelane.dto.response.HospitalDto;
import com.lifelane.service.HospitalService;
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
 * REST controller that exposes hospital data to the dashboard.
 *
 * <p>Endpoint: {@code GET /api/hospitals}</p>
 */
@RestController
@RequestMapping("/api/hospitals")
@RequiredArgsConstructor
@Tag(name = "Hospitals", description = "Hospital availability and capabilities listings")
public class HospitalController {

    private final HospitalService hospitalService;

    /**
     * Returns the current capabilities and availability of every hospital.
     */
    @Operation(summary = "List all hospitals",
               description = "Returns the full hospital list with capabilities and availability flag.")
    @ApiResponse(responseCode = "200", description = "Hospital list returned")
    @GetMapping
    public ResponseEntity<List<HospitalDto>> getAllHospitals() {
        return ResponseEntity.ok(hospitalService.findAllDtos());
    }
}
