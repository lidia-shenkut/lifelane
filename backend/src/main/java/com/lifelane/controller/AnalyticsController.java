package com.lifelane.controller;

import com.lifelane.dto.response.AnalyticsSummaryDto;
import com.lifelane.service.AnalyticsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for aggregate analytics over recorded dispatch decisions.
 *
 * <p>Endpoint: {@code GET /api/analytics/summary}</p>
 */
@RestController
@RequestMapping("/api/analytics")
@RequiredArgsConstructor
@Tag(name = "Analytics", description = "Aggregate metrics over recorded dispatch decisions")
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    /**
     * Returns a summary of system performance metrics.
     *
     * <p>When no decisions have been recorded all numeric fields are zero
     * and {@code overrideRatePercent} is 0.0. This behaviour is guaranteed
     * by {@link AnalyticsService#computeSummary()}.</p>
     */
    @Operation(summary = "Get analytics summary",
               description = "Returns total requests, average ETA, override rate, and per-scenario recommendation counts.")
    @ApiResponse(responseCode = "200", description = "Analytics summary returned")
    @GetMapping("/summary")
    public ResponseEntity<AnalyticsSummaryDto> getSummary() {
        return ResponseEntity.ok(analyticsService.computeSummary());
    }
}
