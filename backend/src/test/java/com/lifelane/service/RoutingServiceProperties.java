package com.lifelane.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lifelane.model.RoadEvent;
import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.ForAll;
import net.jqwik.api.Label;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RoutingServiceProperties {

    private static final Coordinate START = new Coordinate(0.0, 0.0);
    private static final Coordinate SCENE = new Coordinate(0.01, 0.01);
    private static final Coordinate HOSPITAL = new Coordinate(0.02, 0.02);
    private static final OsrmRoute ROUTE = new OsrmRoute(30.0, List.of(
            List.of(0.0, 0.0), List.of(0.01, 0.01)));

        @Test
        void closurePrefersAnAvailableCandidateThatAvoidsTheEventCentroid() {
        OsrmRoutingClient client = mock(OsrmRoutingClient.class);
        OsrmRoute throughClosure = new OsrmRoute(25.0, List.of(
            List.of(0.0, 0.0), List.of(0.01, 0.01)));
        OsrmRoute aroundClosure = new OsrmRoute(40.0, List.of(
            List.of(0.0, 0.0), List.of(0.01, 0.0), List.of(0.01, 0.01)));
        when(client.route(START, SCENE)).thenReturn(List.of(throughClosure, aroundClosure));
        when(client.route(SCENE, HOSPITAL)).thenReturn(List.of(ROUTE));
        RoutingService service = new RoutingService(client, new ObjectMapper());
        RoadEvent closure = RoadEvent.builder()
            .status("active")
            .eventType("closure")
            .latitude(0.005)
            .longitude(0.005)
            .build();

        RouteResult result = service.computeTwoLegRoute(START, SCENE, HOSPITAL, List.of(closure));

        assertTrue(result.legs().get(0).geometry().contains("0.01,0.0"));
        assertEquals(70, result.totalDurationSeconds());
        }

        @Test
        void eventProximityIncludesSegmentInterior() {
        RoutingService service = new RoutingService(mock(OsrmRoutingClient.class), new ObjectMapper());
        List<List<Double>> line = List.of(List.of(0.0, 0.0), List.of(0.02, 0.0));

        assertTrue(service.routePassesNear(line, 0.0, 0.01, 100.0));
        }

    @Property(tries = 100)
    @Label("Feature: lifelane-mini, Property 8: Road event activation increases or preserves ETA")
    void activatingDelayEventNeverReducesRouteEta(@ForAll("delays") int delaySeconds) {
        OsrmRoutingClient client = mock(OsrmRoutingClient.class);
        when(client.route(START, SCENE)).thenReturn(List.of(ROUTE));
        when(client.route(SCENE, HOSPITAL)).thenReturn(List.of(ROUTE));
        RoutingService service = new RoutingService(client, new ObjectMapper());
        RoadEvent event = delayEvent(delaySeconds);

        int baseline = service.computeTwoLegRoute(START, SCENE, HOSPITAL, List.of()).totalDurationSeconds();
        int adjusted = service.computeTwoLegRoute(START, SCENE, HOSPITAL, List.of(event)).totalDurationSeconds();

        assertTrue(adjusted >= baseline);
        assertEquals(baseline + (2 * delaySeconds), adjusted);
    }

    @Property(tries = 100)
    @Label("Feature: lifelane-mini, Property 9: Road event activate-then-deactivate restores routing behavior")
    void removingDelayEventRestoresBaselineEta(@ForAll("delays") int delaySeconds) {
        OsrmRoutingClient client = mock(OsrmRoutingClient.class);
        when(client.route(START, SCENE)).thenReturn(List.of(ROUTE));
        when(client.route(SCENE, HOSPITAL)).thenReturn(List.of(ROUTE));
        RoutingService service = new RoutingService(client, new ObjectMapper());
        RoadEvent event = delayEvent(delaySeconds);

        int baseline = service.computeTwoLegRoute(START, SCENE, HOSPITAL, List.of()).totalDurationSeconds();
        int activated = service.computeTwoLegRoute(START, SCENE, HOSPITAL, List.of(event)).totalDurationSeconds();
        int deactivated = service.computeTwoLegRoute(START, SCENE, HOSPITAL, List.of()).totalDurationSeconds();

        assertTrue(activated >= baseline);
        assertEquals(baseline, deactivated);
    }

    private RoadEvent delayEvent(int delaySeconds) {
        return RoadEvent.builder()
                .status("active")
                .eventType("delay")
                .delaySeconds(delaySeconds)
                .latitude(0.0)
                .longitude(0.0)
                .build();
    }

    @Provide
    Arbitrary<Integer> delays() {
        return Arbitraries.integers().between(0, 1800);
    }
}