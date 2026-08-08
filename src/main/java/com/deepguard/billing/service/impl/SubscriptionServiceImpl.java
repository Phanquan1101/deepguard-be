package com.deepguard.billing.service.impl;

import com.deepguard.billing.dto.response.CurrentSubscriptionResponse;
import com.deepguard.billing.entity.Subscription;
import com.deepguard.billing.enums.SubscriptionStatus;
import com.deepguard.billing.repository.SubscriptionRepository;
import com.deepguard.billing.service.SubscriptionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class SubscriptionServiceImpl implements SubscriptionService {

    private static final String FREE_PLAN_ID = "FREE";
    private static final String FREE_PLAN_NAME = "Free Tier";
    private static final String FREE_STATUS = "FREE";

    private final SubscriptionRepository subscriptionRepository;

    @Override
    @Transactional(readOnly = true)
    public CurrentSubscriptionResponse getMyCurrentSubscription(String userId) {
        return subscriptionRepository
                .findFirstByUser_IdAndStatusAndEndDateAfterOrderByEndDateDesc(
                        userId,
                        SubscriptionStatus.ACTIVE,
                        LocalDateTime.now()
                )
                .map(this::toResponse)
                .orElseGet(this::freePlanResponse);
    }

    private CurrentSubscriptionResponse toResponse(Subscription subscription) {
        return CurrentSubscriptionResponse.builder()
                .subscriptionId(subscription.getId())
                .pricingPlanId(subscription.getPricingPlan().getId())
                .pricingPlanName(subscription.getPricingPlan().getName())
                .status(subscription.getStatus().name())
                .credits(subscription.getPricingPlan().getCredits())
                .startDate(subscription.getStartDate())
                .endDate(subscription.getEndDate())
                .build();
    }

    private CurrentSubscriptionResponse freePlanResponse() {
        return CurrentSubscriptionResponse.builder()
                .pricingPlanId(FREE_PLAN_ID)
                .pricingPlanName(FREE_PLAN_NAME)
                .status(FREE_STATUS)
                .build();
    }
}
