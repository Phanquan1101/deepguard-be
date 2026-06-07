package com.deepguard.scan.service;

import com.deepguard.auth.entity.User;
import com.deepguard.common.exception.BusinessException;
import com.deepguard.common.exception.ErrorCode;
import com.deepguard.media.entity.MediaFile;
import com.deepguard.common.response.PageResponse;
import com.deepguard.scan.repository.ScanJobRepository;
import com.deepguard.scan.dto.response.AIDetectResponse;
import com.deepguard.scan.dto.response.ScanJobResponse;
import com.deepguard.scan.entity.DetectionResult;
import com.deepguard.scan.entity.ScanJob;
import com.deepguard.scan.enums.DetectionLabel;
import com.deepguard.scan.enums.ScanJobStatus;
import com.deepguard.security.userdetails.CustomUserDetails;
import jakarta.persistence.EntityManager;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ScanJobServiceImpl implements ScanJobService {

    private final EntityManager entityManager;
    private final RestTemplate restTemplate;
    private final ScanJobRepository scanJobRepository;

    @Value("${app.ai.python-server-url}")
    private String pythonServerUrl;

    /**
     * Call AI server with image URL and persist ScanJob + DetectionResult. Returns AI response (or null on failure).
     */
    @Transactional
    @Override
    public AIDetectResponse createScanJobAndResult(MediaFile mediaFile, String imageUrl, User user) {
        AIDetectResponse aiResponse = null;
        try {
            // call external AI server
            try {
                String predictUrl = pythonServerUrl + "/predict";
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);
                Map<String, Object> body = new HashMap<>();
                body.put("imageUrl", imageUrl);
                HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
                ResponseEntity<AIDetectResponse> response = restTemplate.postForEntity(predictUrl, request, AIDetectResponse.class);
                aiResponse = response.getBody();
            } catch (Exception e) {
                // keep aiResponse null if call fails
            }

            ScanJobStatus status = (aiResponse != null) ? ScanJobStatus.COMPLETED : ScanJobStatus.FAILED;

            ScanJob scanJob = ScanJob.builder()
                    .mediaFile(mediaFile)
                    .user(user)
                    .status(status)
                    .startedAt(LocalDateTime.now())
                    .finishedAt(LocalDateTime.now())
                    .build();

            entityManager.persist(scanJob);

            if (aiResponse != null) {
                double fakeScore = aiResponse.getFakeProbability() != null
                        ? aiResponse.getFakeProbability()
                        : 0.0;

                BigDecimal bdFakeScore = BigDecimal.valueOf(fakeScore)
                        .setScale(2, RoundingMode.HALF_UP);

                BigDecimal bdConfidence = BigDecimal.valueOf(
                        aiResponse.getRealProbability() != null
                                ? aiResponse.getRealProbability()
                                : 1.0 - fakeScore
                ).setScale(2, RoundingMode.HALF_UP);
                DetectionLabel labelEnum =
                        aiResponse.getPrediction() != null
                                && aiResponse.getPrediction().equalsIgnoreCase("FAKE")
                                ? DetectionLabel.FAKE
                                : DetectionLabel.REAL;

                DetectionResult detectionResult = DetectionResult.builder()
                        .scanJob(scanJob)
                        .fakeScore(bdFakeScore)
                        .confidence(bdConfidence)
                        .resultLabel(labelEnum)
                        .modelVersion(aiResponse.getMessage() != null ? aiResponse.getMessage() : "unknown")
                        .processedAt(LocalDateTime.now())
                        .build();

                entityManager.persist(detectionResult);
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to persist scan job/result: " + e.getMessage(), e);
        }

        return aiResponse;
    }

    private User getCurrentAuthenticatedUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof CustomUserDetails principal)) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        return principal.getUser();
    }

    @Transactional(readOnly = true)
    @Override
    public List<ScanJobResponse> getMyScanJobs() {
        User currentUser = getCurrentAuthenticatedUser();
        List<ScanJob> scanJobList = scanJobRepository.findByUserOrderByStartedAtDesc(currentUser);
        return scanJobList.stream()
                .map(scanJob -> ScanJobResponse.builder()
                        .scanJobId(scanJob.getId())
                        .mediaId(scanJob.getMediaFile().getId())
                        .email(scanJob.getUser().getEmail())
                        .fileName(scanJob.getMediaFile().getFileName())
                        .originalUrl(scanJob.getMediaFile().getOriginalUrl())
                        .errorLoggings(scanJob.getErrorLoggings())
                        .status(scanJob.getStatus().name())
                        .startedAt(scanJob.getStartedAt())
                        .finishedAt(scanJob.getFinishedAt())
                        .build())
                .toList();
    }

    @Transactional(readOnly = true)
    @Override
    public PageResponse<ScanJobResponse> getAllScanJob(Integer page, Integer size, ScanJobStatus status) {
        int pageNo = (page == null || page < 0) ? 0 : page;
        int pageSize = (size == null || size <= 0) ? 10 : size;

        Pageable pageable = PageRequest.of(
                pageNo,
                pageSize,
                Sort.by(
                        Sort.Order.asc("status"),
                        Sort.Order.desc("startedAt")
                )
        );
        Page<ScanJob> scanJobPage;
        if (status != null) {
            scanJobPage = scanJobRepository.findAllByStatus(status, pageable);
        } else {
            scanJobPage = scanJobRepository.findAll(pageable);
        }

        List<ScanJobResponse> content = scanJobPage.getContent()
                .stream()
                .map(this::mapToResponse)
                .toList();
        return PageResponse.<ScanJobResponse>builder()
                .content(content)
                .page(scanJobPage.getNumber())
                .size(scanJobPage.getSize())
                .totalElements(scanJobPage.getTotalElements())
                .totalPages(scanJobPage.getTotalPages())
                .last(scanJobPage.isLast())
                .build();
    }

    private ScanJobResponse mapToResponse(ScanJob scanJob) {
        return ScanJobResponse.builder()
                .scanJobId(scanJob.getId())
                .mediaId(scanJob.getMediaFile().getId())
                .email(scanJob.getUser().getEmail())
                .fileName(scanJob.getMediaFile().getFileName())
                .originalUrl(scanJob.getMediaFile().getOriginalUrl())
                .errorLoggings(scanJob.getErrorLoggings())
                .status(scanJob.getStatus().name())
                .startedAt(scanJob.getStartedAt())
                .finishedAt(scanJob.getFinishedAt())
                .build();
    }
}
