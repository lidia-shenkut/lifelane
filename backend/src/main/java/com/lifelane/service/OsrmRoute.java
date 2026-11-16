package com.lifelane.service;

import java.util.List;

/** A normalized candidate route returned by OSRM. GeoJSON coordinate order is [longitude, latitude]. */
public record OsrmRoute(double durationSeconds, List<List<Double>> coordinates) {
}