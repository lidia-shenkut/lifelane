package com.lifelane.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lifelane.config.OsrmProperties;
import com.lifelane.exception.RoutingServiceException;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** HTTP client implementation for the OSRM v5 route service. */
@Component
public class OsrmRoutingClientImpl implements OsrmRoutingClient {

    private final OsrmProperties properties;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    public OsrmRoutingClientImpl(OsrmProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(Math.max(1, properties.getTimeoutSeconds())))
                .build();
    }

    /**
     * Retrieves candidate routes between two points. Coordinates are encoded as OSRM requires:
     * longitude first, latitude second. The route response carries GeoJSON LineString geometry.
     */
    @Override
    public List<OsrmRoute> route(Coordinate from, Coordinate to) {
        String coordinates = String.format(Locale.ROOT, "%s,%s;%s,%s",
                number(from.longitude()), number(from.latitude()),
                number(to.longitude()), number(to.latitude()));
        String baseUrl = properties.getBaseUrl().replaceAll("/+$", "");
        URI uri = URI.create(baseUrl + "/route/v1/driving/" + coordinates
                + "?overview=full&geometries=geojson&steps=true&alternatives=true");
        HttpRequest request = HttpRequest.newBuilder(uri)
                .timeout(Duration.ofSeconds(Math.max(1, properties.getTimeoutSeconds())))
                .header("Accept", "application/json")
                .GET()
                .build();

        int maxAttempts = Math.max(1, properties.getRetryCount() + 1);
        Exception lastFailure = null;
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() >= 500 && attempt < maxAttempts) {
                    lastFailure = new IOException("OSRM returned HTTP " + response.statusCode());
                    continue;
                }
                if (response.statusCode() < 200 || response.statusCode() >= 300) {
                    throw new RoutingServiceException("OSRM returned HTTP " + response.statusCode());
                }
                return parseRoutes(response.body());
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                throw new RoutingServiceException("OSRM request was interrupted");
            } catch (IOException ex) {
                lastFailure = ex;
                if (attempt == maxAttempts) {
                    break;
                }
            }
        }
        throw new RoutingServiceException("OSRM request failed after " + maxAttempts + " attempt(s): "
                + (lastFailure == null ? "upstream server error" : lastFailure.getMessage()));
    }

    private List<OsrmRoute> parseRoutes(String body) {
        try {
            JsonNode root = objectMapper.readTree(body);
            if (!"Ok".equals(root.path("code").asText())) {
                throw new RoutingServiceException("OSRM response code was not Ok: "
                        + root.path("code").asText("missing code"));
            }
            JsonNode routes = root.path("routes");
            if (!routes.isArray() || routes.isEmpty()) {
                throw new RoutingServiceException("OSRM returned no candidate routes");
            }

            List<OsrmRoute> parsed = new ArrayList<>();
            for (JsonNode route : routes) {
                JsonNode duration = route.path("duration");
                JsonNode coordinates = route.path("geometry").path("coordinates");
                if (!duration.isNumber() || !coordinates.isArray() || coordinates.size() < 2) {
                    throw new RoutingServiceException("OSRM route is missing duration or GeoJSON geometry");
                }
                List<List<Double>> points = new ArrayList<>();
                for (JsonNode point : coordinates) {
                    if (!point.isArray() || point.size() < 2
                            || !point.get(0).isNumber() || !point.get(1).isNumber()) {
                        throw new RoutingServiceException("OSRM returned invalid GeoJSON coordinates");
                    }
                    points.add(List.of(point.get(0).asDouble(), point.get(1).asDouble()));
                }
                parsed.add(new OsrmRoute(duration.asDouble(), List.copyOf(points)));
            }
            return List.copyOf(parsed);
        } catch (IOException ex) {
            throw new RoutingServiceException("OSRM returned invalid JSON: " + ex.getMessage());
        }
    }

    private String number(double value) {
        return String.format(Locale.ROOT, "%.7f", value);
    }
}
