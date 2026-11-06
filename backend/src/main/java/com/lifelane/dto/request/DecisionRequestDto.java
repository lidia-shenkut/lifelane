package com.lifelane.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for recording a dispatcher's accept or override decision.
 *
 * <p>When {@code action} is {@code "override"}, both {@code ambulanceId} and
 * {@code hospitalId} must be provided. Cross-field validation is enforced at
 * the service layer.</p>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DecisionRequestDto {

    @NotBlank(message = "action is required")
    @Schema(description = "Dispatcher action: accept the recommendation or override it",
            example = "accept", allowableValues = {"accept", "override"})
    private String action;

    @Schema(description = "ID of the ambulance to dispatch (required when action is 'override')",
            example = "5")
    private Long ambulanceId;

    @Schema(description = "ID of the hospital to use (required when action is 'override')",
            example = "2")
    private Long hospitalId;

    @Schema(description = "Optional free-text comment from the dispatcher",
            example = "Closer unit available")
    private String comment;
}
