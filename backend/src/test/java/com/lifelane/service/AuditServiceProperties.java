package com.lifelane.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lifelane.model.AuditLog;
import com.lifelane.model.DispatchDecision;
import com.lifelane.model.DispatchRecommendation;
import com.lifelane.model.EmergencyRequest;
import com.lifelane.repository.AuditLogRepository;
import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.ForAll;
import net.jqwik.api.Label;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessResourceFailureException;

import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AuditServiceProperties {

    @Property(tries = 100)
    @Label("Feature: lifelane-mini, Property 12: Audit log captures every recommendation")
    void recommendationAuditContainsRequestAndPayload(
            @ForAll long requestId,
            @ForAll long recommendationId) {
        AuditLogRepository repository = mock(AuditLogRepository.class);
        AuditService service = new AuditServiceImpl(repository, new ObjectMapper());
        DispatchRecommendation recommendation = DispatchRecommendation.builder()
                .id(recommendationId)
                .ambulanceEtaSeconds(37)
                .hospitalEtaSeconds(81)
                .totalEtaSeconds(118)
                .explanation("Property-generated recommendation")
                .build();

        service.recordRecommendation(recommendation, requestId);

        org.mockito.ArgumentCaptor<AuditLog> captor = forClass(AuditLog.class);
        verify(repository).save(captor.capture());
        AuditLog saved = captor.getValue();
        assertEquals("RECOMMENDATION", saved.getEventType());
        assertEquals(requestId, saved.getEmergencyRequestId());
        assertEquals(recommendationId, saved.getRecommendationId());
        assertNotNull(saved.getPayloadJson());
        assertTrue(saved.getPayloadJson().contains("Property-generated recommendation"));
        assertNotNull(saved.getCreatedAt());
        assertEquals(ZoneOffset.UTC, saved.getCreatedAt().getOffset());
    }

    @Property(tries = 100)
    @Label("Feature: lifelane-mini, Property 12: Audit log captures every decision")
    void decisionAuditContainsRequestRecommendationAndPayload(
            @ForAll long requestId,
            @ForAll long recommendationId,
            @ForAll long decisionId,
            @ForAll("actions") String action) {
        AuditLogRepository repository = mock(AuditLogRepository.class);
        AuditService service = new AuditServiceImpl(repository, new ObjectMapper());
        DispatchDecision decision = DispatchDecision.builder()
                .id(decisionId)
                .emergencyRequest(EmergencyRequest.builder().id(requestId).build())
                .recommendation(DispatchRecommendation.builder().id(recommendationId).build())
                .action(action)
                .dispatcherComment("Property-generated decision")
                .build();

        service.recordDecision(decision);

        org.mockito.ArgumentCaptor<AuditLog> captor = forClass(AuditLog.class);
        verify(repository).save(captor.capture());
        AuditLog saved = captor.getValue();
        assertEquals("DECISION", saved.getEventType());
        assertEquals(requestId, saved.getEmergencyRequestId());
        assertEquals(recommendationId, saved.getRecommendationId());
        assertEquals(decisionId, saved.getDecisionId());
        assertNotNull(saved.getPayloadJson());
        assertTrue(saved.getPayloadJson().contains(action));
        assertTrue(saved.getPayloadJson().contains("Property-generated decision"));
        assertNotNull(saved.getCreatedAt());
        assertEquals(ZoneOffset.UTC, saved.getCreatedAt().getOffset());
    }

    @Test
    void repositoryFailureIsLoggedByServiceAndPropagated() {
        AuditLogRepository repository = mock(AuditLogRepository.class);
        DataAccessResourceFailureException failure =
                new DataAccessResourceFailureException("database unavailable");
        when(repository.save(any(AuditLog.class))).thenThrow(failure);
        AuditService service = new AuditServiceImpl(repository, new ObjectMapper());
        DispatchRecommendation recommendation = DispatchRecommendation.builder()
                .id(11L)
                .build();

        assertThrows(DataAccessResourceFailureException.class,
                () -> service.recordRecommendation(recommendation, 7L));
    }

        @Provide
        Arbitrary<String> actions() {
        return Arbitraries.of("accept", "override");
    }
}