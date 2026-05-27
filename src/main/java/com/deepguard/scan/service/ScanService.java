package com.deepguard.scan.service;

import com.deepguard.auth.entity.User;
import com.deepguard.media.entity.MediaFile;
import com.deepguard.scan.dto.response.AIDetectResponse;
import com.deepguard.scan.entity.DetectionResult;
import com.deepguard.scan.entity.ScanJob;
import com.deepguard.scan.enums.DetectionLabel;
import com.deepguard.scan.enums.ScanJobStatus;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
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
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ScanService {

    private final EntityManager entityManager;
    private final RestTemplate restTemplate;

    @Value("${app.ai.python-server-url}")
    private String pythonServerUrl;

    /**
     * Call AI server with image URL and persist ScanJob + DetectionResult. Returns AI response (or null on failure).
     */
    @Transactional
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
                double score = aiResponse.getScore() != null ? aiResponse.getScore() : 0.0;
                BigDecimal bdScore = BigDecimal.valueOf(score).setScale(2, RoundingMode.HALF_UP);
                DetectionLabel labelEnum = (aiResponse.getLabel() != null && aiResponse.getLabel().toLowerCase().contains("fake")) ? DetectionLabel.FAKE : DetectionLabel.REAL;

                DetectionResult detectionResult = DetectionResult.builder()
                        .scanJob(scanJob)
                        .fakeScore(bdScore)
                        .confidence(bdScore)
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
}
