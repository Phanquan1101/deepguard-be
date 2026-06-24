package com.deepguard.admin.service;

import com.deepguard.admin.dto.AdminBillingHistoryResponse;
import com.deepguard.admin.dto.AdminUserDetailResponse;
import com.deepguard.admin.dto.AdminUserResponse;
import com.deepguard.admin.dto.UserStatsResponse;
import com.deepguard.auth.entity.Role;
import com.deepguard.auth.entity.User;
import com.deepguard.auth.enums.UserStatus;
import com.deepguard.auth.repository.RoleRepository;
import com.deepguard.auth.repository.UserRepository;
import com.deepguard.billing.entity.Payment;
import com.deepguard.billing.entity.PricingPlan;
import com.deepguard.billing.entity.Subscription;
import com.deepguard.billing.enums.PaymentStatus;
import com.deepguard.billing.repository.PaymentRepository;
import com.deepguard.common.exception.BusinessException;
import com.deepguard.common.exception.ErrorCode;
import com.deepguard.common.response.PageResponse;
import com.deepguard.media.repository.MediaFileRepository;
import com.deepguard.scan.entity.ScanJob;
import com.deepguard.scan.repository.ScanJobRepository;
import com.deepguard.user.entity.UserProfile;
import com.deepguard.user.repository.UserProfileRepository;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminUserServiceImpl implements AdminUserService {

    private final UserRepository userRepository;
    private final UserProfileRepository userProfileRepository;
    private final RoleRepository roleRepository;
    private final ScanJobRepository scanJobRepository;
    private final MediaFileRepository mediaFileRepository;
    private final PaymentRepository paymentRepository;

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

    @Override
    @Transactional(readOnly = true)
    public PageResponse<AdminBillingHistoryResponse> getAllBillingHistory(
            String keyword,
            PaymentStatus status,
            String paymentMethod,
            LocalDateTime startDate,
            LocalDateTime endDate,
            Pageable pageable) {
        log.info("Admin fetching billing history with keyword: {}, status: {}, paymentMethod: {}, startDate: {}, endDate: {}",
                keyword, status, paymentMethod, startDate, endDate);

        if (startDate != null && endDate != null && startDate.isAfter(endDate)) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "startDate must be before or equal to endDate");
        }

        Pageable effectivePageable = pageable;
        if (pageable.getSort().isUnsorted()) {
            effectivePageable = PageRequest.of(
                    pageable.getPageNumber(),
                    pageable.getPageSize(),
                    Sort.by(Sort.Direction.DESC, "createdAt"));
        }

        Specification<Payment> specification = buildBillingHistorySpecification(
                normalize(keyword), normalize(paymentMethod), status, startDate, endDate);

        Page<AdminBillingHistoryResponse> paymentPage = paymentRepository.findAll(specification, effectivePageable)
                .map(this::mapToAdminBillingHistoryResponse);

        return PageResponse.<AdminBillingHistoryResponse>builder()
                .content(paymentPage.getContent())
                .page(paymentPage.getNumber())
                .size(paymentPage.getSize())
                .totalElements(paymentPage.getTotalElements())
                .totalPages(paymentPage.getTotalPages())
                .last(paymentPage.isLast())
                .build();
    }

    private Specification<Payment> buildBillingHistorySpecification(
            String keyword,
            String paymentMethod,
            PaymentStatus status,
            LocalDateTime startDate,
            LocalDateTime endDate) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (keyword != null) {
                Join<Payment, User> user = root.join("user", JoinType.LEFT);
                String keywordPattern = "%" + keyword.toLowerCase(Locale.ROOT) + "%";
                predicates.add(criteriaBuilder.or(
                        criteriaBuilder.like(
                                criteriaBuilder.lower(root.<String>get("transactionCode")), keywordPattern),
                        criteriaBuilder.like(
                                criteriaBuilder.lower(user.<String>get("email")), keywordPattern),
                        criteriaBuilder.like(
                                criteriaBuilder.lower(user.<String>get("username")), keywordPattern)));
            }

            if (status != null) {
                predicates.add(criteriaBuilder.equal(root.get("status"), status));
            }

            if (paymentMethod != null) {
                predicates.add(criteriaBuilder.equal(
                        criteriaBuilder.lower(root.<String>get("paymentMethod")),
                        paymentMethod.toLowerCase(Locale.ROOT)));
            }

            if (startDate != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(
                        root.<LocalDateTime>get("createdAt"), startDate));
            }

            if (endDate != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(
                        root.<LocalDateTime>get("createdAt"), endDate));
            }

            return criteriaBuilder.and(predicates.toArray(Predicate[]::new));
        };
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }

        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private AdminBillingHistoryResponse mapToAdminBillingHistoryResponse(Payment payment) {
        User user = payment.getUser();
        Subscription subscription = payment.getSubscription();
        PricingPlan pricingPlan = subscription != null ? subscription.getPricingPlan() : null;

        return AdminBillingHistoryResponse.builder()
                .paymentId(payment.getId())
                .transactionCode(payment.getTransactionCode())
                .amount(payment.getAmount())
                .paymentMethod(payment.getPaymentMethod())
                .status(payment.getStatus())
                .createdAt(payment.getCreatedAt())
                .userId(user != null ? user.getId() : null)
                .userEmail(user != null ? user.getEmail() : null)
                .username(user != null ? user.getUsername() : null)
                .subscriptionId(subscription != null ? subscription.getId() : null)
                .subscriptionStatus(subscription != null ? subscription.getStatus() : null)
                .subscriptionStartDate(subscription != null ? subscription.getStartDate() : null)
                .subscriptionEndDate(subscription != null ? subscription.getEndDate() : null)
                .pricingPlanId(pricingPlan != null ? pricingPlan.getId() : null)
                .pricingPlanName(pricingPlan != null ? pricingPlan.getName() : null)
                .credits(pricingPlan != null ? pricingPlan.getCredits() : null)
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
