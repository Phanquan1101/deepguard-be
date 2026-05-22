package com.deepguard.admin.controller;

import com.deepguard.common.response.ApiResponse;
import com.deepguard.common.response.PageResponse;
import com.deepguard.media.dto.response.AdminMediaResponse;
import com.deepguard.media.service.MediaFileService;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/admin/media")
@RequiredArgsConstructor
public class AdminMediaController {

    private final MediaFileService mediaFileService;

    @GetMapping("/all")
    public ResponseEntity<ApiResponse<PageResponse<AdminMediaResponse>>> getAllMedia(
            @RequestParam(required = false)
            LocalDateTime startDate,
            @RequestParam(required = false)
            LocalDateTime endDate,
            @ParameterObject Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success("Get all media successfully", mediaFileService.getAllMedia(startDate, endDate, pageable)));
    }

}
