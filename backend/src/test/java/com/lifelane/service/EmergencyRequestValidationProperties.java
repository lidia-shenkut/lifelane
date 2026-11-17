package com.lifelane.service;

import com.lifelane.dto.request.EmergencyRequestDto;
import com.lifelane.dto.response.ErrorResponseDto;
import com.lifelane.model.EmergencyRequest;
import com.lifelane.repository.EmergencyRequestRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.jqwik.api.Arbitraries;
import net.jqwik.api.ForAll;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.Provide;
import net.jqwik.api.Label;
import net.jqwik.api.Property;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(SpringExtension.class)
@SpringBootTest
@AutoConfigureMockMvc
class EmergencyRequestValidationProperties {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private EmergencyRequestRepository repository;

    @BeforeEach
    void clean() {
        repository.deleteAll();
    }

    // ---------- Property 11: Invalid coordinates are rejected ----------
    @Property(tries = 50)
    @Label("Feature: lifelane-mini, Property 11: Invalid coordinates are rejected")
    void invalidCoordinatesCauseBadRequest(@ForAll("outOfRangeLatitudes") Double latitude,
                                          @ForAll("outOfRangeLongitudes") Double longitude) throws Exception {
        EmergencyRequestDto dto = EmergencyRequestDto.builder()
                .latitude(latitude)
                .longitude(longitude)
                .urgencyLevel("high")
                .requiredCareType("trauma")
                .build();

        String json = objectMapper.writeValueAsString(dto);
        String response = mockMvc.perform(post("/api/emergencies")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest())
                .andReturn()
                .getResponse()
                .getContentAsString();

        ErrorResponseDto error = objectMapper.readValue(response, ErrorResponseDto.class);
        assertThat(error.getErrors()).isNotEmpty();
    }

    // Generators for out‑of‑range latitude and longitude
    @Provide
    Arbitrary<Double> outOfRangeLatitudes() {
        // generate a double and map to outside [-90, 90]
        return Arbitraries.doubles()
                .between(-200.0, 200.0)
                .filter(d -> d < -90.0 || d > 90.0);
    }

    @Provide
    Arbitrary<Double> outOfRangeLongitudes() {
        return Arbitraries.doubles()
                .between(-400.0, 400.0)
                .filter(d -> d < -180.0 || d > 180.0);
    }

    // ---------- Property 10: Emergency request IDs are unique ----------
    @Property(tries = 20)
    @Label("Feature: lifelane-mini, Property 10: Emergency request IDs are unique")
    void generatedIdsAreUnique() {
        // Create a set of IDs from multiple saved requests
        Set<Long> ids = new HashSet<>();
        for (int i = 0; i < 30; i++) {
            EmergencyRequest entity = EmergencyRequest.builder()
                    .latitude(10.0 + i)
                    .longitude(20.0 + i)
                    .urgencyLevel("medium")
                    .requiredCareType("cardiac")
                    .status("open")
                    .build();
            EmergencyRequest saved = repository.save(entity);
            ids.add(saved.getId());
        }
        // Ensure no duplicate IDs were generated
        assertThat(ids).hasSize(30);
    }
}
