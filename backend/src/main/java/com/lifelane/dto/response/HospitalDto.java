package com.lifelane.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Nested DTO representing the recommended hospital within a recommendation response.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HospitalDto {

    @Schema(description = "Unique identifier of the hospital", example = "1")
    private Long id;

    @Schema(description = "Hospital name", example = "Tikur Anbessa Specialized Hospital")
    private String name;

    @Schema(description = "Estimated time in seconds from the incident scene to the hospital",
            example = "310")
    private int etaFromSceneSeconds;

    @Schema(description = "Medical capabilities offered by the hospital",
            example = "[\"trauma\", \"surgery\", \"ICU\"]")
    private String[] capabilities;
}
