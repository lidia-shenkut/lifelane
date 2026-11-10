package com.lifelane.exception;

/**
 * Thrown when the recommendation engine finds no ambulances that meet the
 * eligibility criteria for the given emergency request.
 *
 * <p>Mapped to HTTP 422 Unprocessable Entity by the
 * {@link GlobalExceptionHandler}.</p>
 */
public class NoEligibleAmbulanceException extends RuntimeException {

    public NoEligibleAmbulanceException(String message) {
        super(message);
    }
}
