package com.deepguard.scan.service;

import com.deepguard.auth.entity.User;
import com.deepguard.media.entity.MediaFile;
import com.deepguard.scan.dto.response.AIDetectResponse;
import com.deepguard.common.response.PageResponse;
import com.deepguard.scan.dto.response.ScanJobResponse;
import com.deepguard.scan.enums.ScanJobStatus;

import java.util.List;

public interface ScanJobService {
    AIDetectResponse createScanJobAndResult(MediaFile mediaFile, String imageUrl, User user);
    List<ScanJobResponse> getMyScanJobs();
    PageResponse<ScanJobResponse> getAllScanJob(Integer page, Integer size, ScanJobStatus status);
}
