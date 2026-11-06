package com.lifelane.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Externalised configuration for the OSRM routing service client.
 *
 * <p>Bound from the {@code osrm.*} keys in {@code application.yml}.
 * Defaults mirror the local-dev setup (localhost:5000, 5 s timeout, 2 retries).
 */
@Data
@Component
@ConfigurationProperties(prefix = "osrm")
public class OsrmProperties {

    /** Base URL of the OSRM HTTP API, e.g. {@code http://localhost:5000}. */
    private String baseUrl;

    /** HTTP read timeout in seconds for each OSRM request. */
    private int timeoutSeconds = 5;

    /** Number of times a failed OSRM call is retried before propagating the error. */
    private int retryCount = 2;
}
