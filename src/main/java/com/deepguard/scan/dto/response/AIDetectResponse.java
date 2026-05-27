package com.deepguard.scan.dto.response;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AIDetectResponse {

    /**
     * Prediction label, e.g. "deepfake" or "real"
     */
    @JsonProperty("label")
    private String label;

    /**
     * Prediction score (0.0 - 1.0). Accepts both "score" and legacy "confidence" from Python.
     */
    @JsonProperty("score")
    @JsonAlias({"confidence"})
    private Double score;

    /**
     * Source image URL returned by Python server (optional)
     */
    @JsonProperty("imageUrl")
    private String imageUrl;

    /**
     * Additional message from Python server (optional)
     */
    @JsonProperty("message")
    private String message;
}
