package com.lifelane.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * Response DTO for a generated dispatch recommendation.
 *
 * <p>Contains the recommended ambulance and hospital, the two-leg route,
 * a human-readable explanation, and a ranked list of alternatives.</p>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RecommendationResponseDto {

    @Schema(description = "Unique identifier of this recommendation", example = "7")
    private Long recommendationId;

    @Schema(description = "ID of the emergency request this recommendation belongs to",
            example = "42")
    private Long emergencyRequestId;

    @Schema(description = "Recommended ambulance details")
    private AmbulanceDto ambulance;

    @Schema(description = "Recommended hospital details")
    private HospitalDto hospital;

    @Schema(description = "Two-leg route from ambulance to scene and from scene to hospital")
    private RouteDto route;

    @Schema(description = "Human-readable explanation for why this pair was selected",
            example = "AMB-003 is the nearest available unit with trauma equipment (ETA 4 min).")
    private String explanation;

    @Schema(description = "Ranked list of alternative ambulance/hospital pairs")
    private List<AlternativeDto> alternatives;

    @Schema(description = "UTC timestamp when the recommendation was generated")
    private OffsetDateTime createdAt;

    // ── Inner: RouteDto ──────────────────────────────────────────────────────

    /**
     * Aggregated route information for the entire emergency dispatch trip.
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class RouteDto {

        @Schema(description = "Total estimated duration in seconds for both legs combined",
                example = "550")
        private int totalDurationSeconds;

        @Schema(description = "Individual route segments for each leg of the journey")
        private List<RouteSegmentDto> segments;
    }

    // ── Inner: AlternativeDto ────────────────────────────────────────────────

    /**
     * A ranked alternative ambulance/hospital pair that was not selected as
     * the primary recommendation.
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class AlternativeDto {

        @Schema(description = "ID of the alternative ambulance", example = "1")
        private Long ambulanceId;

        @Schema(description = "Name of the alternative ambulance", example = "AMB-001")
        private String ambulanceName;

        @Schema(description = "ID of the alternative hospital", example = "2")
        private Long hospitalId;

        @Schema(description = "Name of the alternative hospital", example = "City Hospital")
        private String hospitalName;

        @Schema(description = "Total ETA in seconds for this alternative pair", example = "720")
        private int totalEtaSeconds;

        @Schema(description = "Reason why this pair was not selected as the primary recommendation",
                example = "AMB-001 is busy; City Hospital lacks trauma capability")
        private String reason;
    }
}
