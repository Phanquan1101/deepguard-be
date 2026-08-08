package com.deepguard.billing.controller;

import com.deepguard.billing.dto.response.CurrentSubscriptionResponse;
import com.deepguard.billing.service.SubscriptionService;
import com.deepguard.common.exception.BusinessException;
import com.deepguard.common.exception.ErrorCode;
import com.deepguard.common.response.ApiResponse;
import com.deepguard.security.userdetails.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/billing/subscriptions")
@RequiredArgsConstructor
public class SubscriptionController {

    private final SubscriptionService subscriptionService;

    @GetMapping("/me/current")
    public ResponseEntity<ApiResponse<CurrentSubscriptionResponse>> getMyCurrentSubscription(
            Authentication authentication
    ) {
        CustomUserDetails userDetails = getCurrentUserDetails(authentication);
        CurrentSubscriptionResponse response = subscriptionService.getMyCurrentSubscription(userDetails.getId());
        return ResponseEntity.ok(ApiResponse.success("Current subscription fetched successfully", response));
    }

    private CustomUserDetails getCurrentUserDetails(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof CustomUserDetails userDetails)) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        return userDetails;
    }
}
