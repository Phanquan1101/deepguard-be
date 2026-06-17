package com.deepguard.auth.bootstrap;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    @Value("${resend.api-key}")
    private String apiKey;

    @Value("${resend.from-email}")
    private String fromEmail;

    private final RestClient restClient = RestClient.create();

    @Async
    public void sendOtp(String email, String otp) {
        try {

            Map<String, Object> body = Map.of(
                    "from", fromEmail,
                    "to", List.of(email),
                    "subject", "DeepGuard Email Verification",
                    "html",
                    """
                    <h2>DeepGuard Verification</h2>
                    <p>Your OTP code:</p>
                    <h1>%s</h1>
                    <p>This code expires in 10 minutes.</p>
                    """.formatted(otp)
            );

            restClient.post()
                    .uri("https://api.resend.com/emails")
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .body(body)
                    .retrieve()
                    .toBodilessEntity();

        } catch (Exception e) {
            log.error("Failed to send OTP email to {}", email, e);
        }
    }

}
