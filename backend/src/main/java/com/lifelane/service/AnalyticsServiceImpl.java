package com.lifelane.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lifelane.config.MlFeatureProperties;
import com.lifelane.dto.response.AnalyticsSummaryDto;
import com.lifelane.model.AuditLog;
import com.lifelane.model.DispatchRecommendation;
import com.lifelane.repository.AuditLogRepository;
import com.lifelane.repository.DispatchRecommendationRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Computes dispatch analytics exclusively from persisted audit entries and recommendations. */
@Service
@RequiredArgsConstructor
public class AnalyticsServiceImpl implements AnalyticsService {

    private static final Logger log = LoggerFactory.getLogger(AnalyticsServiceImpl.class);
    private static final String RECOMMENDATION_EVENT = "RECOMMENDATION";
    private static final String DECISION_EVENT = "DECISION";
    private static final String UNKNOWN_SCENARIO = "unknown";

    private final AuditLogRepository auditLogRepository;
    private final DispatchRecommendationRepository recommendationRepository;
    private final MlFeatureProperties mlFeatureProperties;
    private final ObjectMapper objectMapper;

    @Override
    public AnalyticsSummaryDto computeSummary() {
        List<AuditLog> auditLogs = auditLogRepository.findAll();
        Set<Long> requestIds = new HashSet<>();
        Map<Long, RecommendationMetrics> recommendationsById = new HashMap<>();
        Map<String, Long> recommendationsPerScenario = new HashMap<>();
        for (AuditLog auditLog : auditLogs) {
            if (auditLog.getEmergencyRequestId() != null) {
                requestIds.add(auditLog.getEmergencyRequestId());
            }

            JsonNode payload = parsePayload(auditLog);
            if (RECOMMENDATION_EVENT.equalsIgnoreCase(auditLog.getEventType())) {
                String scenario = textOr(payload, "roadEventScenario", UNKNOWN_SCENARIO);
                recommendationsPerScenario.merge(scenario, 1L, Long::sum);
                if (auditLog.getRecommendationId() != null) {
                    JsonNode eta = payload.path("totalEtaSeconds");
                    if (eta.isNumber()) {
                        recommendationsById.put(auditLog.getRecommendationId(),
                            new RecommendationMetrics(eta.asDouble()));
                    }
                }
            }
        }

        long decisionCount = 0;
        long overrideCount = 0;
        double acceptedEtaTotal = 0.0;
        long acceptedRecommendationCount = 0;
        for (AuditLog auditLog : auditLogs) {
            if (!DECISION_EVENT.equalsIgnoreCase(auditLog.getEventType())) {
                continue;
            }
            decisionCount++;
            JsonNode payload = parsePayload(auditLog);
            String action = textOr(payload, "action", "");
            if ("override".equalsIgnoreCase(action)) {
                overrideCount++;
            } else if ("accept".equalsIgnoreCase(action)) {
                RecommendationMetrics metrics = recommendationsById.get(auditLog.getRecommendationId());
                if (metrics != null) {
                    acceptedEtaTotal += metrics.totalEtaSeconds();
                    acceptedRecommendationCount++;
                }
            }
        }

        Object mlMetrics = computeMlMetrics();

        if (decisionCount == 0) {
            return zeroSummary(mlMetrics);
        }

        double averageAcceptedEta = acceptedRecommendationCount == 0
                ? 0.0
                : acceptedEtaTotal / acceptedRecommendationCount;
        double overrideRate = decisionCount == 0
                ? 0.0
                : (overrideCount * 100.0) / decisionCount;

        return AnalyticsSummaryDto.builder()
                .totalEmergencyRequests(requestIds.size())
                .averageTotalEtaSeconds(averageAcceptedEta)
                .overrideRatePercent(overrideRate)
                .recommendationsPerScenario(Map.copyOf(recommendationsPerScenario))
                .mlMetrics(mlMetrics)
                .build();
    }

    private Object computeMlMetrics() {
        if (mlFeatureProperties == null || !mlFeatureProperties.isEnabled()) {
            return null;
        }

        List<DispatchRecommendation> recommendations = recommendationRepository.findAll();
        if (recommendations.isEmpty()) {
            return Map.of(
                    "meanAbsoluteError", 0.0,
                    "baselineOsrmMae", 0.0,
                    "improvementPercent", 0.0,
                    "samplesCount", 0
            );
        }

        double totalDiff = 0.0;
        double baselineDiff = 0.0;
        int count = 0;

        for (DispatchRecommendation dr : recommendations) {
            if (dr.getTotalEtaSeconds() != null && dr.getAmbulanceEtaSeconds() != null && dr.getHospitalEtaSeconds() != null) {
                int osrmBaseline = dr.getAmbulanceEtaSeconds() + dr.getHospitalEtaSeconds();
                int mlPredicted = dr.getTotalEtaSeconds();
                // Measure MAE against OSRM baseline
                totalDiff += Math.abs(mlPredicted - osrmBaseline);
                baselineDiff += osrmBaseline;
                count++;
            }
        }

        if (count == 0) {
            return Map.of(
                    "meanAbsoluteError", 0.0,
                    "baselineOsrmMae", 0.0,
                    "improvementPercent", 0.0,
                    "samplesCount", 0
            );
        }

        double mae = totalDiff / count;
        double baselineMae = baselineDiff / count;
        double improvement = baselineMae > 0.0 ? ((baselineMae - mae) / baselineMae) * 100.0 : 0.0;

        return Map.of(
                "meanAbsoluteError", Math.round(mae * 10.0) / 10.0,
                "baselineOsrmMae", Math.round(baselineMae * 10.0) / 10.0,
                "improvementPercent", Math.round(improvement * 10.0) / 10.0,
                "samplesCount", count
        );
    }

    private AnalyticsSummaryDto zeroSummary(Object mlMetrics) {
        return AnalyticsSummaryDto.builder()
                .totalEmergencyRequests(0)
                .averageTotalEtaSeconds(0.0)
                .overrideRatePercent(0.0)
                .recommendationsPerScenario(Map.of())
                .mlMetrics(mlMetrics)
                .build();
    }

    private JsonNode parsePayload(AuditLog auditLog) {
        try {
            return objectMapper.readTree(auditLog.getPayloadJson());
        } catch (Exception ex) {
            log.warn("Ignoring malformed audit payload for event {} (audit log {})",
                    auditLog.getEventType(), auditLog.getId(), ex);
            return objectMapper.createObjectNode();
        }
    }

    private String textOr(JsonNode node, String field, String fallback) {
        JsonNode value = node.path(field);
        return value.isTextual() && !value.asText().isBlank() ? value.asText() : fallback;
    }

    private record RecommendationMetrics(double totalEtaSeconds) {
    }
}