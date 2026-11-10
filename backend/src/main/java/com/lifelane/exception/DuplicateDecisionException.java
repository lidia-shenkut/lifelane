package com.lifelane.exception;

/**
 * Thrown when a dispatcher attempts to record a second decision for an
 * emergency request that already has one.
 *
 * <p>Mapped to HTTP 409 Conflict by the {@link GlobalExceptionHandler}.</p>
 */
public class DuplicateDecisionException extends RuntimeException {

    public DuplicateDecisionException(String message) {
        super(message);
    }
}
