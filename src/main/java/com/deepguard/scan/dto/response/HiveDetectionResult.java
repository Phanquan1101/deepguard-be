package com.deepguard.scan.dto.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Processed results from Hive AI-Generated & Deepfake Content Detection API.
 * For images: single frame result.
 * For videos: aggregated results across frames with overall verdict.
 * For audio: audio authenticity scores.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class HiveDetectionResult {

    /** Overall prediction label: AI_GENERATED, NOT_AI_GENERATED, DEEPFAKE, or INCONCLUSIVE */
    private String prediction;

    /** Confidence in the prediction (0.0 - 1.0) */
    private Double confidence;

    /** AI-generated authenticity score (ai_generated class value) */
    private Double aiGeneratedScore;

    /** Not AI-generated score */
    private Double notAiGeneratedScore;

    /** Deepfake visual score */
    private Double deepfakeScore;

    /** AI-generated audio score (for videos with audio or audio-only inputs) */
    private Double aiGeneratedAudioScore;

    /** Not AI-generated audio score */
    private Double notAiGeneratedAudioScore;

    /** Attributed generator model if detected (e.g., "midjourney", "stablediffusion", "dalle", etc.) */
    private String attributedGenerator;

    /** Whether the input is a video with multiple frames */
    @JsonProperty("isVideo")
    private boolean isVideo;

    /** Frame-level details for videos */
    private List<FrameResult> frames;

    /** Provider task ID; retained for internal persistence only. */
    @JsonIgnore
    private String taskId;

    /** Internal source URL; MediaFileResponse already returns originalUrl. */
    @JsonIgnore
    private String mediaUrl;

    /** Internal scan job ID, surfaced by MediaFileResponse at the top level. */
    @JsonIgnore
    private String scanJobId;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class FrameResult {
        private Integer frameIndex;
        private Double timestamp;
        private Double aiGeneratedScore;
        private Double notAiGeneratedScore;
        private Double deepfakeScore;
        private String attributedGenerator;
        private Double aiGeneratedAudioScore;
        private Double notAiGeneratedAudioScore;
    }
}
