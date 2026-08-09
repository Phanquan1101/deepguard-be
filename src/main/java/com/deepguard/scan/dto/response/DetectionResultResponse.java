package com.deepguard.scan.dto.response;

import com.deepguard.scan.enums.DetectionLabel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DetectionResultResponse {
    private String detectionResultId;
    private String scanJobId;
    private String email;
    private String mediaId;
    private String fileName;
    private String originalUrl;
    private BigDecimal fakeScore;
    private BigDecimal confidence;
    private BigDecimal aiGeneratedScore;
    private BigDecimal notAiGeneratedScore;
    private BigDecimal deepfakeScore;
    private BigDecimal aiGeneratedAudioScore;
    private BigDecimal notAiGeneratedAudioScore;
    private String attributedGenerator;
    private boolean video;
    private List<DetectionFrameResponse> frames;
    private String resultLabel;
    private String modelVersion;
    private LocalDateTime processedAt;
}
