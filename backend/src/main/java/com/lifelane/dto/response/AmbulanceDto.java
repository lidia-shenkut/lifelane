package com.lifelane.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Nested DTO representing the recommended ambulance within a recommendation response.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AmbulanceDto {

    @Schema(description = "Unique identifier of the ambulance", example = "3")
    private Long id;

    @Schema(description = "Human-readable ambulance name", example = "AMB-003")
    private String name;

    @Schema(description = "Estimated time in seconds for the ambulance to reach the incident scene",
            example = "240")
    private int etaToSceneSeconds;

    @Schema(description = "List of medical equipment carried by the ambulance",
            example = "[\"AED\", \"oxygen\", \"trauma_kit\"]")
    private String[] equipment;
}
