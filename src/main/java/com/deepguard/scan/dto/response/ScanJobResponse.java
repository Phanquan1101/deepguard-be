package com.deepguard.scan.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScanJobResponse {
    private String scanJobId;
    private String mediaId;
    private String email;
    private String fileName;
    private String originalUrl;
    private String errorLoggings;
    private String status;
    private LocalDateTime startedAt;
    private LocalDateTime finishedAt;
}
