package com.lifelane.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Represents one leg of the two-part emergency route.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RouteSegmentDto {

    @Schema(description = "Route leg identifier",
            example = "ambulance_to_scene",
            allowableValues = {"ambulance_to_scene", "scene_to_hospital"})
    private String leg;

    @Schema(description = "Duration of this route leg in seconds", example = "240")
    private int durationSeconds;

    @Schema(description = "Encoded polyline geometry string for map rendering",
            example = "polyline_string_1")
    private String geometry;
}
