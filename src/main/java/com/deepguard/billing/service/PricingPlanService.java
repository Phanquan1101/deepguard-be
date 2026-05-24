package com.deepguard.billing.service;

import com.deepguard.billing.dto.response.PricingPlanResponse;

import java.util.List;

public interface PricingPlanService {

    List<PricingPlanResponse> getAllPlans();
}
