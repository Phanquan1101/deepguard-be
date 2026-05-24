package com.deepguard.billing.controller;

import com.deepguard.billing.dto.request.CreateVnpayPaymentRequest;
import com.deepguard.billing.dto.response.CreateVnpayPaymentResponse;
import com.deepguard.billing.dto.response.PaymentDetailResponse;
import com.deepguard.billing.dto.response.PaymentHistoryResponse;
import com.deepguard.billing.dto.response.VnpayIpnResponse;
import com.deepguard.billing.dto.response.VnpayReturnResponse;
import com.deepguard.billing.service.PaymentQueryService;
import com.deepguard.billing.service.VnpayPaymentService;
import com.deepguard.common.exception.BusinessException;
import com.deepguard.common.exception.ErrorCode;
import com.deepguard.common.response.ApiResponse;
import com.deepguard.security.userdetails.CustomUserDetails;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/billing/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final VnpayPaymentService vnpayPaymentService;
    private final PaymentQueryService paymentQueryService;

    @PostMapping("/vnpay/create")
    public ResponseEntity<ApiResponse<CreateVnpayPaymentResponse>> createVnpayPayment(
            @Valid @RequestBody CreateVnpayPaymentRequest request,
            Authentication authentication,
            HttpServletRequest servletRequest
    ) {
        CustomUserDetails userDetails = getCurrentUserDetails(authentication);

        CreateVnpayPaymentResponse response = vnpayPaymentService.createPayment(
                request,
                userDetails.getId(),
                resolveClientIp(servletRequest)
        );
        return ResponseEntity.ok(ApiResponse.success("VNPAY payment URL created successfully", response));
    }

    @GetMapping("/vnpay/return")
    public ResponseEntity<ApiResponse<VnpayReturnResponse>> handleVnpayReturn(@RequestParam Map<String, String> params) {
        VnpayReturnResponse response = vnpayPaymentService.handleReturn(params);
        return ResponseEntity.ok(ApiResponse.success("VNPAY return handled successfully", response));
    }

    @GetMapping("/vnpay/ipn")
    public ResponseEntity<VnpayIpnResponse> handleVnpayIpn(@RequestParam Map<String, String> params) {
        return ResponseEntity.ok(vnpayPaymentService.handleIpn(params));
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<List<PaymentHistoryResponse>>> getMyPayments(Authentication authentication) {
        CustomUserDetails userDetails = getCurrentUserDetails(authentication);
        List<PaymentHistoryResponse> response = vnpayPaymentService.getMyPayments(userDetails.getId());
        return ResponseEntity.ok(ApiResponse.success("Payment history fetched successfully", response));
    }

    @GetMapping("/{paymentId}")
    public ResponseEntity<ApiResponse<PaymentDetailResponse>> getPaymentDetail(
            @PathVariable String paymentId,
            Authentication authentication
    ) {
        CustomUserDetails userDetails = getCurrentUserDetails(authentication);
        PaymentDetailResponse response = paymentQueryService.getPaymentDetail(paymentId, userDetails.getId());
        return ResponseEntity.ok(ApiResponse.success("Payment detail fetched successfully", response));
    }

    private CustomUserDetails getCurrentUserDetails(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof CustomUserDetails userDetails)) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        return userDetails;
    }

    private String resolveClientIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
