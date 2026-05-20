package com.deepguard.auth.repository;

import com.deepguard.auth.entity.EmailVerification;
import com.deepguard.auth.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface EmailVerificationRepository extends JpaRepository<EmailVerification, String> {

    Optional<EmailVerification> findByUserAndVerificationCode(User user, String verificationCode);
}
