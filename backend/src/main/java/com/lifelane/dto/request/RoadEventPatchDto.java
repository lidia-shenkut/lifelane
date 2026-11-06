package com.lifelane.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for toggling a road event's status.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RoadEventPatchDto {

    @NotBlank(message = "status is required")
    @Schema(description = "New status for the road event",
            example = "active", allowableValues = {"active", "inactive"})
    private String status;
}
