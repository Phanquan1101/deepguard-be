package com.deepguard.media.controller;

import com.deepguard.common.response.ApiResponse;
import com.deepguard.common.response.PageResponse;
import com.deepguard.media.dto.response.MediaFileResponse;
import com.deepguard.media.service.MediaFileService;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/media")
@RequiredArgsConstructor
public class MediaFileController {

    private final MediaFileService mediaFileService;

    @PostMapping(
            value = "/upload",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ResponseEntity<ApiResponse<MediaFileResponse>> uploadFile(@RequestParam("file") MultipartFile file){
        MediaFileResponse mediaFileResponse = mediaFileService.uploadFile(file);
        return ResponseEntity.ok(ApiResponse.success("Upload media file successfully", mediaFileResponse));
    }

    @GetMapping("/my")
    public ResponseEntity<ApiResponse<PageResponse<MediaFileResponse>>> getMyMedia(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime startDate,

            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime endDate,

            @ParameterObject Pageable pageable) {
        PageResponse<MediaFileResponse> mediaFileResponseList = mediaFileService.getMyMediaFiles(startDate, endDate, pageable);
        return ResponseEntity.ok(ApiResponse.success("Get my media files successfully", mediaFileResponseList));
    }

    @DeleteMapping("/{mediaId}")
    public ResponseEntity<ApiResponse<MediaFileResponse>> deleteMedia(@PathVariable String mediaId) {
        MediaFileResponse mediaFileResponse = mediaFileService.deleteMedia(mediaId);
        return ResponseEntity.ok(ApiResponse.success("Delete media file successfully", mediaFileResponse));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<MediaFileResponse>> getMedia(@PathVariable String id) {
        MediaFileResponse mediaFileResponse = mediaFileService.getMyMediaFileById(id);
        return ResponseEntity.ok(ApiResponse.success("Get media file successfully", mediaFileResponse));
    }

}
