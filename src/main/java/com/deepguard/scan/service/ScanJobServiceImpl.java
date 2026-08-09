package com.deepguard.scan.service;

import com.deepguard.auth.entity.User;
import com.deepguard.common.exception.BusinessException;
import com.deepguard.common.exception.ErrorCode;
import com.deepguard.media.entity.MediaFile;
import com.deepguard.media.enums.FileType;
import com.deepguard.common.response.PageResponse;
import com.deepguard.scan.repository.ScanJobRepository;
import com.deepguard.scan.repository.DetectionFrameRepository;
import com.deepguard.scan.dto.response.HiveDetectionResult;
import com.deepguard.scan.dto.response.ScanJobResponse;
import com.deepguard.scan.entity.DetectionResult;
import com.deepguard.scan.entity.DetectionFrame;
import com.deepguard.scan.entity.ScanJob;
import com.deepguard.scan.enums.DetectionLabel;
import com.deepguard.scan.enums.ScanJobStatus;
import com.deepguard.security.userdetails.CustomUserDetails;
import jakarta.persistence.EntityManager;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ScanJobServiceImpl implements ScanJobService {

    private final EntityManager entityManager;
    private final ScanJobRepository scanJobRepository;
    private final DetectionFrameRepository detectionFrameRepository;
    private final HiveService hiveService;

    /** Creates a scan job and detection result for any supported media file. */
    @Transactional
    @Override
    public HiveDetectionResult createMediaScanJobAndResult(MediaFile mediaFile, String mediaUrl, User user) {
        HiveDetectionResult hiveResult = null;
        try {
            // The provider receives the public URL after the file is stored.
            try {
                hiveResult = hiveService.detectFromUrl(mediaUrl);
            } catch (Exception e) {
                log.error("Media detection failed: {}", e.getMessage());
            }

            ScanJobStatus status = (hiveResult != null) ? ScanJobStatus.COMPLETED : ScanJobStatus.FAILED;

            ScanJob scanJob = ScanJob.builder()
                    .mediaFile(mediaFile)
                    .user(user)
                    .status(status)
                    .startedAt(LocalDateTime.now())
                    .finishedAt(LocalDateTime.now())
                    .build();

            entityManager.persist(scanJob);

            if (hiveResult != null) {
                boolean isAudio = mediaFile.getFileType() == FileType.AUDIO;
                double fakeScore = isAudio
                        ? valueOrZero(hiveResult.getAiGeneratedAudioScore())
                        : Math.max(
                                valueOrZero(hiveResult.getAiGeneratedScore()),
                                Math.max(
                                        valueOrZero(hiveResult.getDeepfakeScore()),
                                        valueOrZero(hiveResult.getAiGeneratedAudioScore())
                                )
                        );

                BigDecimal bdFakeScore = BigDecimal.valueOf(fakeScore)
                        .setScale(2, RoundingMode.HALF_UP);

                BigDecimal bdConfidence = BigDecimal.valueOf(
                        hiveResult.getConfidence() != null ? hiveResult.getConfidence() : 0.0
                ).setScale(2, RoundingMode.HALF_UP);

                boolean isFake = isAudio
                        ? "AI_GENERATED_AUDIO".equalsIgnoreCase(hiveResult.getPrediction())
                                || "AI_GENERATED".equalsIgnoreCase(hiveResult.getPrediction())
                        : hiveResult.getPrediction() != null
                                && !"NOT_AI_GENERATED".equalsIgnoreCase(hiveResult.getPrediction());

                DetectionLabel labelEnum = isFake ? DetectionLabel.FAKE : DetectionLabel.REAL;

                DetectionResult detectionResult = DetectionResult.builder()
                        .scanJob(scanJob)
                        .fakeScore(bdFakeScore)
                        .confidence(bdConfidence)
                        .aiGeneratedScore(toScore(hiveResult.getAiGeneratedScore()))
                        .notAiGeneratedScore(toScore(hiveResult.getNotAiGeneratedScore()))
                        .deepfakeScore(toScore(hiveResult.getDeepfakeScore()))
                        .aiGeneratedAudioScore(toScore(hiveResult.getAiGeneratedAudioScore()))
                        .notAiGeneratedAudioScore(toScore(hiveResult.getNotAiGeneratedAudioScore()))
                        .attributedGenerator(hiveResult.getAttributedGenerator())
                        .video(hiveResult.isVideo())
                        .resultLabel(labelEnum)
                        .modelVersion("deepguard-detection-v1")
                        .processedAt(LocalDateTime.now())
                        .build();

                entityManager.persist(detectionResult);
                persistFrames(detectionResult, hiveResult);
            }

            // Keep the identifier out of the detection payload itself; the
            // upload response exposes it at the media level for direct routing.
            hiveResult.setScanJobId(scanJob.getId());
        } catch (Exception e) {
            throw new RuntimeException("Failed to persist media scan job/result: " + e.getMessage(), e);
        }

        return hiveResult;
    }

    private double valueOrZero(Double value) {
        return value != null ? value : 0.0;
    }

    private BigDecimal toScore(Double value) {
        return BigDecimal.valueOf(valueOrZero(value)).setScale(5, RoundingMode.HALF_UP);
    }

    private void persistFrames(DetectionResult detectionResult, HiveDetectionResult hiveResult) {
        if (!hiveResult.isVideo() || hiveResult.getFrames() == null || hiveResult.getFrames().isEmpty()) {
            return;
        }

        List<DetectionFrame> frames = hiveResult.getFrames().stream()
                .map(frame -> DetectionFrame.builder()
                        .detectionResult(detectionResult)
                        .frameIndex(frame.getFrameIndex())
                        .frameTimestamp(toFloat(frame.getTimestamp()))
                        .suspicionScore(toFloat(Math.max(
                                valueOrZero(frame.getAiGeneratedScore()),
                                Math.max(
                                        valueOrZero(frame.getDeepfakeScore()),
                                        valueOrZero(frame.getAiGeneratedAudioScore())
                                )
                        )))
                        .aiGeneratedScore(toScore(frame.getAiGeneratedScore()))
                        .notAiGeneratedScore(toScore(frame.getNotAiGeneratedScore()))
                        .deepfakeScore(toScore(frame.getDeepfakeScore()))
                        .attributedGenerator(frame.getAttributedGenerator())
                        .aiGeneratedAudioScore(toScore(frame.getAiGeneratedAudioScore()))
                        .notAiGeneratedAudioScore(toScore(frame.getNotAiGeneratedAudioScore()))
                        .build())
                .toList();
        detectionFrameRepository.saveAll(frames);
    }

    private Float toFloat(Double value) {
        return (float) valueOrZero(value);
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
