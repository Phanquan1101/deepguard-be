package com.deepguard.media.service;

import com.deepguard.auth.entity.User;
import com.deepguard.billing.enums.ActionType;
import com.deepguard.billing.service.UserCreditService;
import com.deepguard.common.exception.BusinessException;
import com.deepguard.common.exception.ErrorCode;
import com.deepguard.common.response.PageResponse;
import com.deepguard.media.dto.response.AdminMediaResponse;
import com.deepguard.media.dto.response.MediaFileResponse;
import com.deepguard.media.entity.MediaFile;
import com.deepguard.media.enums.FileType;
import com.deepguard.media.enums.UploadStatus;
import com.deepguard.media.mapper.ToMediaFileMapper;
import com.deepguard.media.repository.MediaFileRepository;
import com.deepguard.scan.dto.response.AIDetectResponse;
import com.deepguard.scan.dto.response.HiveDetectionResult;
import com.deepguard.scan.service.ScanJobService;
import com.deepguard.scan.service.ScanJobServiceImpl;
import com.deepguard.security.userdetails.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class MediaFileServiceImpl implements MediaFileService {

    private static final String IMAGE_FOLDER = "image";
    private static final String VIDEO_FOLDER = "video";
    private static final String AUDIO_FOLDER = "audio";

    private final MediaFileRepository mediaFileRepository;
    private final SupabaseStorageService supabaseStorageService;
    private final ToMediaFileMapper toMediaFileMapper;
    private final ScanJobService scanJobService;
    private final UserCreditService userCreditService;

    private User getCurrentAuthenticatedUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof CustomUserDetails principal)) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        return principal.getUser();
    }

    /**
     * Uploads a file to Supabase Storage and returns the public URL.
     *
     * @param file the multipart file to upload
     * @return the public URL of the uploaded file
     */
    @Override
    public MediaFileResponse uploadFile(MultipartFile file) {
        User currentUser = getCurrentAuthenticatedUser();

        String contentType = file.getContentType();
        String folder = determineFolder(contentType);
        FileType fileType = determineFileType(contentType);

        if (fileType == FileType.IMAGE) {
            userCreditService.validateEnoughCredits(currentUser, ActionType.IMAGE_SCAN);
        } else if (fileType == FileType.VIDEO) {
            userCreditService.validateEnoughCredits(currentUser, ActionType.VIDEO_SCAN);
        } else if (fileType == FileType.AUDIO) {
            userCreditService.validateEnoughCredits(currentUser, ActionType.AUDIO_SCAN);
        }

        String publicUrl = null;

        try {
            // UPLOAD FILE
            publicUrl = supabaseStorageService.uploadFile(file, folder);

            // SAVE MEDIA
            MediaFile mediaFile = MediaFile.builder()
                    .user(currentUser)
                    .fileName(file.getOriginalFilename())
                    .originalUrl(publicUrl)
                    .fileType(fileType)
                    .fileSize(file.getSize())
                    .uploadStatus(UploadStatus.COMPLETED)
                    .uploadedAt(LocalDateTime.now())
                    .build();

            MediaFile savedMediaFile = mediaFileRepository.save(mediaFile);

            // CONSUME CREDIT AFTER SUCCESSFUL UPLOAD
            if (fileType == FileType.IMAGE) {
                userCreditService.consumeCredits(currentUser, ActionType.IMAGE_SCAN);
            } else if (fileType == FileType.VIDEO) {
                userCreditService.consumeCredits(currentUser, ActionType.VIDEO_SCAN);
            } else if (fileType == FileType.AUDIO) {
                userCreditService.consumeCredits(currentUser, ActionType.AUDIO_SCAN);
            }

            // AI DETECT
            AIDetectResponse aiResponse = null;
            HiveDetectionResult hiveResult = null;

            if (fileType == FileType.IMAGE) {
                aiResponse = scanJobService.createScanJobAndResult(savedMediaFile, publicUrl, currentUser);
            } else if (fileType == FileType.VIDEO) {
                hiveResult = scanJobService.createVideoScanJobAndResult(savedMediaFile, publicUrl, currentUser);
            } else if (fileType == FileType.AUDIO) {
                hiveResult = scanJobService.createAudioScanJobAndResult(savedMediaFile, publicUrl, currentUser);
            }

            return MediaFileResponse.builder()
                    .id(savedMediaFile.getId())
                    .userId(savedMediaFile.getUser().getId())
                    .fileName(savedMediaFile.getFileName())
                    .originalUrl(savedMediaFile.getOriginalUrl())
                    .fileType(savedMediaFile.getFileType().name())
                    .fileSize(savedMediaFile.getFileSize())
                    .uploadedAt(savedMediaFile.getUploadedAt())
                    .aiDetect(aiResponse)
                    .hiveDetect(hiveResult)
                    .build();

        } catch (Exception e) {

            log.error("Upload or AI detection failed: {}", e.getMessage());

            // REFUND IF CREDIT ALREADY CONSUMED
            if (fileType == FileType.IMAGE) {
                userCreditService.refundCredit(currentUser, ActionType.IMAGE_SCAN);
            } else if (fileType == FileType.VIDEO) {
                userCreditService.refundCredit(currentUser, ActionType.VIDEO_SCAN);
            } else if (fileType == FileType.AUDIO) {
                userCreditService.refundCredit(currentUser, ActionType.AUDIO_SCAN);
            }
            throw new RuntimeException("Upload or AI detection failed", e);
        }
    }

    private String determineFolder(String contentType) {
        if (contentType == null) {
            throw new BusinessException(ErrorCode.FILE_UPLOAD_FAILED);
        }
        if (contentType.startsWith("image/")) {
            return IMAGE_FOLDER;
        }
        if (contentType.startsWith("video/")) {
            return VIDEO_FOLDER;
        }
        if (contentType.startsWith("audio/")) {
            return AUDIO_FOLDER;
        }
        throw new BusinessException(ErrorCode.FILE_UPLOAD_FAILED, "Unsupported file type");
    }

    private FileType determineFileType(String contentType) {
        if (contentType == null) {
            throw new BusinessException(ErrorCode.FILE_UPLOAD_FAILED);
        }
        if (contentType.startsWith("image/")) {
            return FileType.IMAGE;
        }
        if (contentType.startsWith("video/")) {
            return FileType.VIDEO;
        }
        if (contentType.startsWith("audio/")) {
            return FileType.AUDIO;
        }
        throw new BusinessException(ErrorCode.FILE_UPLOAD_FAILED, "Unsupported file type");
    }

    /**
     * Retrieves the media files uploaded by the currently authenticated user.
     *
     * @return a list of MediaFileResponse objects representing the user's media files
     */
    @Override
    public PageResponse<MediaFileResponse> getMyMediaFiles(
            LocalDateTime startDate,
            LocalDateTime endDate,
            Pageable pageable
    ) {
        User currentUser = getCurrentAuthenticatedUser();
        if (pageable == null || pageable.isUnpaged()) {
            pageable = PageRequest.of(0, 10, Sort.by("uploadedAt").descending());
        }
        Page<MediaFile> page;
        if (startDate != null && endDate != null) {
            page = mediaFileRepository.findByUserIdAndUploadedAtBetween(
                    currentUser.getId(), startDate, endDate, pageable);

        } else if (startDate != null) {
            page = mediaFileRepository.findByUserIdAndUploadedAtGreaterThanEqual(
                    currentUser.getId(), startDate, pageable);

        } else if (endDate != null) {
            page = mediaFileRepository.findByUserIdAndUploadedAtLessThanEqual(
                    currentUser.getId(), endDate, pageable);

        } else {
            page = mediaFileRepository.findByUserId(
                    currentUser.getId(), pageable);
        }
        List<MediaFileResponse> content = page.getContent().stream()
                .map(toMediaFileMapper::mapToMediaFileResponse)
                .toList();
        return PageResponse.<MediaFileResponse>builder()
                .content(content)
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .last(page.isLast())
                .build();
    }

    /**
     * Deletes a media file by its ID if it belongs to the currently authenticated user.
     *
     * @param mediaId the ID of the media file to delete
     * @return a MediaFileResponse object representing the deleted media file
     */
    @Override
    public MediaFileResponse deleteMedia(String mediaId) {
        User currentUser = getCurrentAuthenticatedUser();
        MediaFile mediaFile = mediaFileRepository.findById(mediaId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEDIA_NOT_FOUND));
        if (!mediaFile.getUser().getId().equals(currentUser.getId())) {
            throw new BusinessException(ErrorCode.MEDIA_NOT_USER);
        }
        String publicUrl = mediaFile.getOriginalUrl();
        String filePath = extractFilePath(publicUrl);
        supabaseStorageService.deleteFile(filePath);
        mediaFileRepository.delete(mediaFile);
        return toMediaFileMapper.mapToMediaFileResponse(mediaFile);
    }

    private String extractFilePath(String publicUrl) {
        String prefix = "/object/public/deepguard-storage/";
        int index = publicUrl.indexOf(prefix);
        if (index == -1) {
            throw new BusinessException(ErrorCode.INVALID_MEDIA_URL);
        }
        return publicUrl.substring(index + prefix.length()
        );
    }

    /**
     * Retrieves all media files uploaded by all users.
     *
     * @return a list of MediaFileResponse objects representing all media files
     */
    @Override
    public PageResponse<AdminMediaResponse> getAllMedia(LocalDateTime startDate, LocalDateTime endDate, Pageable pageable) {
        Page<MediaFile> mediaPage = mediaFileRepository.findAllWithFilters(startDate, endDate, pageable);

        List<AdminMediaResponse> items = mediaPage.getContent().stream()
                        .map(media -> AdminMediaResponse.builder()
                                .id(media.getId())
                                .fileName(media.getFileName())
                                .originalUrl(media.getOriginalUrl())
                                .fileType(media.getFileType().name())
                                .fileSize(media.getFileSize())
                                .uploadedAt(media.getUploadedAt())
                                .userId(media.getUser().getId())
                                .email(media.getUser().getEmail())
                                .build())
                        .toList();

        return PageResponse.<AdminMediaResponse>builder()
                .content(items)
                .page(mediaPage.getNumber())
                .size(mediaPage.getSize())
                .totalElements(mediaPage.getTotalElements())
                .totalPages(mediaPage.getTotalPages())
                .last(mediaPage.isLast())
                .build();
    }

    /**
     * Retrieves a media file by its ID if it belongs to the currently authenticated user.
     *
     * @param mediaFileId the ID of the media file to retrieve
     * @return a MediaFileResponse object representing the retrieved media file
     */
    @Override
    public MediaFileResponse getMyMediaFileById(String mediaFileId) {
        User currentUser = getCurrentAuthenticatedUser();
        MediaFile mediaFile = mediaFileRepository.findByIdAndUserId(mediaFileId, currentUser.getId());
        if (mediaFile == null) {
            throw new BusinessException(ErrorCode.MEDIA_NOT_FOUND);
        }
        if (!mediaFile.getUser().getId().equals(currentUser.getId())) {
            throw new BusinessException(ErrorCode.MEDIA_NOT_USER);
        }
        return toMediaFileMapper.mapToMediaFileResponse(mediaFile);
    }
}
