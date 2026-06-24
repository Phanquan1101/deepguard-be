package com.deepguard.scan.service;

import com.deepguard.common.exception.BusinessException;
import com.deepguard.common.exception.ErrorCode;
import com.deepguard.scan.config.HiveProperties;
import com.deepguard.scan.dto.request.HiveDetectRequest;
import com.deepguard.scan.dto.response.HiveDetectResponse;
import com.deepguard.scan.dto.response.HiveDetectionResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Implementation of Hive AI-Generated & Deepfake Content Detection API integration.
 *
 * API Endpoint: POST https://api.thehive.ai/api/v3/hive/ai-generated-and-deepfake-content-detection
 * Authentication: Bearer token in Authorization header
 *
 * Class categories returned:
 *   - ai_generated / not_ai_generated (image authenticity)
 *   - deepfake (visual deepfake detection)
 *   - Generator attribution (midjourney, stablediffusion, dalle, etc.)
 *   - ai_generated_audio / not_ai_generated_audio (audio authenticity)
 *
 * Recommended thresholds:
 *   - AI-Generated Images: ai_generated >= 0.9
 *   - AI-Generated Videos: ai_generated >= 0.9 on any frame
 *   - Deepfake Image: deepfake >= 0.9
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class HiveServiceImpl implements HiveService {

    private static final double AI_GENERATED_THRESHOLD = 0.9;
    private static final double DEEPFAKE_THRESHOLD = 0.9;

    private static final String CLASS_AI_GENERATED = "ai_generated";
    private static final String CLASS_NOT_AI_GENERATED = "not_ai_generated";
    private static final String CLASS_DEEPFAKE = "deepfake";
    private static final String CLASS_AI_GENERATED_AUDIO = "ai_generated_audio";
    private static final String CLASS_NOT_AI_GENERATED_AUDIO = "not_ai_generated_audio";
    private static final String CLASS_NONE = "none";

    /** Known generator model classes (subset — the full list is longer). */
    private static final Set<String> GENERATOR_CLASSES = Set.of(
            "midjourney", "stablediffusion", "dalle", "dalle2", "dalle3",
            "firefly", "imagen", "parti", "cogview", "makeascene",
            "stablevideo", "gen2", "pika", "kaiber", "deforum",
            "other_image_generators", "other_video_generators"
    );

    private final HiveProperties hiveProperties;
    private final RestTemplate restTemplate;

    @Override
    public HiveDetectionResult detect(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ErrorCode.FILE_UPLOAD_FAILED, "File is empty");
        }
        // This method expects the file to be uploaded externally and URL provided.
        // For direct file detection, use detectFromUrl after uploading.
        throw new BusinessException(ErrorCode.FILE_UPLOAD_FAILED,
                "Direct file detection not supported. Upload file first, then call detectFromUrl.");
    }

    @Override
    public HiveDetectionResult detectFromUrl(String mediaUrl) {
        if (mediaUrl == null || mediaUrl.trim().isEmpty()) {
            throw new BusinessException(ErrorCode.FILE_UPLOAD_FAILED, "Media URL is empty");
        }

        String apiUrl = hiveProperties.getFullApiUrl();
        String apiKey = hiveProperties.getApiKey();

        log.info("Sending media URL to Hive API: {}", apiUrl);
        log.debug("Media URL: {}", mediaUrl);

        // Build request
        HiveDetectRequest request = HiveDetectRequest.builder()
                .input(List.of(
                        HiveDetectRequest.InputItem.builder()
                                .mediaUrl(mediaUrl)
                                .build()
                ))
                .build();

        // Build headers
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + apiKey);

        HttpEntity<HiveDetectRequest> requestEntity = new HttpEntity<>(request, headers);

        try {
            ResponseEntity<HiveDetectResponse> response = restTemplate.postForEntity(
                    apiUrl,
                    requestEntity,
                    HiveDetectResponse.class
            );

            log.debug("Hive API response status: {}", response.getStatusCode());

            HiveDetectResponse responseBody = response.getBody();
            if (responseBody == null) {
                log.error("Hive API returned null body");
                throw new BusinessException(ErrorCode.AI_SERVER_UNAVAILABLE, "Hive API returned empty response");
            }

            return processHiveResponse(responseBody, mediaUrl);

        } catch (RestClientException e) {
            log.error("Failed to call Hive API at {}: {}", apiUrl, e.getMessage(), e);
            throw new BusinessException(ErrorCode.AI_SERVER_UNAVAILABLE, "Hive API call failed: " + e.getMessage());
        }
    }

    /**
     * Processes the raw Hive API response into a structured HiveDetectionResult.
     * Handles both single-frame (images) and multi-frame (videos) responses.
     */
    private HiveDetectionResult processHiveResponse(HiveDetectResponse response, String mediaUrl) {
        if (response.getOutput() == null || response.getOutput().isEmpty()) {
            log.warn("Hive API returned no output");
            return HiveDetectionResult.builder()
                    .prediction("INCONCLUSIVE")
                    .confidence(0.0)
                    .aiGeneratedScore(0.0)
                    .notAiGeneratedScore(0.0)
                    .deepfakeScore(0.0)
                    .mediaUrl(mediaUrl)
                    .taskId(response.getTaskId())
                    .isVideo(false)
                    .build();
        }

        List<HiveDetectResponse.OutputItem> output = response.getOutput();

        // Determine if this is a video (multiple frames) or single image
        boolean isVideo = output.size() > 1
                || output.stream()
                .anyMatch(item -> item.getExtra() != null
                        && item.getExtra().stream()
                        .anyMatch(e -> "frame_index".equals(e.getName()) && ((Number) e.getValue()).intValue() > 0));

        // Process all frames
        List<HiveDetectionResult.FrameResult> frames = output.stream()
                .map(this::processFrame)
                .collect(Collectors.toList());

        // Compute aggregate scores
        double maxAiGenerated = frames.stream()
                .mapToDouble(f -> f.getAiGeneratedScore() != null ? f.getAiGeneratedScore() : 0.0)
                .max()
                .orElse(0.0);

        double minNotAiGenerated = frames.stream()
                .mapToDouble(f -> f.getNotAiGeneratedScore() != null ? f.getNotAiGeneratedScore() : 0.0)
                .min()
                .orElse(0.0);

        double maxDeepfake = frames.stream()
                .mapToDouble(f -> f.getDeepfakeScore() != null ? f.getDeepfakeScore() : 0.0)
                .max()
                .orElse(0.0);

        double maxAiGeneratedAudio = frames.stream()
                .mapToDouble(f -> f.getAiGeneratedAudioScore() != null ? f.getAiGeneratedAudioScore() : 0.0)
                .max()
                .orElse(0.0);

        double minNotAiGeneratedAudio = frames.stream()
                .mapToDouble(f -> f.getNotAiGeneratedAudioScore() != null ? f.getNotAiGeneratedAudioScore() : 0.0)
                .min()
                .orElse(0.0);

        // Find top generator across all frames
        String topGenerator = frames.stream()
                .filter(f -> f.getAttributedGenerator() != null && !f.getAttributedGenerator().isEmpty())
                .map(HiveDetectionResult.FrameResult::getAttributedGenerator)
                .filter(gen -> !CLASS_NONE.equals(gen))
                .findFirst()
                .orElse(null);

        // Determine overall prediction using Hive recommended thresholds
        String prediction;
        Double confidence;

        boolean isAiGenerated = maxAiGenerated >= AI_GENERATED_THRESHOLD;
        boolean isDeepfake = maxDeepfake >= DEEPFAKE_THRESHOLD;
        boolean isAiGeneratedAudio = maxAiGeneratedAudio >= AI_GENERATED_THRESHOLD;

        if (isAiGenerated || isDeepfake) {
            if (isAiGenerated && isDeepfake) {
                prediction = "AI_GENERATED_AND_DEEPFAKE";
                confidence = Math.max(maxAiGenerated, maxDeepfake);
            } else if (isAiGenerated) {
                prediction = "AI_GENERATED";
                confidence = maxAiGenerated;
            } else {
                prediction = "DEEPFAKE";
                confidence = maxDeepfake;
            }
        } else if (isAiGeneratedAudio) {
            prediction = "AI_GENERATED_AUDIO";
            confidence = maxAiGeneratedAudio;
        } else {
            prediction = "NOT_AI_GENERATED";
            // Use the highest not_ai_generated score as confidence
            confidence = frames.stream()
                    .mapToDouble(f -> f.getNotAiGeneratedScore() != null ? f.getNotAiGeneratedScore() : 0.0)
                    .max()
                    .orElse(0.0);
        }

        return HiveDetectionResult.builder()
                .prediction(prediction)
                .confidence(confidence)
                .aiGeneratedScore(maxAiGenerated)
                .notAiGeneratedScore(minNotAiGenerated)
                .deepfakeScore(maxDeepfake)
                .aiGeneratedAudioScore(maxAiGeneratedAudio)
                .notAiGeneratedAudioScore(minNotAiGeneratedAudio)
                .attributedGenerator(topGenerator)
                .isVideo(isVideo)
                .frames(frames)
                .taskId(response.getTaskId())
                .mediaUrl(mediaUrl)
                .build();
    }

    /**
     * Processes a single output item (one frame in a video, or the single result for an image).
     */
    private HiveDetectionResult.FrameResult processFrame(HiveDetectResponse.OutputItem item) {
        Map<String, Double> classMap = new HashMap<>();
        if (item.getClasses() != null) {
            for (HiveDetectResponse.OutputItem.ClassItem ci : item.getClasses()) {
                classMap.put(ci.getClassName(), ci.getValue() != null ? ci.getValue() : 0.0);
            }
        }

        // Extract frame metadata
        Integer frameIndex = null;
        Double timestamp = null;
        if (item.getExtra() != null) {
            for (HiveDetectResponse.OutputItem.ExtraItem extra : item.getExtra()) {
                if ("frame_index".equals(extra.getName())) {
                    frameIndex = ((Number) extra.getValue()).intValue();
                } else if ("timestamp".equals(extra.getName())) {
                    timestamp = ((Number) extra.getValue()).doubleValue();
                }
            }
        }

        double aiGeneratedScore = classMap.getOrDefault(CLASS_AI_GENERATED, 0.0);
        double notAiGeneratedScore = classMap.getOrDefault(CLASS_NOT_AI_GENERATED, 0.0);
        double deepfakeScore = classMap.getOrDefault(CLASS_DEEPFAKE, 0.0);
        double aiGeneratedAudioScore = classMap.getOrDefault(CLASS_AI_GENERATED_AUDIO, 0.0);
        double notAiGeneratedAudioScore = classMap.getOrDefault(CLASS_NOT_AI_GENERATED_AUDIO, 0.0);

        // Find the attributed generator with the highest score
        String attributedGenerator = null;
        double maxGeneratorScore = 0.0;
        for (Map.Entry<String, Double> entry : classMap.entrySet()) {
            if (GENERATOR_CLASSES.contains(entry.getKey())
                    && !CLASS_NONE.equals(entry.getKey())
                    && entry.getValue() > maxGeneratorScore) {
                maxGeneratorScore = entry.getValue();
                attributedGenerator = entry.getKey();
            }
        }

        return HiveDetectionResult.FrameResult.builder()
                .frameIndex(frameIndex != null ? frameIndex : 0)
                .timestamp(timestamp != null ? timestamp : 0.0)
                .aiGeneratedScore(aiGeneratedScore)
                .notAiGeneratedScore(notAiGeneratedScore)
                .deepfakeScore(deepfakeScore)
                .attributedGenerator(attributedGenerator)
                .aiGeneratedAudioScore(aiGeneratedAudioScore)
                .notAiGeneratedAudioScore(notAiGeneratedAudioScore)
                .build();
    }
}