package com.deepguard.media.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminMediaResponse {

    private String id;
    private String fileName;
    private String originalUrl;
    private String fileType;
    private Long fileSize;
    private LocalDateTime uploadedAt;
    private String userId;
    private String email;

}
