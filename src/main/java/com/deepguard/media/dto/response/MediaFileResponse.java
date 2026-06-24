package com.deepguard.media.dto.response;

import com.deepguard.scan.dto.response.AIDetectResponse;
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

    // Optional AI detection result for image scans (may be null)
    private AIDetectResponse aiDetect;

    // Optional Hive detection result for video/audio scans (may be null)
    private HiveDetectionResult hiveDetect;

}
