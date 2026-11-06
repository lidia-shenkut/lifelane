package com.lifelane.controller;

import com.lifelane.dto.request.RoadEventPatchDto;
import com.lifelane.model.RoadEvent;
import com.lifelane.service.RoadEventService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;

/**
 * REST controller for toggling road-event simulation states.
 *
 * <p>Endpoint: {@code PATCH /api/road-events/{id}}</p>
 */
@RestController
@RequestMapping("/api/road-events")
@RequiredArgsConstructor
@Tag(name = "Road Events", description = "Traffic scenario simulation toggles")
public class RoadEventController {

    private final RoadEventService roadEventService;

    /**
     * Activates or deactivates a road event by ID.
     *
     * @param id  the road event ID
     * @param dto the patch payload containing the new status
     * @throws java.util.NoSuchElementException if the road event is not found → 404
     */
    @Operation(summary = "Toggle road event status",
               description = "Sets a road event to 'active' or 'inactive', affecting subsequent routing queries.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Road event status updated"),
        @ApiResponse(responseCode = "400", description = "Validation error"),
        @ApiResponse(responseCode = "404", description = "Road event not found")
    })
    @PatchMapping("/{id}")
    public ResponseEntity<RoadEventResponseDto> patchRoadEvent(
            @PathVariable Long id,
            @Valid @RequestBody RoadEventPatchDto dto) {

        RoadEvent updated = roadEventService.setStatus(id, dto.getStatus());

        RoadEventResponseDto response = RoadEventResponseDto.builder()
                .id(updated.getId())
                .name(updated.getName())
                .status(updated.getStatus())
                .delaySeconds(updated.getDelaySeconds())
                .eventType(updated.getEventType())
                .latitude(updated.getLatitude())
                .longitude(updated.getLongitude())
                .updatedAt(updated.getUpdatedAt())
                .build();

        return ResponseEntity.ok(response);
    }

    // ── Inner response DTO ───────────────────────────────────────────────────

    @lombok.Value
    @lombok.Builder
    @lombok.AllArgsConstructor
    static class RoadEventResponseDto {
        Long id;
        String name;
        String status;
        Integer delaySeconds;
        String eventType;
        Double latitude;
        Double longitude;
        OffsetDateTime updatedAt;
    }
}
