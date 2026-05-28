package com.deepguard.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Lightweight user record for paginated list view.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminUserResponse {
    private String id;
    private String email;
    private String username;
    private String role;
    private String status;
    private Boolean isVerified;
    private String fullName;
    private String avatarUrl;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
