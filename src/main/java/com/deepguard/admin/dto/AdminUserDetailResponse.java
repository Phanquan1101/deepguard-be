package com.deepguard.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Full user detail for admin — includes profile info and activity stats.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminUserDetailResponse {

    // ── Auth info ──────────────────────────────────────────────────────────────
    private String id;
    private String email;
    private String username;
    private String role;
    private String status;
    private Boolean isVerified;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // ── Profile info ───────────────────────────────────────────────────────────
    private String fullName;
    private String avatarUrl;
    private String bio;
    private LocalDateTime profileCreatedAt;

    // ── Activity stats ─────────────────────────────────────────────────────────
    private Long totalScanJobs;
    private Long totalMediaFiles;
    private LocalDateTime lastScanAt;
}
