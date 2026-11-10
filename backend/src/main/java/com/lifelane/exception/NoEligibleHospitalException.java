package com.lifelane.exception;

/**
 * Thrown when the recommendation engine finds no hospitals that can handle
 * the required care type for the given emergency request.
 *
 * <p>Mapped to HTTP 422 Unprocessable Entity by the
 * {@link GlobalExceptionHandler}.</p>
 */
public class NoEligibleHospitalException extends RuntimeException {

    public NoEligibleHospitalException(String message) {
        super(message);
    }
}
