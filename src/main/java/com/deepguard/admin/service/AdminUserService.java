package com.deepguard.admin.service;

import com.deepguard.admin.dto.AdminBillingHistoryResponse;
import com.deepguard.admin.dto.AdminUserDetailResponse;
import com.deepguard.admin.dto.AdminUserResponse;
import com.deepguard.admin.dto.UserStatsResponse;
import com.deepguard.billing.enums.PaymentStatus;
import com.deepguard.common.response.PageResponse;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.domain.Pageable;

public interface AdminUserService {

    PageResponse<AdminUserResponse> getAllUsers(String keyword, String status, String roleName, Pageable pageable);

    AdminUserDetailResponse getUserDetail(String userId);

    void updateUserStatus(String userId, String status);

    void updateUserRole(String userId, String roleName);

    UserStatsResponse getUserStats();

    PageResponse<AdminBillingHistoryResponse> getAllBillingHistory(
            String keyword,
            PaymentStatus status,
            String paymentMethod,
            LocalDateTime startDate,
            LocalDateTime endDate,
            Pageable pageable);
}
