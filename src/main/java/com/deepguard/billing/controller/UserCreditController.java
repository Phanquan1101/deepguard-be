package com.deepguard.billing.controller;

import com.deepguard.billing.dto.response.UserCreditResponse;
import com.deepguard.billing.service.UserCreditService;
import com.deepguard.common.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/credits")
@RequiredArgsConstructor
public class UserCreditController {

    private final UserCreditService userCreditService;

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserCreditResponse>> getUserCredit() {
        UserCreditResponse creditResponse = userCreditService.getMyCredit();
        return ResponseEntity.ok(ApiResponse.success("User credit retrieved successfully", creditResponse));
    }

}
