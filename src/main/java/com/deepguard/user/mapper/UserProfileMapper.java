package com.deepguard.user.mapper;

import com.deepguard.user.dto.response.UserProfileResponse;
import com.deepguard.user.entity.UserProfile;
import org.springframework.stereotype.Component;

@Component
public class UserProfileMapper {

    public UserProfileResponse toUserProfileResponse(UserProfile userProfile) {
        if (userProfile == null) {
            return null;
        }

        return UserProfileResponse.builder()
                .id(userProfile.getId())
                .userId(userProfile.getUser() != null ? userProfile.getUser().getId() : null)
                .fullName(userProfile.getFullName())
                .avatarUrl(userProfile.getAvatarUrl())
                .bio(userProfile.getBio())
                .createdAt(userProfile.getCreatedAt())
                .updatedAt(userProfile.getUpdatedAt())
                .build();
    }
}
