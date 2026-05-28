package com.deepguard.scan.dto.response;

import com.deepguard.scan.enums.DetectionLabel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

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
    private String resultLabel;
    private String modelVersion;
    private LocalDateTime processedAt;
}
