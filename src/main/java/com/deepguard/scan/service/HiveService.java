package com.deepguard.scan.service;

import com.deepguard.scan.dto.response.HiveDetectionResult;
import org.springframework.web.multipart.MultipartFile;

/**
 * Service for detecting AI-generated and deepfake content in images, videos, and audio
 * using Hive's AI-Generated and Deepfake Content Detection API.
 */
public interface HiveService {

    /**
     * Detect AI-generated/deepfake content from a media file (image, video, or audio).
     * Uploads the file to Supabase first, then sends the public URL to Hive API.
     *
     * @param file the media file to analyze
     * @return HiveDetectionResult with processed predictions
     */
    HiveDetectionResult detect(MultipartFile file);

    /**
     * Detect AI-generated/deepfake content from a publicly accessible media URL.
     *
     * @param mediaUrl publicly accessible URL of the media (image, video, or audio)
     * @return HiveDetectionResult with processed predictions
     */
    HiveDetectionResult detectFromUrl(String mediaUrl);
}