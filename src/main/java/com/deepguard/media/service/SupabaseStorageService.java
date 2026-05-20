package com.deepguard.media.service;

import com.deepguard.common.exception.BusinessException;
import com.deepguard.common.exception.ErrorCode;
import com.deepguard.media.config.SupabaseStorageConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class SupabaseStorageService {

    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024; // 5MB
    private static final List<String> ALLOWED_CONTENT_TYPES = Arrays.asList(
            "image/jpeg", "image/png", "image/gif", "image/webp", "image/avif"
    );

    private final SupabaseStorageConfig storageConfig;
    private final RestTemplate supabaseRestTemplate;

    /**
     * Uploads a file to Supabase Storage and returns the public URL.
     *
     * @param file   the multipart file to upload
     * @param folder the folder path within the bucket (e.g., "avatars")
     * @return the public URL of the uploaded file
     */
    public String uploadFile(MultipartFile file, String folder) {
        validateFile(file);

        String originalFilename = file.getOriginalFilename();
        String extension = getFileExtension(originalFilename);
        String uniqueFileName = UUID.randomUUID() + extension;
        String filePath = folder + "/" + uniqueFileName;

        try {
            String uploadUrl = storageConfig.getUploadUrl(filePath);

            HttpHeaders headers = new HttpHeaders();
            headers.set("apikey", storageConfig.getServiceKey());
            headers.set("Authorization", "Bearer " + storageConfig.getServiceKey());
            headers.setContentType(MediaType.parseMediaType(file.getContentType()));

            HttpEntity<byte[]> requestEntity = new HttpEntity<>(file.getBytes(), headers);

            ResponseEntity<String> response = supabaseRestTemplate.exchange(
                    uploadUrl,
                    HttpMethod.POST,
                    requestEntity,
                    String.class
            );

            if (!response.getStatusCode().is2xxSuccessful()) {
                log.error("Supabase upload failed with status: {} body: {}",
                        response.getStatusCode(), response.getBody());
                throw new BusinessException(ErrorCode.FILE_UPLOAD_FAILED);
            }

            String publicUrl = storageConfig.getPublicUrlBase() + "/" + filePath;
            log.info("File uploaded successfully to Supabase: {}", publicUrl);
            return publicUrl;

        } catch (IOException e) {
            log.error("Failed to read file bytes", e);
            throw new BusinessException(ErrorCode.FILE_UPLOAD_FAILED, "Failed to read file content");
        } catch (RestClientException e) {
            log.error("Supabase API call failed", e);
            throw new BusinessException(ErrorCode.FILE_UPLOAD_FAILED, "Failed to upload file to storage");
        }
    }

    /**
     * Deletes a file from Supabase Storage by its file path.
     *
     * @param filePath the path of the file within the bucket
     */
    public void deleteFile(String filePath) {
        try {
            String deleteUrl = storageConfig.getUploadUrl(filePath);

            HttpHeaders headers = new HttpHeaders();
            headers.set("apikey", storageConfig.getServiceKey());
            headers.set("Authorization", "Bearer " + storageConfig.getServiceKey());

            HttpEntity<Void> requestEntity = new HttpEntity<>(headers);

            supabaseRestTemplate.exchange(
                    deleteUrl,
                    HttpMethod.DELETE,
                    requestEntity,
                    String.class
            );

            log.info("File deleted successfully from Supabase: {}", filePath);
        } catch (RestClientException e) {
            log.warn("Failed to delete file from Supabase: {}", filePath, e);
        }
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ErrorCode.FILE_UPLOAD_FAILED, "File is empty");
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            throw new BusinessException(ErrorCode.FILE_UPLOAD_FAILED,
                    "File size exceeds maximum allowed size of 5MB");
        }

        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType)) {
            throw new BusinessException(ErrorCode.FILE_UPLOAD_FAILED,
                    "Invalid file type. Allowed types: JPEG, PNG, GIF, WebP");
        }
    }

    private String getFileExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "";
        }
        return filename.substring(filename.lastIndexOf("."));
    }
}
