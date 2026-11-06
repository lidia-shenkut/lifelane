package com.lifelane.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for creating a new emergency request.
 *
 * <p>Coordinates must fall within valid geographic ranges.
 * Both {@code urgencyLevel} and {@code requiredCareType} are required.</p>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmergencyRequestDto {

    @NotNull(message = "latitude is required")
    @DecimalMin(value = "-90.0", message = "latitude must be between -90 and 90")
    @DecimalMax(value = "90.0",  message = "latitude must be between -90 and 90")
    @Schema(description = "Incident latitude in decimal degrees", example = "9.0120",
            minimum = "-90", maximum = "90")
    private Double latitude;

    @NotNull(message = "longitude is required")
    @DecimalMin(value = "-180.0", message = "longitude must be between -180 and 180")
    @DecimalMax(value = "180.0",  message = "longitude must be between -180 and 180")
    @Schema(description = "Incident longitude in decimal degrees", example = "38.7612",
            minimum = "-180", maximum = "180")
    private Double longitude;

    @NotBlank(message = "urgencyLevel is required")
    @Schema(description = "Urgency level of the emergency", example = "high",
            allowableValues = {"low", "medium", "high", "critical"})
    private String urgencyLevel;

    @NotBlank(message = "requiredCareType is required")
    @Schema(description = "Type of medical care required", example = "trauma")
    private String requiredCareType;
}
