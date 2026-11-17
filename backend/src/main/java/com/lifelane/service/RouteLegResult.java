package com.lifelane.service;

/** Result for one leg of a dispatch route. */
public record RouteLegResult(String leg, int durationSeconds, String geometry) {
}