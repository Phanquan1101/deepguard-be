package com.deepguard.scan.controller;

import com.deepguard.scan.dto.response.AIDetectResponse;
import com.deepguard.scan.service.AIService;
import com.deepguard.common.response.ApiResponse;
import com.deepguard.media.service.SupabaseStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
// @RestController removed: deprecated
// @RequestMapping("/api/ai")
@RequiredArgsConstructor
public class DetectController {

    private final AIService aiService;
    private final SupabaseStorageService supabaseStorageService;

    /**
     * Endpoint to detect deepfake in an uploaded image.
     * POST /api/ai/detect
     *
     * @param file the image file to be analyzed (multipart/form-data, field name = "image")
     * @return ApiResponse containing detection result (label + confidence)
     */
    @PostMapping(value = "/detect", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<AIDetectResponse>> detect(
            @RequestParam("image") MultipartFile file
    ) {
        AIDetectResponse result = aiService.detect(file);
        // quick console output for debugging score
        System.out.println("AI SCORE (detect): " + (result != null ? result.getScore() : "null"));
        return ResponseEntity.ok(ApiResponse.success("AI detection completed successfully", result));
    }

    /**
     * Endpoint to detect deepfake by uploading image to Supabase first.
     * FE uploads → Spring uploads to Supabase → Spring calls Python AI with imageUrl → returns result
     * POST /api/ai/detect-with-upload
     *
     * @param file the image file to be uploaded and analyzed
     * @return ApiResponse containing detection result (label + confidence + message)
     */
    @PostMapping(value = "/detect-with-upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<AIDetectResponse>> detectWithUpload(
            @RequestParam("image") MultipartFile file
    ) {
        log.info("Starting detection with upload for file: {}", file.getOriginalFilename());

        String imageUrl = supabaseStorageService.uploadFile(file, "images");
        log.info("Image uploaded to Supabase: {}", imageUrl);

        // debug: immediate console output to confirm flow reaches here
        System.out.println("Calling AI with URL: " + imageUrl);

        AIDetectResponse result = null;
        try {
            result = aiService.detectFromUrl(imageUrl);
        } catch (Exception e) {
            // print stacktrace to console for easier debugging during development
            e.printStackTrace();
            log.error("AI detection failed for URL {}: {}", imageUrl, e.getMessage(), e);
            throw e; // rethrow so GlobalExceptionHandler handles it and client gets error response
        }

        log.info("AI detection completed: label={}, score={}", result.getLabel(), result.getScore());
        // quick console output for debugging score
        System.out.println("AI SCORE (detect-with-upload): " + (result != null ? result.getScore() : "null"));
        return ResponseEntity.ok(ApiResponse.success("AI detection completed successfully", result));
    }
}
