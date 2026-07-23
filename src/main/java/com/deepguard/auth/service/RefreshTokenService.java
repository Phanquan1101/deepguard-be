package com.deepguard.auth.service;

import com.deepguard.auth.entity.RefreshToken;
import com.deepguard.auth.entity.User;

public interface RefreshTokenService {

    RefreshToken createRefreshToken(User user);

    RefreshToken verifyRefreshToken(String token);

    RefreshToken rotateRefreshToken(String token);

    void revokeRefreshToken(String token, String userId);

    void revokeAllRefreshTokens(String userId);

    boolean isExpired(RefreshToken refreshToken);
}
