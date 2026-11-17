package com.lifelane.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lifelane.config.OsrmProperties;
import com.lifelane.exception.RoutingServiceException;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OsrmRoutingClientTest {

    private HttpServer server;
    private OsrmRoutingClientImpl client;
    private final AtomicReference<String> responseBody = new AtomicReference<>();
    private final AtomicReference<Integer> responseStatus = new AtomicReference<>(200);
    private final AtomicReference<String> requestPath = new AtomicReference<>();
    private final AtomicInteger failFirstAttempts = new AtomicInteger();
    private final AtomicInteger requestCount = new AtomicInteger();
    private final AtomicInteger responseDelayMillis = new AtomicInteger();

    @BeforeEach
    void setUp() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/route/v1/driving/", exchange -> {
            requestPath.set(exchange.getRequestURI().toString());
            requestCount.incrementAndGet();
            boolean shouldFail = failFirstAttempts.getAndUpdate(value -> Math.max(0, value - 1)) > 0;
            int status = shouldFail ? 503 : responseStatus.get();
            String bodyText = shouldFail ? "temporary upstream error" : responseBody.get();
            try {
                Thread.sleep(responseDelayMillis.get());
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                exchange.close();
                return;
            }
            byte[] body = bodyText.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(status, body.length);
            try (var output = exchange.getResponseBody()) {
                output.write(body);
            }
        });
        server.start();

        OsrmProperties properties = new OsrmProperties();
        properties.setBaseUrl("http://127.0.0.1:" + server.getAddress().getPort());
        properties.setTimeoutSeconds(2);
        properties.setRetryCount(2);
        client = new OsrmRoutingClientImpl(properties, new ObjectMapper());
    }

    @AfterEach
    void tearDown() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void parsesRouteAndUsesOsrmLongitudeLatitudeCoordinateOrder() {
        responseBody.set("{\"code\":\"Ok\",\"routes\":[{\"duration\":42.4,"
                + "\"geometry\":{\"type\":\"LineString\",\"coordinates\":[[38.7,9.0],"
                + "[38.71,9.01]]}}]}");

        List<OsrmRoute> routes = client.route(new Coordinate(9.0, 38.7), new Coordinate(9.01, 38.71));

        assertEquals(1, routes.size());
        assertEquals(42.4, routes.get(0).durationSeconds());
        assertEquals(List.of(List.of(38.7, 9.0), List.of(38.71, 9.01)), routes.get(0).coordinates());
        assertTrue(requestPath.get().startsWith(
                "/route/v1/driving/38.7000000,9.0000000;38.7100000,9.0100000?"));
        assertTrue(requestPath.get().contains("geometries=geojson"));
    }

    @Test
    void convertsHttpErrorsToRoutingServiceException() {
        responseStatus.set(503);
        responseBody.set("upstream error");

        RoutingServiceException exception = assertThrows(RoutingServiceException.class,
                () -> client.route(new Coordinate(9.0, 38.7), new Coordinate(9.01, 38.71)));

        assertTrue(exception.getMessage().contains("503"));
    }

    @Test
    void retriesTransientServerErrorsBeforeReturningRoute() {
        failFirstAttempts.set(1);
        responseBody.set("{\"code\":\"Ok\",\"routes\":[{\"duration\":18,"
                + "\"geometry\":{\"type\":\"LineString\",\"coordinates\":[[38.7,9.0],"
                + "[38.71,9.01]]}}]}");

        List<OsrmRoute> routes = client.route(new Coordinate(9.0, 38.7), new Coordinate(9.01, 38.71));

        assertEquals(1, routes.size());
        assertEquals(2, requestCount.get());
    }

    @Test
    void convertsRequestTimeoutToRoutingServiceException() {
        responseDelayMillis.set(1500);
        responseBody.set("{\"code\":\"Ok\",\"routes\":[{\"duration\":18,"
                + "\"geometry\":{\"type\":\"LineString\",\"coordinates\":[[38.7,9.0],"
                + "[38.71,9.01]]}}]}");
        OsrmProperties properties = new OsrmProperties();
        properties.setBaseUrl("http://127.0.0.1:" + server.getAddress().getPort());
        properties.setTimeoutSeconds(1);
        properties.setRetryCount(0);
        client = new OsrmRoutingClientImpl(properties, new ObjectMapper());

        assertThrows(RoutingServiceException.class,
                () -> client.route(new Coordinate(9.0, 38.7), new Coordinate(9.01, 38.71)));
    }

    @Test
    void rejectsMalformedJsonAndOsrmErrorResponse() {
        responseBody.set("{not json");
        assertThrows(RoutingServiceException.class,
                () -> client.route(new Coordinate(9.0, 38.7), new Coordinate(9.01, 38.71)));

        responseBody.set("{\"code\":\"NoRoute\"}");
        assertThrows(RoutingServiceException.class,
                () -> client.route(new Coordinate(9.0, 38.7), new Coordinate(9.01, 38.71)));
    }
}
