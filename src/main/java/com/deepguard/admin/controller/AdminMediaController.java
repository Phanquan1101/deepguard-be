package com.deepguard.admin.controller;

import com.deepguard.common.response.ApiResponse;
import com.deepguard.common.response.PageResponse;
import com.deepguard.admin.dto.AdminAnalyticsResponse;
import com.deepguard.admin.service.AdminAnalyticsService;
import com.deepguard.media.dto.response.AdminMediaResponse;
import com.deepguard.media.service.MediaFileService;
import com.deepguard.scan.dto.response.DetectionResultResponse;
import com.deepguard.scan.dto.response.ScanJobResponse;
import com.deepguard.scan.enums.DetectionLabel;
import com.deepguard.scan.enums.ScanJobStatus;
import com.deepguard.scan.service.DetectionResultService;
import com.deepguard.scan.service.ScanJobService;
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
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminMediaController {

    private final MediaFileService mediaFileService;
    private final ScanJobService scanJobService;
    private final DetectionResultService detectionResultService;
    private final AdminAnalyticsService adminAnalyticsService;

    @GetMapping("/analytics")
    public ResponseEntity<ApiResponse<AdminAnalyticsResponse>> getAnalytics() {
        return ResponseEntity.ok(ApiResponse.success(
                "Get admin analytics successfully",
                adminAnalyticsService.getAnalytics()
        ));
    }

    @GetMapping("/media/all")
    public ResponseEntity<ApiResponse<PageResponse<AdminMediaResponse>>> getAllMedia(
            @RequestParam(required = false)
            LocalDateTime startDate,
            @RequestParam(required = false)
            LocalDateTime endDate,
            @ParameterObject Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success("Get all media successfully", mediaFileService.getAllMedia(startDate, endDate, pageable)));
    }

    @GetMapping("/scan-jobs/all")
    public ResponseEntity<ApiResponse<PageResponse<ScanJobResponse>>> getAllScanJobs(
            @RequestParam(required = false, defaultValue = "0")
            Integer page,

            @RequestParam(required = false, defaultValue = "10")
            Integer size,

            @RequestParam(required = false)
            ScanJobStatus status
    ) {
        PageResponse<ScanJobResponse> response = scanJobService.getAllScanJob(page, size, status);
        return ResponseEntity.ok(ApiResponse.success("Get all scan jobs successfully", response));
    }

    @GetMapping("/detection-results/all")
    public ResponseEntity<ApiResponse<PageResponse<DetectionResultResponse>>> getAllDetectionResults(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) DetectionLabel resultLabel
    ) {
        PageResponse<DetectionResultResponse> response = detectionResultService.getAllDetectionResults(page, size, resultLabel);
        return ResponseEntity.ok(ApiResponse.success("Get all detection results successfully", response));
    }


}
