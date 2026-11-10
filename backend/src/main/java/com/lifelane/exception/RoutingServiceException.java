package com.lifelane.exception;

/**
 * Thrown when the OSRM routing service returns an error or is unreachable.
 *
 * <p>Mapped to HTTP 502 Bad Gateway by the {@link GlobalExceptionHandler}.</p>
 */
public class RoutingServiceException extends RuntimeException {

    public RoutingServiceException(String message) {
        super(message);
    }
}
