package com.deepguard.auth.service;

import com.deepguard.auth.dto.request.EmailVerificationRequest;
import com.deepguard.auth.dto.response.EmailVerificationResponse;
import com.deepguard.auth.entity.EmailVerification;
import com.deepguard.auth.entity.User;
import com.deepguard.auth.repository.EmailVerificationRepository;
import com.deepguard.auth.repository.UserRepository;
import com.deepguard.common.exception.BusinessException;
import com.deepguard.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class EmailVerificationServiceImpl implements EmailVerificationService {

    private final UserRepository userRepository;
    private final EmailVerificationRepository emailVerificationRepository;

    @Override
    public EmailVerificationResponse verifyEmail(EmailVerificationRequest emailVerificationRequest) {
        User user = userRepository.findByEmail(emailVerificationRequest.getEmail())
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        EmailVerification emailVerification = emailVerificationRepository.findByUserAndVerificationCode(user, emailVerificationRequest.getOtp())
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_VERIFICATION_CODE));
        if (emailVerification.getExpiredAt().isBefore(LocalDateTime.now())) {
            throw new BusinessException(ErrorCode.INVALID_VERIFICATION_CODE);
        }
        if (!emailVerification.getVerificationCode().equals(emailVerificationRequest.getOtp())) {
            throw new BusinessException(ErrorCode.INVALID_VERIFICATION_CODE);
        }
        user.setIsVerified(true);
        userRepository.save(user);
        emailVerificationRepository.delete(emailVerification);
        return EmailVerificationResponse.builder()
                .email(user.getEmail())
                .build();
    }
}
