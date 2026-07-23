package com.deepguard.auth.service;

import com.deepguard.auth.bootstrap.EmailService;
import com.deepguard.auth.dto.request.LoginRequest;
import com.deepguard.auth.dto.request.LogoutRequest;
import com.deepguard.auth.dto.request.RefreshTokenRequest;
import com.deepguard.auth.dto.request.RegisterRequest;
import com.deepguard.auth.dto.response.AuthResponse;
import com.deepguard.auth.dto.response.RefreshTokenResponse;
import com.deepguard.auth.dto.response.RegisterResponse;
import com.deepguard.auth.dto.response.UserAuthResponse;
import com.deepguard.auth.entity.EmailVerification;
import com.deepguard.auth.entity.RefreshToken;
import com.deepguard.auth.entity.Role;
import com.deepguard.auth.entity.User;
import com.deepguard.auth.enums.UserStatus;
import com.deepguard.auth.mapper.AuthMapper;
import com.deepguard.auth.repository.EmailVerificationRepository;
import com.deepguard.auth.repository.RoleRepository;
import com.deepguard.auth.repository.UserRepository;
import com.deepguard.common.exception.BusinessException;
import com.deepguard.common.exception.ErrorCode;
import com.deepguard.security.jwt.JwtService;
import com.deepguard.security.userdetails.CustomUserDetails;
import jakarta.mail.MessagingException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.security.SecureRandom;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private static final String DEFAULT_USER_ROLE = "USER";
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final int MAX_LOGIN_FAILURES = 5;
    private static final long LOGIN_ATTEMPT_WINDOW_MINUTES = 15;
    private static final int MAX_TRACKED_LOGIN_ATTEMPTS = 10_000;

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;
    private final AuthMapper authMapper;
    private final EmailService emailService;
    private final EmailVerificationRepository emailVerificationRepository;
    private final ConcurrentMap<String, FailedAttempt> loginFailures = new ConcurrentHashMap<>();

    @Override
    @Transactional
    public RegisterResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BusinessException(ErrorCode.EMAIL_ALREADY_EXISTS);
        }

        if (userRepository.existsByUsername(request.getUsername())) {
            throw new BusinessException(ErrorCode.USERNAME_ALREADY_EXISTS);
        }

        Role role = roleRepository.findByName(DEFAULT_USER_ROLE)
                .orElseThrow(() -> new BusinessException(ErrorCode.ROLE_NOT_FOUND));

        User user = User.builder()
                .email(request.getEmail())
                .username(request.getUsername())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .role(role)
                .isVerified(false)
                .status(UserStatus.ACTIVE.name())
                .createdAt(LocalDateTime.now())
                .updatedAt(null)
                .build();

        User savedUser = userRepository.save(user);

        // Generate otp and send email verification
        String otp = generateOtp();
        EmailVerification emailVerification = EmailVerification.builder()
                .user(savedUser)
                .verificationCode(otp)
                .expiredAt(LocalDateTime.now().plusMinutes(10)) // OTP valid for 10 minutes
                .build();
        emailVerificationRepository.save(emailVerification);
        emailService.sendOtp(savedUser.getEmail(), otp);

        return RegisterResponse.builder()
                .email(savedUser.getEmail())
                .username(savedUser.getUsername())
                .build();
    }

    @Override
    @Transactional
    public AuthResponse login(LoginRequest request) {
        String loginKey = request.getIdentifier().trim().toLowerCase(Locale.ROOT);
        ensureLoginIsNotRateLimited(loginKey);

        User user = userRepository.findByEmailOrUsername(request.getIdentifier(), request.getIdentifier())
                .orElse(null);

        if (user == null || !passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            recordLoginFailure(loginKey);
            throw new BusinessException(ErrorCode.INVALID_CREDENTIALS);
        }

        loginFailures.remove(loginKey);
        assertUserCanAuthenticate(user);

        String accessToken = jwtService.generateAccessToken(user);
        RefreshToken refreshToken = refreshTokenService.createRefreshToken(user);

        return authMapper.toAuthResponse(
                accessToken,
                refreshToken.getRawToken(),
                jwtService.getAccessTokenExpirationSeconds()
        );
    }

    @Override
    @Transactional
    public RefreshTokenResponse refresh(RefreshTokenRequest request) {
        RefreshToken refreshToken = refreshTokenService.rotateRefreshToken(request.getRefreshToken());
        User user = refreshToken.getUser();
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }

        try {
            assertUserCanAuthenticate(user);
        } catch (BusinessException ex) {
            refreshTokenService.revokeAllRefreshTokens(user.getId());
            throw ex;
        }

        String accessToken = jwtService.generateAccessToken(user);
        return authMapper.toRefreshTokenResponse(
                accessToken,
                refreshToken.getRawToken(),
                jwtService.getAccessTokenExpirationSeconds()
        );
    }

    @Override
    @Transactional
    public void logout(LogoutRequest request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof CustomUserDetails principal)) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        refreshTokenService.revokeRefreshToken(request.getRefreshToken(), principal.getId());
    }

    @Override
    @Transactional(readOnly = true)
    public UserAuthResponse getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof CustomUserDetails principal)) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        return authMapper.toUserAuthResponse(principal.getUser());
    }

    private String generateOtp() {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        StringBuilder code = new StringBuilder();
        for (int i = 0; i < 6; i++) {
            code.append(chars.charAt(SECURE_RANDOM.nextInt(chars.length())));
        }
        return code.toString();
    }

    private void assertUserCanAuthenticate(User user) {
        if (!UserStatus.ACTIVE.name().equalsIgnoreCase(user.getStatus())) {
            throw new BusinessException(ErrorCode.ACCOUNT_DISABLED);
        }
        if (!Boolean.TRUE.equals(user.getIsVerified())) {
            throw new BusinessException(ErrorCode.ACCOUNT_NOT_VERIFIED);
        }
    }

    private void ensureLoginIsNotRateLimited(String loginKey) {
        FailedAttempt attempt = loginFailures.get(loginKey);
        if (attempt == null) {
            return;
        }

        LocalDateTime now = LocalDateTime.now();
        if (attempt.lockedUntil() != null && now.isBefore(attempt.lockedUntil())) {
            throw new BusinessException(ErrorCode.AUTHENTICATION_RATE_LIMITED);
        }
        if (!now.isBefore(attempt.firstFailureAt().plusMinutes(LOGIN_ATTEMPT_WINDOW_MINUTES))) {
            loginFailures.remove(loginKey, attempt);
        }
    }

    private void recordLoginFailure(String loginKey) {
        makeRoomForLoginAttempt(loginKey);
        if (!loginFailures.containsKey(loginKey) && loginFailures.size() >= MAX_TRACKED_LOGIN_ATTEMPTS) {
            return;
        }

        loginFailures.compute(loginKey, (key, currentAttempt) -> {
            LocalDateTime now = LocalDateTime.now();
            if (currentAttempt == null
                    || !now.isBefore(currentAttempt.firstFailureAt().plusMinutes(LOGIN_ATTEMPT_WINDOW_MINUTES))) {
                return new FailedAttempt(1, now, null);
            }

            int failures = currentAttempt.failures() + 1;
            LocalDateTime lockedUntil = failures >= MAX_LOGIN_FAILURES
                    ? now.plusMinutes(LOGIN_ATTEMPT_WINDOW_MINUTES)
                    : null;
            return new FailedAttempt(failures, currentAttempt.firstFailureAt(), lockedUntil);
        });
    }

    private void makeRoomForLoginAttempt(String loginKey) {
        if (loginFailures.containsKey(loginKey) || loginFailures.size() < MAX_TRACKED_LOGIN_ATTEMPTS) {
            return;
        }

        LocalDateTime now = LocalDateTime.now();
        loginFailures.entrySet().removeIf(entry ->
                !now.isBefore(entry.getValue().firstFailureAt().plusMinutes(LOGIN_ATTEMPT_WINDOW_MINUTES)));
    }

    private record FailedAttempt(int failures, LocalDateTime firstFailureAt, LocalDateTime lockedUntil) {
    }
}
