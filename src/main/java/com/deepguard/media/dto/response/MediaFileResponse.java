package com.deepguard.media.dto.response;

import com.deepguard.scan.dto.response.HiveDetectionResult;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MediaFileResponse {

    private String id;
    private String userId;
    private String fileName;
    private String originalUrl;
    private String fileType;
    private Long fileSize;
    private LocalDateTime uploadedAt;

    /**
     * Detection result for this media. The detection provider is intentionally
     * an implementation detail and is not part of the public API contract.
     */
    private HiveDetectionResult detection;

}
