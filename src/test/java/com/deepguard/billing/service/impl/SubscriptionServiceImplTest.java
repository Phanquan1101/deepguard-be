package com.deepguard.billing.service.impl;

import com.deepguard.billing.dto.response.CurrentSubscriptionResponse;
import com.deepguard.billing.entity.PricingPlan;
import com.deepguard.billing.entity.Subscription;
import com.deepguard.billing.enums.SubscriptionStatus;
import com.deepguard.billing.repository.SubscriptionRepository;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SubscriptionServiceImplTest {

    private final SubscriptionRepository subscriptionRepository = mock(SubscriptionRepository.class);
    private final SubscriptionServiceImpl subscriptionService = new SubscriptionServiceImpl(subscriptionRepository);

    @Test
    void returnsTheActiveBasicPlanForTheCurrentUser() {
        PricingPlan basicPlan = PricingPlan.builder()
                .id("BASIC")
                .name("Premium")
                .credits(500)
                .build();
        Subscription subscription = Subscription.builder()
                .id("subscription-id")
                .pricingPlan(basicPlan)
                .status(SubscriptionStatus.ACTIVE)
                .startDate(LocalDateTime.now().minusDays(1))
                .endDate(LocalDateTime.now().plusMonths(1))
                .build();

        when(subscriptionRepository.findFirstByUser_IdAndStatusAndEndDateAfterOrderByEndDateDesc(
                eq("user-id"), eq(SubscriptionStatus.ACTIVE), any(LocalDateTime.class)))
                .thenReturn(Optional.of(subscription));

        CurrentSubscriptionResponse result = subscriptionService.getMyCurrentSubscription("user-id");

        assertThat(result.getPricingPlanId()).isEqualTo("BASIC");
        assertThat(result.getPricingPlanName()).isEqualTo("Premium");
        assertThat(result.getStatus()).isEqualTo("ACTIVE");
        assertThat(result.getCredits()).isEqualTo(500);
    }

    @Test
    void returnsFreeWhenNoActiveSubscriptionExists() {
        when(subscriptionRepository.findFirstByUser_IdAndStatusAndEndDateAfterOrderByEndDateDesc(
                eq("user-id"), eq(SubscriptionStatus.ACTIVE), any(LocalDateTime.class)))
                .thenReturn(Optional.empty());

        CurrentSubscriptionResponse result = subscriptionService.getMyCurrentSubscription("user-id");

        assertThat(result.getPricingPlanId()).isEqualTo("FREE");
        assertThat(result.getPricingPlanName()).isEqualTo("Free Tier");
        assertThat(result.getStatus()).isEqualTo("FREE");
    }
}
