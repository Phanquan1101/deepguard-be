package com.deepguard.auth.service;

import com.deepguard.auth.entity.RefreshToken;
import com.deepguard.auth.entity.User;

public interface RefreshTokenService {

    RefreshToken createRefreshToken(User user);

    RefreshToken verifyRefreshToken(String token);

    void revokeRefreshToken(String token);

    boolean isExpired(RefreshToken refreshToken);
}
