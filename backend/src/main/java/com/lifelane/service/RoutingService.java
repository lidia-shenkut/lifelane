package com.lifelane.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lifelane.exception.RoutingServiceException;
import com.lifelane.model.RoadEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;

/** Computes two-leg routes and applies active road-event effects to ETA. */
@Service
@RequiredArgsConstructor
public class RoutingService {

    private static final String DELAY_TYPE = "delay";
    private static final String CLOSURE_TYPE = "closure";
    private static final double EVENT_RADIUS_METERS = 100.0;

    private final OsrmRoutingClient osrmRoutingClient;
    private final ObjectMapper objectMapper;

    public RouteResult computeTwoLegRoute(Coordinate ambulanceLoc,
                                          Coordinate incidentLoc,
                                          Coordinate hospitalLoc,
                                          List<RoadEvent> activeEvents) {
        List<RoadEvent> events = activeEvents == null ? List.of() : activeEvents.stream()
                .filter(event -> "active".equalsIgnoreCase(event.getStatus()))
                .toList();
        RouteLegResult firstLeg = computeLeg(ambulanceLoc, incidentLoc, "ambulance_to_scene", events);
        RouteLegResult secondLeg = computeLeg(incidentLoc, hospitalLoc, "scene_to_hospital", events);
        long total = (long) firstLeg.durationSeconds() + secondLeg.durationSeconds();
        if (total > Integer.MAX_VALUE) {
            throw new RoutingServiceException("Adjusted route duration exceeds supported range");
        }
        return new RouteResult(List.of(firstLeg, secondLeg), (int) total);
    }

    private RouteLegResult computeLeg(Coordinate from,
                                      Coordinate to,
                                      String legName,
                                      List<RoadEvent> events) {
        List<OsrmRoute> candidates = osrmRoutingClient.route(from, to);
        if (candidates == null || candidates.isEmpty()) {
            throw new RoutingServiceException("OSRM returned no route for " + legName);
        }

        // OSRM route coordinates are [longitude, latitude]. Never insert a closure centroid as a
        // mandatory waypoint: that would route through the closure, rather than around it.
        OsrmRoute selected = candidates.stream()
                .min(Comparator.comparingInt((OsrmRoute route) -> closureConflictCount(route, events))
                        .thenComparingDouble(OsrmRoute::durationSeconds))
                .orElseThrow(() -> new RoutingServiceException("OSRM returned no route for " + legName));

        long adjustedSeconds = Math.round(selected.durationSeconds());
        for (RoadEvent event : events) {
            if (DELAY_TYPE.equalsIgnoreCase(event.getEventType())
                    && event.getLatitude() != null
                    && event.getLongitude() != null
                    && routePassesNear(selected.coordinates(), event.getLatitude(),
                    event.getLongitude(), EVENT_RADIUS_METERS)) {
                adjustedSeconds += Math.max(0, event.getDelaySeconds() == null ? 0 : event.getDelaySeconds());
            }
        }
        if (adjustedSeconds > Integer.MAX_VALUE) {
            throw new RoutingServiceException("Adjusted route duration exceeds supported range");
        }
        return new RouteLegResult(legName, (int) adjustedSeconds, toGeoJson(selected.coordinates()));
    }

    private int closureConflictCount(OsrmRoute route, List<RoadEvent> events) {
        int conflicts = 0;
        for (RoadEvent event : events) {
            if (CLOSURE_TYPE.equalsIgnoreCase(event.getEventType())
                    && event.getLatitude() != null
                    && event.getLongitude() != null
                    && routePassesNear(route.coordinates(), event.getLatitude(),
                    event.getLongitude(), EVENT_RADIUS_METERS)) {
                conflicts++;
            }
        }
        return conflicts;
    }

    /** Checks route proximity to an event centroid using a local equirectangular distance. */
    boolean routePassesNear(List<List<Double>> coordinates,
                            double latitude,
                            double longitude,
                            double thresholdMeters) {
        if (coordinates == null || coordinates.isEmpty()) {
            return false;
        }
        List<Double> previous = coordinates.get(0);
        if (distanceFromEvent(previous, latitude, longitude) <= thresholdMeters) {
            return true;
        }
        for (int index = 1; index < coordinates.size(); index++) {
            List<Double> current = coordinates.get(index);
            if (distanceFromEvent(current, latitude, longitude) <= thresholdMeters
                    || distanceToSegmentMeters(previous, current, latitude, longitude) <= thresholdMeters) {
                return true;
            }
            previous = current;
        }
        return false;
    }

    private double distanceFromEvent(List<Double> point, double latitude, double longitude) {
        if (point.size() < 2) {
            return Double.POSITIVE_INFINITY;
        }
        double[] meters = toLocalMeters(point.get(1), point.get(0), latitude, longitude);
        return Math.hypot(meters[0], meters[1]);
    }

    private double distanceToSegmentMeters(List<Double> start,
                                           List<Double> end,
                                           double latitude,
                                           double longitude) {
        if (start.size() < 2 || end.size() < 2) {
            return Double.POSITIVE_INFINITY;
        }
        double[] a = toLocalMeters(start.get(1), start.get(0), latitude, longitude);
        double[] b = toLocalMeters(end.get(1), end.get(0), latitude, longitude);
        double dx = b[0] - a[0];
        double dy = b[1] - a[1];
        double segmentLengthSquared = dx * dx + dy * dy;
        if (segmentLengthSquared == 0.0) {
            return Math.hypot(a[0], a[1]);
        }
        double projection = Math.max(0.0, Math.min(1.0,
                -(a[0] * dx + a[1] * dy) / segmentLengthSquared));
        return Math.hypot(a[0] + projection * dx, a[1] + projection * dy);
    }

    /** Converts a point to local east/north meters relative to the event centroid. */
    private double[] toLocalMeters(double pointLatitude,
                                   double pointLongitude,
                                   double originLatitude,
                                   double originLongitude) {
        double meanLatitude = Math.toRadians((pointLatitude + originLatitude) / 2.0);
        double north = Math.toRadians(pointLatitude - originLatitude) * 6_371_000.0;
        double east = Math.toRadians(pointLongitude - originLongitude)
                * 6_371_000.0 * Math.cos(meanLatitude);
        return new double[]{east, north};
    }

    private String toGeoJson(List<List<Double>> coordinates) {
        try {
            return objectMapper.writeValueAsString(new GeoJsonLineString("LineString", coordinates));
        } catch (JsonProcessingException ex) {
            throw new RoutingServiceException("Could not serialize route geometry: " + ex.getMessage());
        }
    }

    private record GeoJsonLineString(String type, List<List<Double>> coordinates) {
    }
}