package com.deepguard.scan.service;

import com.deepguard.scan.config.AIProperties;
import com.deepguard.scan.dto.response.AIDetectResponse;
import com.deepguard.common.exception.BusinessException;
import com.deepguard.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.Map;

@Slf4j
// @Service removed: moved to ScanService
@RequiredArgsConstructor
public class AIServiceImpl implements AIService {

    private final AIProperties aiProperties;
    private final RestTemplate supabaseRestTemplate;

    /**
     * Sends the image to Python AI server via multipart/form-data POST request.
     *
     * @param file the image file to be analyzed
     * @return AIDetectResponse parsed from the Python server response
     */
    @Override
    public AIDetectResponse detect(MultipartFile file) {
        validateImageFile(file);

        String predictUrl = aiProperties.getPredictUrl();
        log.info("Sending image '{}' to AI server at: {}", file.getOriginalFilename(), predictUrl);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("image", file.getResource());

        HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<AIDetectResponse> response = supabaseRestTemplate.postForEntity(
                    predictUrl,
                    requestEntity,
                    AIDetectResponse.class
            );

            AIDetectResponse result = response.getBody();
            log.info("AI detection result for '{}': label={}, score={}",
                    file.getOriginalFilename(),
                    result != null ? result.getLabel() : "null",
                    result != null ? result.getScore() : "null");

            return result;

        } catch (RestClientException e) {
            log.error("Failed to connect to AI Python server at {}: {}", predictUrl, e.getMessage(), e);
            throw new BusinessException(ErrorCode.AI_SERVER_UNAVAILABLE, "Cannot connect to AI server: " + e.getMessage());
        }
    }

    /**
     * Sends image URL to Python AI server via JSON POST request.
     * This method is used when the image is already uploaded to Supabase storage.
     *
     * @param imageUrl the URL of the image to be analyzed
     * @return AIDetectResponse parsed from the Python server response
     */
    public AIDetectResponse detectFromUrl(String imageUrl) {
        if (imageUrl == null || imageUrl.trim().isEmpty()) {
            throw new BusinessException(ErrorCode.FILE_UPLOAD_FAILED, "Image URL is empty");
        }

        String predictUrl = aiProperties.getPredictUrl();
        log.info("Sending image URL to AI server at: {}", predictUrl);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> body = new HashMap<>();
        body.put("imageUrl", imageUrl);

        HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(body, headers);

        // debug: log outgoing payload
        log.info("AI request payload: {}", body);

        try {
            ResponseEntity<AIDetectResponse> response = supabaseRestTemplate.postForEntity(
                    predictUrl,
                    requestEntity,
                    AIDetectResponse.class
            );

            // debug: log HTTP status and body received from Python server
            log.info("AI response status={} body={}", response.getStatusCode().value(), response.getBody());

            AIDetectResponse result = response.getBody();
            log.info("AI detection result for URL '{}': label={}, score={}",
                    imageUrl,
                    result != null ? result.getLabel() : "null",
                    result != null ? result.getScore() : "null");

            return result;

        } catch (RestClientException e) {
            log.error("Failed to connect to AI Python server at {}: {}", predictUrl, e.getMessage(), e);
            throw new BusinessException(ErrorCode.AI_SERVER_UNAVAILABLE, "Cannot connect to AI server: " + e.getMessage());
        }
    }

    private void validateImageFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ErrorCode.FILE_UPLOAD_FAILED, "Image file is empty");
        }

        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new BusinessException(ErrorCode.FILE_UPLOAD_FAILED, "Only image files are supported (jpeg, png, etc.)");
        }
    }
}
