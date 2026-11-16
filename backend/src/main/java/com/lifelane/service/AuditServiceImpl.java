package com.lifelane.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lifelane.model.AuditLog;
import com.lifelane.model.DispatchDecision;
import com.lifelane.model.DispatchRecommendation;
import com.lifelane.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

/** Persists audit entries for generated recommendations and dispatcher decisions. */
@Service
@RequiredArgsConstructor
public class AuditServiceImpl implements AuditService {

    private static final Logger log = LoggerFactory.getLogger(AuditServiceImpl.class);

    private final AuditLogRepository auditLogRepository;
    private final ObjectMapper objectMapper;

    @Override
    public void recordRecommendation(DispatchRecommendation recommendation, Long requestId) {
        AuditLog auditLog = AuditLog.builder()
                .eventType("RECOMMENDATION")
                .emergencyRequestId(requestId)
                .recommendationId(recommendation.getId())
                .payloadJson(serialize(recommendation, "recommendation"))
                .createdAt(OffsetDateTime.now(ZoneOffset.UTC))
                .build();
        persist(auditLog, "recommendation", requestId);
    }

    @Override
    public void recordDecision(DispatchDecision decision) {
        Long requestId = decision.getEmergencyRequest() == null
                ? null
                : decision.getEmergencyRequest().getId();
        AuditLog auditLog = AuditLog.builder()
                .eventType("DECISION")
                .emergencyRequestId(requestId)
                .recommendationId(decision.getRecommendation() == null
                        ? null
                        : decision.getRecommendation().getId())
                .decisionId(decision.getId())
                .payloadJson(serialize(decision, "decision"))
                .createdAt(OffsetDateTime.now(ZoneOffset.UTC))
                .build();
        persist(auditLog, "decision", requestId);
    }

    private String serialize(Object payload, String payloadType) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException ex) {
            log.error("Failed to serialize {} audit payload", payloadType, ex);
            throw new IllegalStateException("Failed to serialize " + payloadType + " audit payload", ex);
        }
    }

    private void persist(AuditLog auditLog, String eventType, Long requestId) {
        try {
            auditLogRepository.save(auditLog);
        } catch (DataAccessException ex) {
            log.error("Failed to persist {} audit event for emergency request {}",
                    eventType, requestId, ex);
            throw ex;
        }
    }
}