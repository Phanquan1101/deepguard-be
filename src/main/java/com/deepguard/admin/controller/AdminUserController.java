package com.deepguard.admin.controller;

import com.deepguard.admin.dto.AdminBillingHistoryResponse;
import com.deepguard.admin.dto.AdminUserDetailResponse;
import com.deepguard.admin.dto.AdminUserResponse;
import com.deepguard.admin.dto.UpdateUserRoleRequest;
import com.deepguard.admin.dto.UpdateUserStatusRequest;
import com.deepguard.admin.dto.UserStatsResponse;
import com.deepguard.admin.service.AdminUserService;
import com.deepguard.billing.enums.PaymentStatus;
import com.deepguard.common.response.ApiResponse;
import com.deepguard.common.response.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
@Tag(name = "Admin User Management", description = "APIs for administrators to manage users")
@PreAuthorize("hasRole('ADMIN')")
public class AdminUserController {

    private final AdminUserService adminUserService;

    @GetMapping
    @Operation(summary = "Get all users with pagination and filtering")
    public ResponseEntity<ApiResponse<PageResponse<AdminUserResponse>>> getAllUsers(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String roleName,
            @ParameterObject Pageable pageable) {
        
        PageResponse<AdminUserResponse> response = adminUserService.getAllUsers(keyword, status, roleName, pageable);
        return ResponseEntity.ok(ApiResponse.success("Users retrieved successfully", response));
    }

    @GetMapping("/{userId}")
    @Operation(summary = "Get detailed information about a specific user")
    public ResponseEntity<ApiResponse<AdminUserDetailResponse>> getUserDetail(@PathVariable String userId) {
        AdminUserDetailResponse response = adminUserService.getUserDetail(userId);
        return ResponseEntity.ok(ApiResponse.success("User detail retrieved successfully", response));
    }

    @PutMapping("/{userId}/status")
    @Operation(summary = "Update a user's account status")
    public ResponseEntity<ApiResponse<Void>> updateUserStatus(
            @PathVariable String userId,
            @Valid @RequestBody UpdateUserStatusRequest request) {
        
        adminUserService.updateUserStatus(userId, request.getStatus());
        return ResponseEntity.ok(ApiResponse.success("User status updated successfully", null));
    }

    @PutMapping("/{userId}/role")
    @Operation(summary = "Update a user's role")
    public ResponseEntity<ApiResponse<Void>> updateUserRole(
            @PathVariable String userId,
            @Valid @RequestBody UpdateUserRoleRequest request) {
        
        adminUserService.updateUserRole(userId, request.getRoleName());
        return ResponseEntity.ok(ApiResponse.success("User role updated successfully", null));
    }

    @GetMapping("/stats")
    @Operation(summary = "Get high-level user statistics")
    public ResponseEntity<ApiResponse<UserStatsResponse>> getUserStats() {
        UserStatsResponse response = adminUserService.getUserStats();
        return ResponseEntity.ok(ApiResponse.success("User stats retrieved successfully", response));
    }

    @GetMapping("/billing-history")
    @Operation(summary = "Get billing history for all users with pagination and filtering")
    public ResponseEntity<ApiResponse<PageResponse<AdminBillingHistoryResponse>>> getAllBillingHistory(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) PaymentStatus status,
            @RequestParam(required = false) String paymentMethod,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            @ParameterObject Pageable pageable) {
        PageResponse<AdminBillingHistoryResponse> response = adminUserService.getAllBillingHistory(
                keyword, status, paymentMethod, startDate, endDate, pageable);
        return ResponseEntity.ok(ApiResponse.success("Billing history retrieved successfully", response));
    }
}
