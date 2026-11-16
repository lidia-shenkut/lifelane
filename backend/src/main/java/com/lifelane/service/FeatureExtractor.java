package com.lifelane.service;

import com.lifelane.model.EmergencyRequest;
import com.lifelane.model.RoadEvent;
import org.springframework.stereotype.Component;

import java.time.LocalTime;
import java.util.List;

/**
 * Extracts a {@link FeatureVector} from a computed route, emergency request, and active road events.
 */
@Component
public class FeatureExtractor {

    public FeatureVector extract(RouteResult route, EmergencyRequest request, List<RoadEvent> activeEvents) {
        // Calculate cumulative delay from active events as traffic level indicator
        double trafficLevel = 0.0;
        if (activeEvents != null) {
            trafficLevel = activeEvents.stream()
                    .mapToDouble(e -> e.getDelaySeconds() != null ? e.getDelaySeconds() : 0.0)
                    .sum();
        }

        // Approximate route distance from duration (assuming ~11 m/s ~ 40 km/h average) or OSRM geometry length
        double distanceMeters = route.totalDurationSeconds() * 11.1;

        int timeOfDayMinutes = LocalTime.now().getHour() * 60 + LocalTime.now().getMinute();

        int urgency = 1;
        if (request != null && request.getUrgencyLevel() != null) {
            try {
                urgency = Integer.parseInt(request.getUrgencyLevel().trim());
            } catch (NumberFormatException ignored) {
                urgency = 2;
            }
        }

        int roadType = 1; // Default primary/secondary road encoding

        return new FeatureVector(distanceMeters, roadType, timeOfDayMinutes, trafficLevel, urgency);
    }
}
