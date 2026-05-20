package com.deepguard.user.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserProfileResponse {

    private String id;
    private String userId;
    private String fullName;
    private String avatarUrl;
    private String bio;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
