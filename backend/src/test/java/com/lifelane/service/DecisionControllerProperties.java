package com.lifelane.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lifelane.dto.request.DecisionRequestDto;
import com.lifelane.dto.request.EmergencyRequestDto;
import com.lifelane.repository.DispatchDecisionRepository;
import com.lifelane.repository.DispatchRecommendationRepository;
import com.lifelane.repository.EmergencyRequestRepository;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.Combinators;
import net.jqwik.api.ForAll;
import net.jqwik.api.Label;
import net.jqwik.api.Provide;
import net.jqwik.api.Property;
import net.jqwik.api.Arbitraries;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Property‑based test for the one‑decision‑per‑request constraint (Property 14).
 */
@ExtendWith(SpringExtension.class)
@SpringBootTest
@AutoConfigureMockMvc
class DecisionControllerProperties {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper mapper;
    @Autowired private EmergencyRequestRepository requestRepo;
    @Autowired private DispatchRecommendationRepository recRepo;
    @Autowired private DispatchDecisionRepository decisionRepo;

    @BeforeEach
    void clean() {
        decisionRepo.deleteAll();
        recRepo.deleteAll();
        requestRepo.deleteAll();
    }

    @Property(tries = 20)
    @Label("Feature: lifelane-mini, Property 14: One‑decision‑per‑request constraint")
    void decisionUniqueness(@ForAll("validRequests") EmergencyRequestDto requestDto) throws Exception {
        // 1️⃣ Create emergency request
        String requestJson = mapper.writeValueAsString(requestDto);
        String requestResp = mockMvc.perform(post("/api/emergencies")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        Long requestId = mapper.readTree(requestResp).get("id").asLong();

        // 2️⃣ Generate recommendation (use existing endpoint)
        mockMvc.perform(post("/api/emergencies/" + requestId + "/recommendations"))
                .andExpect(status().isOk());

        // 3️⃣ Record first decision (accept)
        DecisionRequestDto first = DecisionRequestDto.builder()
                .action("accept")
                .comment("first")
                .build();
        mockMvc.perform(post("/api/emergencies/" + requestId + "/decision")
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(first)))
                .andExpect(status().isOk());

        // 4️⃣ Attempt second decision – should be 409 Conflict
        DecisionRequestDto second = DecisionRequestDto.builder()
                .action("override")
                .ambulanceId(1L)
                .hospitalId(1L)
                .comment("second")
                .build();
        mockMvc.perform(post("/api/emergencies/" + requestId + "/decision")
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(second)))
                .andExpect(status().isConflict())
                .andExpect(result -> {
                    String body = result.getResponse().getContentAsString();
                    assertThat(body).contains("\"status\":409");
                });
    }

    @Provide
    Arbitrary<EmergencyRequestDto> validRequests() {
        Arbitrary<Double> lat = Arbitraries.doubles().between(-90.0, 90.0);
        Arbitrary<Double> lon = Arbitraries.doubles().between(-180.0, 180.0);
        Arbitrary<String> urgency = Arbitraries.of("low", "medium", "high", "critical");
        Arbitrary<String> care = Arbitraries.of("trauma", "cardiac", "burns", "pediatric");
        return Combinators.combine(lat, lon, urgency, care)
                .as((la, lo, ur, ca) -> EmergencyRequestDto.builder()
                        .latitude(la).longitude(lo)
                        .urgencyLevel(ur).requiredCareType(ca)
                        .build());
    }
}

