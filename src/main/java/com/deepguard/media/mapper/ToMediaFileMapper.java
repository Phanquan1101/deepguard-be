package com.deepguard.media.mapper;

import com.deepguard.media.dto.response.MediaFileResponse;
import com.deepguard.media.entity.MediaFile;
import com.deepguard.scan.dto.response.AIDetectResponse;
import com.deepguard.scan.entity.DetectionResult;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import org.springframework.stereotype.Component;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class ToMediaFileMapper {

    private final EntityManager entityManager;

    public MediaFileResponse mapToMediaFileResponse(MediaFile mediaFile) {
        AIDetectResponse aiDetect = null;

        // Try to load the latest detection result for this media (if any)
        try {
            DetectionResult dr = entityManager.createQuery(
                            "SELECT d FROM DetectionResult d WHERE d.scanJob.mediaFile.id = :mediaId ORDER BY d.processedAt DESC", DetectionResult.class)
                    .setParameter("mediaId", mediaFile.getId())
                    .setMaxResults(1)
                    .getSingleResult();

            if (dr != null) {
                aiDetect = AIDetectResponse.builder()
                        .prediction(dr.getResultLabel() != null ? dr.getResultLabel().name().toLowerCase() : null)
                        .realProbability(dr.getConfidence() != null ? dr.getConfidence().doubleValue() : null)
                        .imageUrl(null)
                        .message(dr.getModelVersion())
                        .build();
            }
        } catch (NoResultException ignored) {
            // no detection result exists
        }

        return MediaFileResponse.builder()
                .id(mediaFile.getId())
                .userId(mediaFile.getUser().getId())
                .fileName(mediaFile.getFileName())
                .originalUrl(mediaFile.getOriginalUrl())
                .fileType(mediaFile.getFileType().name())
                .fileSize(mediaFile.getFileSize())
                .uploadedAt(mediaFile.getUploadedAt())
                .aiDetect(aiDetect)
                .build();
    }

}
