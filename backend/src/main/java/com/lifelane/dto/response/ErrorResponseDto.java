package com.lifelane.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Standard error response body returned by the {@code GlobalExceptionHandler}.
 *
 * <p>The {@code errors} list is populated for validation failures (HTTP 400)
 * with per-field details. For all other errors it is an empty list.</p>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ErrorResponseDto {

    @Schema(description = "HTTP status code", example = "400")
    private int status;

    @Schema(description = "Human-readable error summary", example = "Validation failed")
    private String message;

    @Schema(description = "List of field-level validation errors; empty for non-validation errors")
    private List<FieldErrorDto> errors;

    // ── Inner: FieldErrorDto ─────────────────────────────────────────────────

    /**
     * Represents a single field-level validation failure.
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class FieldErrorDto {

        @Schema(description = "Name of the request field that failed validation",
                example = "latitude")
        private String field;

        @Schema(description = "Validation error message for that field",
                example = "must be between -90 and 90")
        private String message;
    }
}
