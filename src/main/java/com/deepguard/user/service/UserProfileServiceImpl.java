package com.deepguard.user.service;

import com.deepguard.auth.entity.User;
import com.deepguard.common.exception.BusinessException;
import com.deepguard.common.exception.ErrorCode;
import com.deepguard.media.service.SupabaseStorageService;
import com.deepguard.security.userdetails.CustomUserDetails;
import com.deepguard.user.dto.request.CreateUserProfileRequest;
import com.deepguard.user.dto.request.UpdateUserProfileRequest;
import com.deepguard.user.dto.response.UserProfileResponse;
import com.deepguard.user.entity.UserProfile;
import com.deepguard.user.mapper.UserProfileMapper;
import com.deepguard.user.repository.UserProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class UserProfileServiceImpl implements UserProfileService {

    private static final String AVATAR_FOLDER = "avatars";

    private final UserProfileRepository userProfileRepository;
    private final UserProfileMapper userProfileMapper;
    private final SupabaseStorageService supabaseStorageService;

    @Override
    @Transactional
    public UserProfileResponse createUserProfile(CreateUserProfileRequest request, MultipartFile avatar) {
        User currentUser = getCurrentAuthenticatedUser();

        // Check if user already has a profile
        if (userProfileRepository.existsByUserId(currentUser.getId())) {
            throw new BusinessException(ErrorCode.USER_PROFILE_ALREADY_EXISTS);
        }

        // Upload avatar to Supabase Storage if provided
        String avatarUrl = null;
        if (avatar != null && !avatar.isEmpty()) {
            avatarUrl = supabaseStorageService.uploadFile(avatar, AVATAR_FOLDER);
        }

        UserProfile userProfile = UserProfile.builder()
                .user(currentUser)
                .fullName(request.getFullName())
                .avatarUrl(avatarUrl)
                .bio(request.getBio())
                .build();

        UserProfile savedProfile = userProfileRepository.save(userProfile);
        return userProfileMapper.toUserProfileResponse(savedProfile);
    }

    @Override
    @Transactional(readOnly = true)
    public UserProfileResponse getMyProfile() {
        User currentUser = getCurrentAuthenticatedUser();

        UserProfile userProfile = userProfileRepository.findByUserId(currentUser.getId())
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_PROFILE_NOT_FOUND));

        return userProfileMapper.toUserProfileResponse(userProfile);
    }

    @Override
    @Transactional
    public UserProfileResponse updateMyProfile(UpdateUserProfileRequest request, MultipartFile avatar) {
        User currentUser = getCurrentAuthenticatedUser();

        UserProfile userProfile = userProfileRepository.findByUserId(currentUser.getId())
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_PROFILE_NOT_FOUND));

        // Update fields if provided
        if (request.getFullName() != null) {
            userProfile.setFullName(request.getFullName());
        }
        if (request.getBio() != null) {
            userProfile.setBio(request.getBio());
        }

        // Upload new avatar if provided
        if (avatar != null && !avatar.isEmpty()) {
            // Delete old avatar from Supabase if exists
            String oldAvatarUrl = userProfile.getAvatarUrl();
            if (oldAvatarUrl != null && !oldAvatarUrl.isBlank()) {
                String oldFilePath = extractFilePathFromUrl(oldAvatarUrl);
                if (oldFilePath != null) {
                    supabaseStorageService.deleteFile(oldFilePath);
                }
            }

            // Upload new avatar
            String newAvatarUrl = supabaseStorageService.uploadFile(avatar, AVATAR_FOLDER);
            userProfile.setAvatarUrl(newAvatarUrl);
        }

        UserProfile updatedProfile = userProfileRepository.save(userProfile);
        return userProfileMapper.toUserProfileResponse(updatedProfile);
    }

    private User getCurrentAuthenticatedUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof CustomUserDetails principal)) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        return principal.getUser();
    }

    /**
     * Extracts the file path (e.g., "avatars/uuid.jpg") from a full Supabase public URL.
     */
    private String extractFilePathFromUrl(String url) {
        if (url == null) return null;
        String marker = "/storage/v1/object/public/";
        int markerIndex = url.indexOf(marker);
        if (markerIndex == -1) return null;

        // After marker: "bucket-name/avatars/uuid.jpg"
        String afterMarker = url.substring(markerIndex + marker.length());
        // Remove bucket name prefix
        int slashIndex = afterMarker.indexOf("/");
        if (slashIndex == -1) return null;
        return afterMarker.substring(slashIndex + 1);
    }
}
