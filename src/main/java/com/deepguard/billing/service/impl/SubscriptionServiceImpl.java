package com.deepguard.billing.service.impl;

import com.deepguard.billing.dto.response.CurrentSubscriptionResponse;
import com.deepguard.billing.entity.Payment;
import com.deepguard.billing.entity.Subscription;
import com.deepguard.billing.enums.PaymentStatus;
import com.deepguard.billing.enums.SubscriptionStatus;
import com.deepguard.billing.repository.PaymentRepository;
import com.deepguard.billing.repository.SubscriptionRepository;
import com.deepguard.billing.service.SubscriptionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import org.springframework.data.domain.PageRequest;

@Service
@RequiredArgsConstructor
public class SubscriptionServiceImpl implements SubscriptionService {

    private static final String FREE_PLAN_ID = "FREE";
    private static final String FREE_PLAN_NAME = "Free Tier";
    private static final String FREE_STATUS = "FREE";

    private final SubscriptionRepository subscriptionRepository;
    private final PaymentRepository paymentRepository;

    @Override
    @Transactional
    public CurrentSubscriptionResponse getMyCurrentSubscription(String userId) {
        LocalDateTime now = LocalDateTime.now();
        return subscriptionRepository
                .findFirstByUser_IdAndStatusAndEndDateAfterOrderByEndDateDesc(
                        userId,
                        SubscriptionStatus.ACTIVE,
                        now
                )
                .map(this::toResponse)
                .orElseGet(() -> recoverCompletedPaymentSubscription(userId, now)
                        .map(this::toResponse)
                        .orElseGet(this::freePlanResponse));
    }

    /**
     * A payment marked SUCCESS is the source of truth for entitlement. If an
     * older webhook stopped after saving the payment but before activating its
     * subscription, repair that subscription on the next authenticated read.
     * Pending or failed payments are deliberately ignored.
     */
    private java.util.Optional<Subscription> recoverCompletedPaymentSubscription(
            String userId,
            LocalDateTime now
    ) {
        return paymentRepository
                .findSuccessfulPaymentsWithUnexpiredSubscription(
                        userId,
                        PaymentStatus.SUCCESS,
                        now,
                        PageRequest.of(0, 1)
                )
                .stream()
                .findFirst()
                .map(Payment::getSubscription)
                .map(subscription -> {
                    if (subscription.getStatus() != SubscriptionStatus.ACTIVE) {
                        subscription.setStatus(SubscriptionStatus.ACTIVE);
                        subscriptionRepository.save(subscription);
                    }
                    return subscription;
                });
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
