package com.deepguard.auth.mapper;

import com.deepguard.auth.dto.response.AuthResponse;
import com.deepguard.auth.dto.response.RefreshTokenResponse;
import com.deepguard.auth.dto.response.UserAuthResponse;
import com.deepguard.auth.entity.User;
import org.springframework.stereotype.Component;

@Component
public class AuthMapper {

    public UserAuthResponse toUserAuthResponse(User user) {
        if (user == null) {
            return null;
        }

        return UserAuthResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .username(user.getUsername())
                .role(user.getRole() != null ? user.getRole().getName() : null)
                .status(user.getStatus())
                .verified(Boolean.TRUE.equals(user.getIsVerified()))
                .build();
    }

    public AuthResponse toAuthResponse(String accessToken, String refreshToken, long expiresIn, User user) {
        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(expiresIn)
                .user(toUserAuthResponse(user))
                .build();
    }

    public RefreshTokenResponse toRefreshTokenResponse(String accessToken, String refreshToken, long expiresIn) {
        return RefreshTokenResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(expiresIn)
                .build();
    }
}
