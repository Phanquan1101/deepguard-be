package com.deepguard.scan.controller;

import com.deepguard.common.response.ApiResponse;
import com.deepguard.scan.dto.response.ScanJobResponse;
import com.deepguard.scan.service.ScanJobService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/scan-jobs")
@RequiredArgsConstructor
public class ScanJobController {

    private final ScanJobService scanJobService;

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<List<ScanJobResponse>>> getMyScanJob() {
        List<ScanJobResponse> scanJobResponses = scanJobService.getMyScanJobs();
        return ResponseEntity.ok(ApiResponse.success("Get Scan Job Successfully", scanJobResponses));
    }

}
