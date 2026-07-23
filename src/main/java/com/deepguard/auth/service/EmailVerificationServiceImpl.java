package com.deepguard.auth.service;

import com.deepguard.auth.dto.request.EmailVerificationRequest;
import com.deepguard.auth.dto.response.EmailVerificationResponse;
import com.deepguard.auth.entity.EmailVerification;
import com.deepguard.auth.entity.User;
import com.deepguard.auth.repository.EmailVerificationRepository;
import com.deepguard.auth.repository.UserRepository;
import com.deepguard.billing.service.UserCreditService;
import com.deepguard.common.exception.BusinessException;
import com.deepguard.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Service
@RequiredArgsConstructor
public class EmailVerificationServiceImpl implements EmailVerificationService {

    private static final int MAX_VERIFICATION_FAILURES = 5;
    private static final long VERIFICATION_ATTEMPT_WINDOW_MINUTES = 15;
    private static final int MAX_TRACKED_VERIFICATION_ATTEMPTS = 10_000;

    private final UserRepository userRepository;
    private final EmailVerificationRepository emailVerificationRepository;

    private final UserCreditService userCreditService;
    private final ConcurrentMap<String, FailedAttempt> verificationFailures = new ConcurrentHashMap<>();

    @Override
    @Transactional
    public EmailVerificationResponse verifyEmail(EmailVerificationRequest emailVerificationRequest) {
        String email = emailVerificationRequest.getEmail().trim().toLowerCase(Locale.ROOT);
        ensureVerificationIsNotRateLimited(email);

        User user = userRepository.findByEmail(emailVerificationRequest.getEmail())
                .orElseThrow(() -> invalidVerificationAttempt(email));
        EmailVerification emailVerification = emailVerificationRepository.findByUserAndVerificationCode(user, emailVerificationRequest.getOtp())
                .orElseThrow(() -> invalidVerificationAttempt(email));
        if (emailVerification.getExpiredAt().isBefore(LocalDateTime.now())) {
            throw invalidVerificationAttempt(email);
        }
        user.setIsVerified(true);
        userRepository.save(user);
        emailVerificationRepository.delete(emailVerification);

        userCreditService.grantWelcomeBonus(user);
        verificationFailures.remove(email);

        return EmailVerificationResponse.builder()
                .email(user.getEmail())
                .build();
    }

    private void ensureVerificationIsNotRateLimited(String email) {
        FailedAttempt attempt = verificationFailures.get(email);
        if (attempt == null) {
            return;
        }

        LocalDateTime now = LocalDateTime.now();
        if (attempt.lockedUntil() != null && now.isBefore(attempt.lockedUntil())) {
            throw new BusinessException(ErrorCode.AUTHENTICATION_RATE_LIMITED);
        }
        if (!now.isBefore(attempt.firstFailureAt().plusMinutes(VERIFICATION_ATTEMPT_WINDOW_MINUTES))) {
            verificationFailures.remove(email, attempt);
        }
    }

    private BusinessException invalidVerificationAttempt(String email) {
        makeRoomForVerificationAttempt(email);
        if (!verificationFailures.containsKey(email)
                && verificationFailures.size() >= MAX_TRACKED_VERIFICATION_ATTEMPTS) {
            return new BusinessException(ErrorCode.INVALID_VERIFICATION_CODE);
        }

        verificationFailures.compute(email, (key, currentAttempt) -> {
            LocalDateTime now = LocalDateTime.now();
            if (currentAttempt == null
                    || !now.isBefore(currentAttempt.firstFailureAt().plusMinutes(VERIFICATION_ATTEMPT_WINDOW_MINUTES))) {
                return new FailedAttempt(1, now, null);
            }

            int failures = currentAttempt.failures() + 1;
            LocalDateTime lockedUntil = failures >= MAX_VERIFICATION_FAILURES
                    ? now.plusMinutes(VERIFICATION_ATTEMPT_WINDOW_MINUTES)
                    : null;
            return new FailedAttempt(failures, currentAttempt.firstFailureAt(), lockedUntil);
        });
        return new BusinessException(ErrorCode.INVALID_VERIFICATION_CODE);
    }

    private void makeRoomForVerificationAttempt(String email) {
        if (verificationFailures.containsKey(email)
                || verificationFailures.size() < MAX_TRACKED_VERIFICATION_ATTEMPTS) {
            return;
        }

        LocalDateTime now = LocalDateTime.now();
        verificationFailures.entrySet().removeIf(entry ->
                !now.isBefore(entry.getValue().firstFailureAt().plusMinutes(VERIFICATION_ATTEMPT_WINDOW_MINUTES)));
    }

    private record FailedAttempt(int failures, LocalDateTime firstFailureAt, LocalDateTime lockedUntil) {
    }
}
