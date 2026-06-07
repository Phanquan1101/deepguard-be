package com.deepguard.scan.dto.response;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@JsonIgnoreProperties(ignoreUnknown = true)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AIDetectResponse {

    /**
     * Prediction label returned by the detector, e.g. "REAL" or "FAKE".
     */
    @JsonProperty("prediction")
    private String prediction;

    /**
     * Probability that the input is fake (0.0 - 1.0).
     * Accepts legacy names "score" and "confidence" from older services.
     */
    @JsonProperty("fakeProbability")
    @JsonAlias({"score", "confidence"})
    private Double fakeProbability;

    /**
     * Probability that the input is real (0.0 - 1.0).
     * If not provided by the sender, this is computed as (1 - fakeProbability).
     */
    private Double realProbability;

    @JsonProperty("realProbability")
    public Double getRealProbability() {
        if (realProbability != null) return realProbability;
        if (fakeProbability != null) return 1.0 - fakeProbability;
        return null;
    }

    /**
     * Optional source image URL returned by the detector.
     */
    @JsonProperty("imageUrl")
    private String imageUrl;

    /**
     * Optional additional message from the detector.
     */
    @JsonProperty("message")
    private String message;
}
