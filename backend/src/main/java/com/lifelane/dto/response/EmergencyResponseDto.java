package com.lifelane.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

/**
 * Response DTO representing a created or retrieved emergency request.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmergencyResponseDto {

    @Schema(description = "Unique identifier of the emergency request", example = "42")
    private Long id;

    @Schema(description = "Incident latitude in decimal degrees", example = "9.0120")
    private Double latitude;

    @Schema(description = "Incident longitude in decimal degrees", example = "38.7612")
    private Double longitude;

    @Schema(description = "Urgency level", example = "high",
            allowableValues = {"low", "medium", "high", "critical"})
    private String urgencyLevel;

    @Schema(description = "Type of medical care required", example = "trauma")
    private String requiredCareType;

    @Schema(description = "Current status of the request", example = "open",
            allowableValues = {"open", "dispatched", "closed"})
    private String status;

    @Schema(description = "UTC timestamp when the request was created")
    private OffsetDateTime createdAt;
}
