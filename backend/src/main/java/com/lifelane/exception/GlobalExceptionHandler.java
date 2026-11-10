package com.lifelane.exception;

import com.lifelane.dto.response.ErrorResponseDto;
import com.lifelane.dto.response.ErrorResponseDto.FieldErrorDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

/**
 * Centralised exception → HTTP response mapping for all REST controllers.
 *
 * <p>Rules:
 * <ul>
 *   <li>Stack traces are NEVER returned to the client.</li>
 *   <li>All error bodies use {@link ErrorResponseDto}.</li>
 *   <li>The {@code errors} list is populated only for validation failures.</li>
 * </ul>
 * </p>
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    // ── Validation failure: 400 ──────────────────────────────────────────────

    /**
     * Handles Bean Validation failures from {@code @Valid} / {@code @Validated}
     * annotated controller parameters. Returns a 400 response with per-field
     * error detail.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponseDto> handleValidation(MethodArgumentNotValidException ex) {
        List<FieldErrorDto> fieldErrors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(fe -> FieldErrorDto.builder()
                        .field(fe.getField())
                        .message(fe.getDefaultMessage())
                        .build())
                .collect(Collectors.toList());

        ErrorResponseDto body = ErrorResponseDto.builder()
                .status(HttpStatus.BAD_REQUEST.value())
                .message("Validation failed")
                .errors(fieldErrors)
                .build();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    // ── Not found: 404 ──────────────────────────────────────────────────────

    /**
     * Handles {@link NoSuchElementException} thrown by services or JPA
     * {@code findById(...).orElseThrow()} calls.
     */
    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<ErrorResponseDto> handleNotFound(NoSuchElementException ex) {
        ErrorResponseDto body = ErrorResponseDto.builder()
                .status(HttpStatus.NOT_FOUND.value())
                .message(ex.getMessage())
                .errors(Collections.emptyList())
                .build();

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(body);
    }

    // ── Unprocessable entity: 422 ────────────────────────────────────────────

    /**
     * Handles the case where the recommendation engine finds no eligible
     * ambulances for the given emergency request.
     */
    @ExceptionHandler(NoEligibleAmbulanceException.class)
    public ResponseEntity<ErrorResponseDto> handleNoAmbulance(NoEligibleAmbulanceException ex) {
        ErrorResponseDto body = ErrorResponseDto.builder()
                .status(HttpStatus.UNPROCESSABLE_ENTITY.value())
                .message(ex.getMessage())
                .errors(Collections.emptyList())
                .build();

        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(body);
    }

    /**
     * Handles the case where the recommendation engine finds no eligible
     * hospitals for the given emergency request.
     */
    @ExceptionHandler(NoEligibleHospitalException.class)
    public ResponseEntity<ErrorResponseDto> handleNoHospital(NoEligibleHospitalException ex) {
        ErrorResponseDto body = ErrorResponseDto.builder()
                .status(HttpStatus.UNPROCESSABLE_ENTITY.value())
                .message(ex.getMessage())
                .errors(Collections.emptyList())
                .build();

        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(body);
    }

    // ── Conflict: 409 ────────────────────────────────────────────────────────

    /**
     * Handles the case where a dispatcher attempts to record a second decision
     * for an emergency request that already has one.
     */
    @ExceptionHandler(DuplicateDecisionException.class)
    public ResponseEntity<ErrorResponseDto> handleDuplicateDecision(DuplicateDecisionException ex) {
        ErrorResponseDto body = ErrorResponseDto.builder()
                .status(HttpStatus.CONFLICT.value())
                .message(ex.getMessage())
                .errors(Collections.emptyList())
                .build();

        return ResponseEntity.status(HttpStatus.CONFLICT).body(body);
    }

    // ── Bad gateway: 502 ─────────────────────────────────────────────────────

    /**
     * Handles failures communicating with the upstream OSRM routing service.
     */
    @ExceptionHandler(RoutingServiceException.class)
    public ResponseEntity<ErrorResponseDto> handleRoutingService(RoutingServiceException ex) {
        ErrorResponseDto body = ErrorResponseDto.builder()
                .status(HttpStatus.BAD_GATEWAY.value())
                .message(ex.getMessage())
                .errors(Collections.emptyList())
                .build();

        return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(body);
    }

    // ── Catch-all: 500 ───────────────────────────────────────────────────────

    /**
     * Catch-all handler for any unexpected exception. Logs the full stack
     * trace server-side but returns only a generic message to the client so
     * that internal implementation details are never exposed.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponseDto> handleGeneric(Exception ex) {
        log.error("Unhandled exception", ex);

        ErrorResponseDto body = ErrorResponseDto.builder()
                .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .message("Internal server error")
                .errors(Collections.emptyList())
                .build();

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
    }
}
