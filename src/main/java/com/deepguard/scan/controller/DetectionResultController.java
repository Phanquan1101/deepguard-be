package com.deepguard.scan.controller;

import com.deepguard.common.response.ApiResponse;
import com.deepguard.scan.dto.response.DetectionResultResponse;
import com.deepguard.scan.service.DetectionResultService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/detection-results")
@RequiredArgsConstructor
public class DetectionResultController {

    private final DetectionResultService detectionResultService;

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<List<DetectionResultResponse>>> getMyDetectionResults() {
        List<DetectionResultResponse> results = detectionResultService.getMyDetectionResults();
        return ResponseEntity.ok(ApiResponse.success("Get detection result successfully", results));
    }

}
