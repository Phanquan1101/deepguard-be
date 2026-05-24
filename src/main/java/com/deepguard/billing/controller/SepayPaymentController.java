package com.deepguard.billing.controller;

import com.deepguard.billing.dto.request.CreateSepayPaymentRequest;
import com.deepguard.billing.dto.response.CreateSepayPaymentResponse;
import com.deepguard.billing.service.SepayPaymentService;
import com.deepguard.common.exception.BusinessException;
import com.deepguard.common.exception.ErrorCode;
import com.deepguard.common.response.ApiResponse;
import com.deepguard.security.userdetails.CustomUserDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/billing/payments/sepay")
@RequiredArgsConstructor
public class SepayPaymentController {

    private final SepayPaymentService sepayPaymentService;

    @PostMapping("/create")
    public ResponseEntity<ApiResponse<CreateSepayPaymentResponse>> createSepayPayment(
            @Valid @RequestBody CreateSepayPaymentRequest request,
            Authentication authentication
    ) {
        CustomUserDetails userDetails = getCurrentUserDetails(authentication);
        CreateSepayPaymentResponse response = sepayPaymentService.createPayment(request, userDetails.getId());
        return ResponseEntity.ok(ApiResponse.success("SePay payment QR created successfully", response));
    }

    private CustomUserDetails getCurrentUserDetails(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof CustomUserDetails userDetails)) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        return userDetails;
    }
}
