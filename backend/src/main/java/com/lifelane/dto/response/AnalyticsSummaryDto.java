package com.lifelane.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * Response DTO for the analytics summary endpoint.
 *
 * <p>{@code mlMetrics} is {@code null} when the ML feature flag is disabled.
 * When enabled it contains a map of ML performance metrics (MAE, improvement
 * percentage, etc.) — typed as {@code Object} to avoid a compile-time
 * dependency on the optional ML module.</p>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AnalyticsSummaryDto {

    @Schema(description = "Total number of emergency requests created", example = "47")
    private long totalEmergencyRequests;

    @Schema(description = "Average total ETA (ambulance-to-scene + scene-to-hospital) in seconds",
            example = "512.3")
    private double averageTotalEtaSeconds;

    @Schema(description = "Percentage of recommendations that were overridden by the dispatcher",
            example = "14.9")
    private double overrideRatePercent;

    @Schema(description = "Number of recommendations grouped by road-event scenario label")
    private Map<String, Long> recommendationsPerScenario;

    @Schema(description = "ML model performance metrics; null when ML is disabled")
    private Object mlMetrics;
}
