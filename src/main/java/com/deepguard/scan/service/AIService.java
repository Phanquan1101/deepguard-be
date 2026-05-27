package com.deepguard.scan.service;

import com.deepguard.scan.dto.response.AIDetectResponse;
import org.springframework.web.multipart.MultipartFile;

public interface AIService {

    /**
     * Sends an image file to the Python AI server for deepfake detection.
     *
     * @param file the image file to be analyzed
     * @return AIDetectResponse containing label, confidence score, and optional message
     */
    AIDetectResponse detect(MultipartFile file);

    /**
     * Sends an image URL to the Python AI server for deepfake detection.
     *
     * @param imageUrl the URL of the image to be analyzed
     * @return AIDetectResponse containing label, confidence score, and optional message
     */
    AIDetectResponse detectFromUrl(String imageUrl);
}
