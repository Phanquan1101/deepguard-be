package com.deepguard.billing.controller;

import com.deepguard.billing.dto.response.PricingPlanResponse;
import com.deepguard.billing.service.PricingPlanService;
import com.deepguard.common.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/billing/pricing-plans")
@RequiredArgsConstructor
public class PricingPlanController {

    private final PricingPlanService pricingPlanService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<PricingPlanResponse>>> getAllPlans() {
        List<PricingPlanResponse> response = pricingPlanService.getAllPlans();
        return ResponseEntity.ok(ApiResponse.success("Pricing plans fetched successfully", response));
    }
}
