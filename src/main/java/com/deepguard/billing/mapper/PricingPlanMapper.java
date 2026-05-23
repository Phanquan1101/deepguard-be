package com.deepguard.billing.mapper;

import com.deepguard.billing.dto.response.PricingPlanResponse;
import com.deepguard.billing.entity.PricingPlan;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

@Component
public class PricingPlanMapper {

    public PricingPlanResponse toResponse(PricingPlan pricingPlan) {
        if (pricingPlan == null) {
            return null;
        }

        return PricingPlanResponse.builder()
                .id(pricingPlan.getId())
                .name(pricingPlan.getName())
                .price(pricingPlan.getPrice())
                .credits(pricingPlan.getCredits())
                .description(pricingPlan.getDescription())
                .build();
    }

    public List<PricingPlanResponse> toResponseList(List<PricingPlan> pricingPlans) {
        if (pricingPlans == null) {
            return Collections.emptyList();
        }

        return pricingPlans.stream()
                .map(this::toResponse)
                .toList();
    }
}
