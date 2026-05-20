package com.deepguard.auth.service;

import com.deepguard.auth.dto.request.EmailVerificationRequest;
import com.deepguard.auth.dto.response.EmailVerificationResponse;

public interface EmailVerificationService {
    EmailVerificationResponse verifyEmail(EmailVerificationRequest emailVerificationRequest);
}
