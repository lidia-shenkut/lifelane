package com.lifelane.service;

import java.util.List;

/**
 * Normalized feature vector extracted for ML travel-time prediction.
 *
 * <p>Features:
 * <ul>
 *   <li>distance_meters: total route distance in meters</li>
 *   <li>road_type: encoded integer road classification</li>
 *   <li>time_of_day_minutes: minute of day (0-1439)</li>
 *   <li>traffic_level: cumulative delay seconds from active road events</li>
 *   <li>urgency_level: numeric encoded urgency from EmergencyRequest</li>
 * </ul>
 */
public record FeatureVector(
        double distanceMeters,
        int roadType,
        int timeOfDayMinutes,
        double trafficLevel,
        int urgencyLevel
) {
    public List<Number> asFeatureList() {
        return List.of(distanceMeters, roadType, timeOfDayMinutes, trafficLevel, urgencyLevel);
    }
}
