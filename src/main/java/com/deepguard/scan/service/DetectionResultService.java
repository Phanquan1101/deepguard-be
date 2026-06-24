package com.deepguard.scan.service;

import com.deepguard.common.response.PageResponse;
import com.deepguard.scan.dto.response.DetectionResultResponse;
import com.deepguard.scan.enums.DetectionLabel;
import com.deepguard.scan.enums.ScanJobStatus;

import java.util.List;

public interface DetectionResultService {
    List<DetectionResultResponse> getMyDetectionResults();
    PageResponse<DetectionResultResponse> getAllDetectionResults(Integer page, Integer size, DetectionLabel label);
    DetectionResultResponse getDetailDetectionResult(String scanJobId);
    DetectionResultResponse getDetailDetectionResultByResultId(String detectionResultId);
}
