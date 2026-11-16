package com.lifelane.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.util.Map;

/**
 * Service that calls the Python ML sidecar for ETA prediction.
 * Conditional on {@code ml.enabled=true}.
 */
@Service
@ConditionalOnProperty(name = "ml.enabled", havingValue = "true")
public class MlPredictorService {

    private static final Logger log = LoggerFactory.getLogger(MlPredictorService.class);
    private static final String PREDICT_URL = "http://localhost:5001/predict";

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public MlPredictorService(RestTemplateBuilder restTemplateBuilder, ObjectMapper objectMapper) {
        this.restTemplate = restTemplateBuilder
                .setConnectTimeout(Duration.ofSeconds(2))
                .setReadTimeout(Duration.ofSeconds(3))
                .build();
        this.objectMapper = objectMapper;
    }

    /**
     * Sends features array to the ML sidecar and returns predicted ETA in seconds.
     */
    public int predict(FeatureVector features) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, Object> requestBody = Map.of("features", features.asFeatureList());
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

            ResponseEntity<String> response = restTemplate.postForEntity(PREDICT_URL, entity, String.class);
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                JsonNode root = objectMapper.readTree(response.getBody());
                JsonNode predicted = root.get("predicted_eta_seconds");
                if (predicted != null && predicted.isNumber()) {
                    return (int) Math.round(predicted.asDouble());
                }
            }
        } catch (Exception e) {
            log.warn("Failed to obtain prediction from ML sidecar: {}", e.getMessage());
        }
        // Fallback default if sidecar is unreachable
        return (int) Math.round(features.distanceMeters() / 11.1);
    }
}
