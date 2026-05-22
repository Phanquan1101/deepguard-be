package com.deepguard.media.mapper;

import com.deepguard.media.dto.response.MediaFileResponse;
import com.deepguard.media.entity.MediaFile;
import org.springframework.stereotype.Component;

@Component
public class ToMediaFileMapper {

    public MediaFileResponse mapToMediaFileResponse(MediaFile mediaFile) {
        return MediaFileResponse.builder()
                .id(mediaFile.getId())
                .userId(mediaFile.getUser().getId())
                .fileName(mediaFile.getFileName())
                .originalUrl(mediaFile.getOriginalUrl())
                .fileType(mediaFile.getFileType().name())
                .fileSize(mediaFile.getFileSize())
                .uploadedAt(mediaFile.getUploadedAt())
                .build();
    }

}
