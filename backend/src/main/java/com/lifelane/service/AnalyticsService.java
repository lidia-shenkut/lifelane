package com.lifelane.service;

import com.lifelane.dto.response.AnalyticsSummaryDto;

/**
 * Service interface for computing aggregate analytics over recorded decisions.
 *
 * <p>Implemented in Phase 4 (task 4.8). A concrete bean must be registered
 * for {@link com.lifelane.controller.AnalyticsController} to start.</p>
 */
public interface AnalyticsService {

    /**
     * Computes and returns a summary of system performance metrics.
     *
     * <p>When no decisions have been recorded, all numeric fields are zero
     * and {@code overrideRatePercent} is 0.0.</p>
     *
     * @return the analytics summary DTO
     */
    AnalyticsSummaryDto computeSummary();
}
