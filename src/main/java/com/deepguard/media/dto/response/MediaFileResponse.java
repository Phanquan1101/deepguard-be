package com.deepguard.media.dto.response;

import com.deepguard.scan.dto.response.AIDetectResponse;
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

    // Optional AI detection result for the uploaded media (may be null if detection not performed)
    private AIDetectResponse aiDetect;

}
