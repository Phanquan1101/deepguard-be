package com.deepguard.admin.service;

import com.deepguard.admin.dto.AdminUserDetailResponse;
import com.deepguard.admin.dto.AdminUserResponse;
import com.deepguard.admin.dto.UserStatsResponse;
import com.deepguard.auth.entity.Role;
import com.deepguard.auth.entity.User;
import com.deepguard.auth.enums.UserStatus;
import com.deepguard.auth.repository.RoleRepository;
import com.deepguard.auth.repository.UserRepository;
import com.deepguard.common.exception.BusinessException;
import com.deepguard.common.exception.ErrorCode;
import com.deepguard.common.response.PageResponse;
import com.deepguard.media.repository.MediaFileRepository;
import com.deepguard.scan.entity.ScanJob;
import com.deepguard.scan.repository.ScanJobRepository;
import com.deepguard.user.entity.UserProfile;
import com.deepguard.user.repository.UserProfileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminUserServiceImpl implements AdminUserService {

    private final UserRepository userRepository;
    private final UserProfileRepository userProfileRepository;
    private final RoleRepository roleRepository;
    private final ScanJobRepository scanJobRepository;
    private final MediaFileRepository mediaFileRepository;

    @Override
    @Transactional(readOnly = true)
    public PageResponse<AdminUserResponse> getAllUsers(String keyword, String status, String roleName, Pageable pageable) {
        log.info("Admin fetching users with keyword: {}, status: {}, role: {}", keyword, status, roleName);
        Page<User> userPage = userRepository.findAllWithFilters(keyword, status, roleName, pageable);

        List<AdminUserResponse> responses = userPage.getContent().stream()
                .map(this::mapToAdminUserResponse)
                .toList();

        return PageResponse.<AdminUserResponse>builder()
                .content(responses)
                .page(userPage.getNumber())
                .size(userPage.getSize())
                .totalElements(userPage.getTotalElements())
                .totalPages(userPage.getTotalPages())
                .last(userPage.isLast())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public AdminUserDetailResponse getUserDetail(String userId) {
        log.info("Admin fetching user detail for id: {}", userId);
        User user = userRepository.findWithRoleById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        Optional<UserProfile> profileOpt = userProfileRepository.findByUserId(userId);
        
        long totalScanJobs = scanJobRepository.countByUserId(userId);
        long totalMediaFiles = mediaFileRepository.countByUserId(userId);
        
        Optional<ScanJob> lastScanJobOpt = scanJobRepository.findFirstByUserIdOrderByStartedAtDesc(userId);

        AdminUserDetailResponse.AdminUserDetailResponseBuilder builder = AdminUserDetailResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .username(user.getUsername())
                .role(user.getRole().getName())
                .status(user.getStatus())
                .isVerified(user.getIsVerified())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .totalScanJobs(totalScanJobs)
                .totalMediaFiles(totalMediaFiles)
                .lastScanAt(lastScanJobOpt.map(ScanJob::getStartedAt).orElse(null));

        profileOpt.ifPresent(profile -> {
            builder.fullName(profile.getFullName())
                   .avatarUrl(profile.getAvatarUrl())
                   .bio(profile.getBio())
                   .profileCreatedAt(profile.getCreatedAt());
        });

        return builder.build();
    }

    @Override
    @Transactional
    public void updateUserStatus(String userId, String status) {
        log.info("Admin updating status for user: {} to {}", userId, status);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        
        try {
            UserStatus.valueOf(status); // Validate status string
        } catch (IllegalArgumentException e) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Invalid user status: " + status);
        }

        user.setStatus(status);
        userRepository.save(user);
    }

    @Override
    @Transactional
    public void updateUserRole(String userId, String roleName) {
        log.info("Admin updating role for user: {} to {}", userId, roleName);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        Role newRole = roleRepository.findByName(roleName)
                .orElseThrow(() -> new BusinessException(ErrorCode.ROLE_NOT_FOUND));

        user.setRole(newRole);
        userRepository.save(user);
    }

    @Override
    @Transactional(readOnly = true)
    public UserStatsResponse getUserStats() {
        log.info("Admin fetching user stats");
        long total = userRepository.count();
        long active = userRepository.countByStatus(UserStatus.ACTIVE.name());
        long suspended = userRepository.countByStatus(UserStatus.SUSPENDED.name());
        long deleted = userRepository.countByStatus(UserStatus.DELETED.name());
        long pending = userRepository.countByStatus(UserStatus.PENDING_VERIFICATION.name());
        long admins = userRepository.countByRoleName("ROLE_ADMIN");

        return UserStatsResponse.builder()
                .totalUsers(total)
                .activeUsers(active)
                .suspendedUsers(suspended)
                .deletedUsers(deleted)
                .pendingVerificationUsers(pending)
                .totalAdmins(admins)
                .build();
    }

    private AdminUserResponse mapToAdminUserResponse(User user) {
        Optional<UserProfile> profileOpt = userProfileRepository.findByUserId(user.getId());
        
        return AdminUserResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .username(user.getUsername())
                .role(user.getRole().getName())
                .status(user.getStatus())
                .isVerified(user.getIsVerified())
                .fullName(profileOpt.map(UserProfile::getFullName).orElse(null))
                .avatarUrl(profileOpt.map(UserProfile::getAvatarUrl).orElse(null))
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }
}
