package com.deepguard.auth.bootstrap;

import com.deepguard.auth.entity.Role;
import com.deepguard.auth.entity.User;
import com.deepguard.auth.enums.UserStatus;
import com.deepguard.auth.repository.RoleRepository;
import com.deepguard.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class AdminAccountInitializer implements CommandLineRunner {

    private static final String ADMIN_ROLE = "ADMIN";
    private static final String DEFAULT_FALLBACK_PASSWORD = "Admin123@";

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.default-admin.email}")
    private String defaultAdminEmail;

    @Value("${app.default-admin.username}")
    private String defaultAdminUsername;

    @Value("${app.default-admin.password}")
    private String defaultAdminPassword;

    @Override
    @Transactional
    public void run(String... args) {
        if (userRepository.existsByEmail(defaultAdminEmail)) {
            log.info("Default admin already exists: {}", defaultAdminEmail);
            return;
        }

        Role adminRole = roleRepository.findByName(ADMIN_ROLE).orElse(null);
        if (adminRole == null) {
            log.warn("Cannot create default admin because role '{}' was not found", ADMIN_ROLE);
            return;
        }

        if (DEFAULT_FALLBACK_PASSWORD.equals(defaultAdminPassword)) {
            log.warn("Default admin password is using fallback value. Please change it in production.");
        }

        User admin = User.builder()
                .email(defaultAdminEmail)
                .username(defaultAdminUsername)
                .passwordHash(passwordEncoder.encode(defaultAdminPassword))
                .role(adminRole)
                .isVerified(true)
                .status(UserStatus.ACTIVE.name())
                .createdAt(LocalDateTime.now())
                .updatedAt(null)
                .build();

        userRepository.save(admin);
        log.info("Default admin created: {}", defaultAdminEmail);
    }
}
