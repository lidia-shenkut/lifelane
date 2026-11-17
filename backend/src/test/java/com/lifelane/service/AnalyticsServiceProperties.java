package com.lifelane.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lifelane.dto.response.AnalyticsSummaryDto;
import com.lifelane.model.AuditLog;
import com.lifelane.repository.AuditLogRepository;
import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.ForAll;
import net.jqwik.api.Label;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AnalyticsServiceProperties {

    @Property(tries = 100)
    @Label("Feature: lifelane-mini, Property 13: Analytics metrics are consistent with recorded decisions")
    void metricsMatchRecordedDecisionAuditEntries(
            @ForAll("decisionCounts") int requestCount,
            @ForAll("decisionCounts") int generatedOverrideCount,
            @ForAll int etaSeed) {
        int overrideCount = requestCount == 0
                ? 0
                : Math.floorMod(generatedOverrideCount, requestCount + 1);
        List<AuditLog> auditLogs = new ArrayList<>();
        double acceptedEtaSum = 0;
        int acceptedCount = 0;

        for (int index = 0; index < requestCount; index++) {
            long requestId = index + 1L;
            long recommendationId = index + 101L;
            int eta = Math.floorMod(etaSeed + index * 17, 1800);
            String scenario = index % 2 == 0 ? "no_road_events" : "ring_road_closure";
            String recommendationPayload = "{\"totalEtaSeconds\":" + eta
                    + ",\"roadEventScenario\":\"" + scenario + "\"}";
            auditLogs.add(AuditLog.builder()
                    .eventType("RECOMMENDATION")
                    .emergencyRequestId(requestId)
                    .recommendationId(recommendationId)
                    .payloadJson(recommendationPayload)
                    .build());

            boolean overridden = index < overrideCount;
            String action = overridden ? "override" : "accept";
            auditLogs.add(AuditLog.builder()
                    .eventType("DECISION")
                    .emergencyRequestId(requestId)
                    .recommendationId(recommendationId)
                    .decisionId(index + 201L)
                    .payloadJson("{\"action\":\"" + action + "\"}")
                    .build());
            if (!overridden) {
                acceptedEtaSum += eta;
                acceptedCount++;
            }
        }

        AuditLogRepository repository = mock(AuditLogRepository.class);
        when(repository.findAll()).thenReturn(auditLogs);
        com.lifelane.repository.DispatchRecommendationRepository recRepo = mock(com.lifelane.repository.DispatchRecommendationRepository.class);
        com.lifelane.config.MlFeatureProperties mlProps = new com.lifelane.config.MlFeatureProperties();
        mlProps.setEnabled(false);
        AnalyticsSummaryDto actual = new AnalyticsServiceImpl(repository, recRepo, mlProps, new ObjectMapper()).computeSummary();

        assertEquals(requestCount, actual.getTotalEmergencyRequests());
        assertEquals(requestCount == 0 ? 0.0 : overrideCount * 100.0 / requestCount,
                actual.getOverrideRatePercent(), 0.0001);
        assertEquals(acceptedCount == 0 ? 0.0 : acceptedEtaSum / acceptedCount,
                actual.getAverageTotalEtaSeconds(), 0.0001);
        Map<String, Long> expectedScenarios = new java.util.HashMap<>();
        if (requestCount > 0) {
            expectedScenarios.put("no_road_events", (requestCount + 1L) / 2L);
            if (requestCount > 1) {
                expectedScenarios.put("ring_road_closure", requestCount / 2L);
            }
        }
        assertEquals(expectedScenarios, actual.getRecommendationsPerScenario());
    }

    @Test
    void emptyAuditLogReturnsZeroMetrics() {
        AuditLogRepository repository = mock(AuditLogRepository.class);
        when(repository.findAll()).thenReturn(List.of());
        com.lifelane.repository.DispatchRecommendationRepository recRepo = mock(com.lifelane.repository.DispatchRecommendationRepository.class);
        com.lifelane.config.MlFeatureProperties mlProps = new com.lifelane.config.MlFeatureProperties();
        mlProps.setEnabled(false);

        AnalyticsSummaryDto summary = new AnalyticsServiceImpl(repository, recRepo, mlProps, new ObjectMapper()).computeSummary();

        assertEquals(0, summary.getTotalEmergencyRequests());
        assertEquals(0.0, summary.getAverageTotalEtaSeconds());
        assertEquals(0.0, summary.getOverrideRatePercent());
        assertEquals(Map.of(), summary.getRecommendationsPerScenario());
                assertNull(summary.getMlMetrics());
    }

        @Provide
        Arbitrary<Integer> decisionCounts() {
        return Arbitraries.integers().between(0, 40);
    }
}