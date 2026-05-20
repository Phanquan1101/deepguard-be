package com.deepguard.auth.service;

import com.deepguard.auth.dto.request.LoginRequest;
import com.deepguard.auth.dto.request.LogoutRequest;
import com.deepguard.auth.dto.request.RefreshTokenRequest;
import com.deepguard.auth.dto.request.RegisterRequest;
import com.deepguard.auth.dto.response.AuthResponse;
import com.deepguard.auth.dto.response.RefreshTokenResponse;
import com.deepguard.auth.dto.response.RegisterResponse;
import com.deepguard.auth.dto.response.UserAuthResponse;
import jakarta.mail.MessagingException;

public interface AuthService {

    RegisterResponse register(RegisterRequest request) throws MessagingException;

    AuthResponse login(LoginRequest request);

    RefreshTokenResponse refresh(RefreshTokenRequest request);

    void logout(LogoutRequest request);

    UserAuthResponse getCurrentUser();
}
