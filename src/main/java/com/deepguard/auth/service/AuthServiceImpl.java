package com.deepguard.auth.service;

import com.deepguard.auth.dto.request.LoginRequest;
import com.deepguard.auth.dto.request.LogoutRequest;
import com.deepguard.auth.dto.request.RefreshTokenRequest;
import com.deepguard.auth.dto.request.RegisterRequest;
import com.deepguard.auth.dto.response.AuthResponse;
import com.deepguard.auth.dto.response.RefreshTokenResponse;
import com.deepguard.auth.dto.response.UserAuthResponse;
import com.deepguard.auth.entity.RefreshToken;
import com.deepguard.auth.entity.Role;
import com.deepguard.auth.entity.User;
import com.deepguard.auth.enums.UserStatus;
import com.deepguard.auth.mapper.AuthMapper;
import com.deepguard.auth.repository.RoleRepository;
import com.deepguard.auth.repository.UserRepository;
import com.deepguard.common.exception.BusinessException;
import com.deepguard.common.exception.ErrorCode;
import com.deepguard.security.jwt.JwtService;
import com.deepguard.security.userdetails.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private static final String DEFAULT_USER_ROLE = "USER";

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;
    private final AuthMapper authMapper;

    @Override
    @Transactional
    public AuthResponse register(RegisterRequest request) {
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
                .isVerified(true)
                .status(UserStatus.ACTIVE.name())
                .createdAt(LocalDateTime.now())
                .updatedAt(null)
                .build();

        User savedUser = userRepository.save(user);
        String accessToken = jwtService.generateAccessToken(savedUser);
        RefreshToken refreshToken = refreshTokenService.createRefreshToken(savedUser);

        return authMapper.toAuthResponse(
                accessToken,
                refreshToken.getToken(),
                jwtService.getAccessTokenExpirationSeconds(),
                savedUser
        );
    }

    @Override
    @Transactional
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmailOrUsername(request.getIdentifier(), request.getIdentifier())
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_CREDENTIALS));

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new BusinessException(ErrorCode.INVALID_CREDENTIALS);
        }

        if (!UserStatus.ACTIVE.name().equalsIgnoreCase(user.getStatus())) {
            throw new BusinessException(ErrorCode.ACCOUNT_DISABLED);
        }

        String accessToken = jwtService.generateAccessToken(user);
        RefreshToken refreshToken = refreshTokenService.createRefreshToken(user);

        return authMapper.toAuthResponse(
                accessToken,
                refreshToken.getToken(),
                jwtService.getAccessTokenExpirationSeconds(),
                user
        );
    }

    @Override
    @Transactional(readOnly = true)
    public RefreshTokenResponse refresh(RefreshTokenRequest request) {
        RefreshToken refreshToken = refreshTokenService.verifyRefreshToken(request.getRefreshToken());
        User user = refreshToken.getUser();
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }

        String accessToken = jwtService.generateAccessToken(user);
        return authMapper.toRefreshTokenResponse(
                accessToken,
                refreshToken.getToken(),
                jwtService.getAccessTokenExpirationSeconds()
        );
    }

    @Override
    @Transactional
    public void logout(LogoutRequest request) {
        refreshTokenService.revokeRefreshToken(request.getRefreshToken());
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
}
