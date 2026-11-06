package com.lifelane.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Externalised configuration for the ML ETA-predictor feature.
 *
 * <p>Bound from the {@code ml.*} keys in {@code application.yml}.
 * The feature is disabled by default ({@code ml.enabled=false}) so the
 * {@code MlPredictorService} bean is not instantiated in the standard profile.
 */
@Data
@Component
@ConfigurationProperties(prefix = "ml")
public class MlFeatureProperties {

    /**
     * Master feature flag. Set to {@code true} to activate the ML predictor.
     * The {@code MlPredictorService} bean is conditional on this value.
     */
    private boolean enabled;

    /**
     * Filesystem path (inside the container) to the serialised scikit-learn
     * model file, e.g. {@code /models/eta_predictor.pkl}.
     */
    private String modelPath;

    /**
     * Minimum number of recorded trips required before the ML model is
     * considered trained enough to produce reliable predictions.
     */
    private int minTrainingSamples;
}
