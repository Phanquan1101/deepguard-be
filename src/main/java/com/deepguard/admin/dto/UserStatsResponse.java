package com.deepguard.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * High-level statistics about users for the admin dashboard.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserStatsResponse {
    private long totalUsers;
    private long activeUsers;
    private long suspendedUsers;
    private long deletedUsers;
    private long pendingVerificationUsers;
    private long totalAdmins;
}
