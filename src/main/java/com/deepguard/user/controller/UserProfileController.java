package com.deepguard.user.controller;

import com.deepguard.common.response.ApiResponse;
import com.deepguard.user.dto.request.CreateUserProfileRequest;
import com.deepguard.user.dto.request.UpdateUserProfileRequest;
import com.deepguard.user.dto.response.UserProfileResponse;
import com.deepguard.user.service.UserProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/user-profiles")
@RequiredArgsConstructor
public class UserProfileController {

    private final UserProfileService userProfileService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<UserProfileResponse>> createUserProfile(
            @RequestParam(value = "fullName", required = false) String fullName,
            @RequestParam(value = "bio", required = false) String bio,
            @RequestParam(value = "avatar", required = false) MultipartFile avatar) {

        CreateUserProfileRequest request = CreateUserProfileRequest.builder()
                .fullName(fullName)
                .bio(bio)
                .build();

        UserProfileResponse response = userProfileService.createUserProfile(request, avatar);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("User profile created successfully", response));
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserProfileResponse>> getMyProfile() {
        UserProfileResponse response = userProfileService.getMyProfile();
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PutMapping(value = "/me", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<UserProfileResponse>> updateMyProfile(
            @RequestParam(value = "fullName", required = false) String fullName,
            @RequestParam(value = "bio", required = false) String bio,
            @RequestParam(value = "avatar", required = false) MultipartFile avatar) {

        UpdateUserProfileRequest request = UpdateUserProfileRequest.builder()
                .fullName(fullName)
                .bio(bio)
                .build();

        UserProfileResponse response = userProfileService.updateMyProfile(request, avatar);
        return ResponseEntity.ok(ApiResponse.success("User profile updated successfully", response));
    }
}
