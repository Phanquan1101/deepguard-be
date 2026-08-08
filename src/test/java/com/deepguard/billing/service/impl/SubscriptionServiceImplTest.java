package com.deepguard.billing.service.impl;

import com.deepguard.billing.dto.response.CurrentSubscriptionResponse;
import com.deepguard.billing.entity.Payment;
import com.deepguard.billing.entity.PricingPlan;
import com.deepguard.billing.entity.Subscription;
import com.deepguard.billing.enums.PaymentStatus;
import com.deepguard.billing.enums.SubscriptionStatus;
import com.deepguard.billing.repository.PaymentRepository;
import com.deepguard.billing.repository.SubscriptionRepository;
import org.springframework.data.domain.Pageable;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SubscriptionServiceImplTest {

    private final SubscriptionRepository subscriptionRepository = mock(SubscriptionRepository.class);
    private final PaymentRepository paymentRepository = mock(PaymentRepository.class);
    private final SubscriptionServiceImpl subscriptionService = new SubscriptionServiceImpl(
            subscriptionRepository,
            paymentRepository
    );

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
        when(paymentRepository.findSuccessfulPaymentsWithUnexpiredSubscription(
                eq("user-id"), eq(PaymentStatus.SUCCESS), any(LocalDateTime.class), any(Pageable.class)))
                .thenReturn(java.util.List.of());

        CurrentSubscriptionResponse result = subscriptionService.getMyCurrentSubscription("user-id");

        assertThat(result.getPricingPlanId()).isEqualTo("FREE");
        assertThat(result.getPricingPlanName()).isEqualTo("Free Tier");
        assertThat(result.getStatus()).isEqualTo("FREE");
    }

    @Test
    void recoversACompletedPaymentWhoseSubscriptionWasLeftPending() {
        PricingPlan basicPlan = PricingPlan.builder()
                .id("BASIC")
                .name("Premium")
                .credits(500)
                .build();
        Subscription subscription = Subscription.builder()
                .id("subscription-id")
                .pricingPlan(basicPlan)
                .status(SubscriptionStatus.PENDING)
                .startDate(LocalDateTime.now().minusHours(1))
                .endDate(LocalDateTime.now().plusMonths(1))
                .build();
        Payment payment = Payment.builder()
                .subscription(subscription)
                .status(PaymentStatus.SUCCESS)
                .build();

        when(subscriptionRepository.findFirstByUser_IdAndStatusAndEndDateAfterOrderByEndDateDesc(
                eq("user-id"), eq(SubscriptionStatus.ACTIVE), any(LocalDateTime.class)))
                .thenReturn(Optional.empty());
        when(paymentRepository.findSuccessfulPaymentsWithUnexpiredSubscription(
                eq("user-id"), eq(PaymentStatus.SUCCESS), any(LocalDateTime.class), any(Pageable.class)))
                .thenReturn(java.util.List.of(payment));

        CurrentSubscriptionResponse result = subscriptionService.getMyCurrentSubscription("user-id");

        assertThat(result.getPricingPlanId()).isEqualTo("BASIC");
        assertThat(result.getStatus()).isEqualTo("ACTIVE");
        assertThat(subscription.getStatus()).isEqualTo(SubscriptionStatus.ACTIVE);
        verify(subscriptionRepository).save(subscription);
    }
}
