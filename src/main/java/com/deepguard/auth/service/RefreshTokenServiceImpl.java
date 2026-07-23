package com.deepguard.auth.service;

import com.deepguard.auth.entity.RefreshToken;
import com.deepguard.auth.entity.User;
import com.deepguard.auth.repository.RefreshTokenRepository;
import com.deepguard.common.exception.BusinessException;
import com.deepguard.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HexFormat;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RefreshTokenServiceImpl implements RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;

    @Value("${app.security.jwt.refresh-token-expiration-ms}")
    private long refreshTokenExpirationMs;

    @Override
    @Transactional
    public RefreshToken createRefreshToken(User user) {
        String rawToken = UUID.randomUUID().toString();
        RefreshToken refreshToken = RefreshToken.builder()
                .user(user)
                .token(hashToken(rawToken))
                .rawToken(rawToken)
                .expiredAt(LocalDateTime.now().plus(refreshTokenExpirationMs, ChronoUnit.MILLIS))
                .revoked(false)
                .build();
        return refreshTokenRepository.save(refreshToken);
    }

    @Override
    @Transactional(readOnly = true)
    public RefreshToken verifyRefreshToken(String token) {
        RefreshToken refreshToken = findByRawOrHashedToken(token, false);
        validateRefreshToken(refreshToken);
        return refreshToken;
    }

    @Override
    @Transactional
    public RefreshToken rotateRefreshToken(String token) {
        RefreshToken refreshToken = findByRawOrHashedToken(token, true);
        validateRefreshToken(refreshToken);

        String rawToken = UUID.randomUUID().toString();
        refreshToken.setToken(hashToken(rawToken));
        refreshToken.setRawToken(rawToken);
        return refreshTokenRepository.save(refreshToken);
    }

    @Override
    @Transactional
    public void revokeRefreshToken(String token, String userId) {
        RefreshToken refreshToken = findByRawOrHashedToken(token, true);
        if (!refreshToken.getUser().getId().equals(userId)) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED);
        }
        refreshToken.setRevoked(true);
        refreshTokenRepository.save(refreshToken);
    }

    @Override
    @Transactional
    public void revokeAllRefreshTokens(String userId) {
        refreshTokenRepository.findAllByUser_Id(userId).forEach(refreshToken -> refreshToken.setRevoked(true));
    }

    @Override
    public boolean isExpired(RefreshToken refreshToken) {
        return refreshToken.getExpiredAt().isBefore(LocalDateTime.now());
    }

    private RefreshToken findByRawOrHashedToken(String rawToken, boolean lockForUpdate) {
        String hashedToken = hashToken(rawToken);
        Optional<RefreshToken> refreshToken = lockForUpdate
                ? refreshTokenRepository.findByTokenForUpdate(hashedToken)
                : refreshTokenRepository.findByToken(hashedToken);

        // Allows tokens issued before hashing was introduced to be rotated once.
        return refreshToken.or(() -> lockForUpdate
                        ? refreshTokenRepository.findByTokenForUpdate(rawToken)
                        : refreshTokenRepository.findByToken(rawToken))
                .orElseThrow(() -> new BusinessException(ErrorCode.REFRESH_TOKEN_NOT_FOUND));
    }

    private void validateRefreshToken(RefreshToken refreshToken) {
        if (Boolean.TRUE.equals(refreshToken.getRevoked())) {
            throw new BusinessException(ErrorCode.REFRESH_TOKEN_REVOKED);
        }
        if (isExpired(refreshToken)) {
            throw new BusinessException(ErrorCode.REFRESH_TOKEN_EXPIRED);
        }
    }

    private String hashToken(String token) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 is unavailable", ex);
        }
    }
}
