package com.lifelane.service;

import java.util.List;

/** Complete two-leg route, including road-event-adjusted ETA. */
public record RouteResult(List<RouteLegResult> legs, int totalDurationSeconds) {
}