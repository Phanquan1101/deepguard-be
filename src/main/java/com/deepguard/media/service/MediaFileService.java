package com.deepguard.media.service;

import com.deepguard.auth.entity.User;
import com.deepguard.common.response.PageResponse;
import com.deepguard.media.dto.response.AdminMediaResponse;
import com.deepguard.media.dto.response.MediaFileResponse;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;

public interface MediaFileService {

    MediaFileResponse uploadFile(MultipartFile file);
    PageResponse<MediaFileResponse> getMyMediaFiles(LocalDateTime startDate, LocalDateTime endDate, Pageable pageable);
    MediaFileResponse deleteMedia(String mediaId);
    PageResponse<AdminMediaResponse> getAllMedia(LocalDateTime startDate, LocalDateTime endDate, Pageable pageable);
    MediaFileResponse getMyMediaFileById(String mediaFileId);

}
