package com.deepguard.scan.service;

import com.deepguard.auth.entity.User;
import com.deepguard.common.exception.BusinessException;
import com.deepguard.common.exception.ErrorCode;
import com.deepguard.common.response.PageResponse;
import com.deepguard.scan.dto.response.DetectionResultResponse;
import com.deepguard.scan.entity.DetectionResult;
import com.deepguard.scan.entity.ScanJob;
import com.deepguard.scan.enums.DetectionLabel;
import com.deepguard.scan.repository.DetectionResultRepository;
import com.deepguard.security.userdetails.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DetectionResultServiceImpl implements DetectionResultService {

    private final DetectionResultRepository detectionResultRepository;

    private User getCurrentAuthenticatedUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof CustomUserDetails principal)) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        return principal.getUser();
    }

    @Transactional(readOnly = true)
    @Override
    public List<DetectionResultResponse> getMyDetectionResults() {
        User currentUser = getCurrentAuthenticatedUser();
        List<DetectionResult> detectionResultList = detectionResultRepository.findByScanJob_UserOrderByProcessedAtDesc(currentUser);
        return detectionResultList.stream()
                .map(detection -> {
                    ScanJob scanJob = detection.getScanJob();
                    return DetectionResultResponse.builder()
                            .detectionResultId(detection.getId())
                            .scanJobId(scanJob.getId())
                            .mediaId(scanJob.getMediaFile().getId())
                            .fileName(scanJob.getMediaFile().getFileName())
                            .originalUrl(scanJob.getMediaFile().getOriginalUrl())
                            .fakeScore(detection.getFakeScore())
                            .confidence(detection.getConfidence())
                            .resultLabel(detection.getResultLabel().name())
                            .modelVersion("DeepGuard Detection Engine")
                            .processedAt(detection.getProcessedAt())
                            .build();
                })
                .toList();
    }

    @Override
    public PageResponse<DetectionResultResponse> getAllDetectionResults(Integer page, Integer size, DetectionLabel label) {
        int pageNo = (page == null || page < 0) ? 0 : page;
        int pageSize = (size == null || size <= 0) ? 10 : size;

        Pageable pageable = PageRequest.of(
                pageNo,
                pageSize,
                Sort.by(Sort.Order.desc("processedAt"))
        );

        Page<DetectionResult> detectionPage;
        if (label != null) {
            detectionPage = detectionResultRepository
                    .findAllByResultLabel(label, pageable);
        } else {
            detectionPage = detectionResultRepository
                    .findAllDetectionResult(pageable);
        }
        List<DetectionResultResponse> content = detectionPage.getContent()
                .stream()
                .map(this::mapToResponse)
                .toList();
        return PageResponse.<DetectionResultResponse>builder()
                .content(content)
                .page(detectionPage.getNumber())
                .size(detectionPage.getSize())
                .totalElements(detectionPage.getTotalElements())
                .totalPages(detectionPage.getTotalPages())
                .last(detectionPage.isLast())
                .build();
    }

    @Transactional(readOnly = true)
    @Override
    public DetectionResultResponse getDetailDetectionResult(String scanJobId) {
        User currentUser = getCurrentAuthenticatedUser();
        DetectionResult detectionResult = detectionResultRepository.findByScanJobId(scanJobId)
                .orElseThrow(() -> new BusinessException(ErrorCode.DETECTION_RESULT_NOT_FOUND));
        if (!detectionResult.getScanJob().getUser().getId().equals(currentUser.getId())) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED);
        }
        return mapToResponse(detectionResult);
    }

    @Transactional(readOnly = true)
    @Override
    public DetectionResultResponse getDetailDetectionResultByResultId(String detectionResultId) {
        User currentUser = getCurrentAuthenticatedUser();
        DetectionResult detectionResult = detectionResultRepository.findById(detectionResultId)
                .orElseThrow(() -> new BusinessException(ErrorCode.DETECTION_RESULT_NOT_FOUND));
        if (!detectionResult.getScanJob().getUser().getId().equals(currentUser.getId())) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED);
        }
        return mapToResponse(detectionResult);
    }

    private DetectionResultResponse mapToResponse(DetectionResult detection) {

        ScanJob scanJob = detection.getScanJob();

        return DetectionResultResponse.builder()
                .detectionResultId(detection.getId())
                .scanJobId(scanJob.getId())
                .mediaId(scanJob.getMediaFile().getId())
                .email(scanJob.getUser().getEmail())
                .fileName(scanJob.getMediaFile().getFileName())
                .originalUrl(scanJob.getMediaFile().getOriginalUrl())
                .fakeScore(detection.getFakeScore())
                .confidence(detection.getConfidence())
                .resultLabel(detection.getResultLabel().name())
                .modelVersion("DeepGuard Detection Engine")
                .processedAt(detection.getProcessedAt())
                .build();
    }
}
