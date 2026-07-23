package com.deepguard.auth.controller;

import com.deepguard.auth.dto.request.EmailVerificationRequest;
import com.deepguard.auth.dto.response.EmailVerificationResponse;
import com.deepguard.auth.service.EmailVerificationService;
import com.deepguard.common.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/verify")
@RequiredArgsConstructor
public class VerifyController {

    private final EmailVerificationService emailVerificationService;

    @PostMapping("/email")
    public ResponseEntity<ApiResponse<EmailVerificationResponse>> verifyEmail(@Valid @RequestBody EmailVerificationRequest request) {
        EmailVerificationResponse emailVerificationResponse = emailVerificationService.verifyEmail(request);
        return ResponseEntity.ok(ApiResponse.<EmailVerificationResponse>builder()
                .code("200")
                .message("Email verified successfully")
                .data(emailVerificationResponse)
                .build());
    }
}
