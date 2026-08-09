package com.deepguard.scan.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DetectionFrameResponse {
    private Integer frameIndex;
    private Double timestamp;
    private Double suspicionScore;
    private Double aiGeneratedScore;
    private Double notAiGeneratedScore;
    private Double deepfakeScore;
    private String attributedGenerator;
    private Double aiGeneratedAudioScore;
    private Double notAiGeneratedAudioScore;
}
