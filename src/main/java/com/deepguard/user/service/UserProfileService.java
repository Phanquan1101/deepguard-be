package com.deepguard.user.service;

import com.deepguard.user.dto.request.CreateUserProfileRequest;
import com.deepguard.user.dto.request.UpdateUserProfileRequest;
import com.deepguard.user.dto.response.UserProfileResponse;
import org.springframework.web.multipart.MultipartFile;

public interface UserProfileService {

    UserProfileResponse createUserProfile(CreateUserProfileRequest request, MultipartFile avatar);

    UserProfileResponse getMyProfile();

    UserProfileResponse updateMyProfile(UpdateUserProfileRequest request, MultipartFile avatar);
}
