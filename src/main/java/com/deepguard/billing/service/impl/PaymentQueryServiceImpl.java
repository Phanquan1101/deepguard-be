package com.deepguard.billing.service.impl;

import com.deepguard.billing.dto.response.PaymentDetailResponse;
import com.deepguard.billing.entity.Payment;
import com.deepguard.billing.entity.PricingPlan;
import com.deepguard.billing.entity.Subscription;
import com.deepguard.billing.repository.PaymentRepository;
import com.deepguard.billing.service.PaymentQueryService;
import com.deepguard.common.exception.BusinessException;
import com.deepguard.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PaymentQueryServiceImpl implements PaymentQueryService {

    private final PaymentRepository paymentRepository;

    @Override
    @Transactional(readOnly = true)
    public PaymentDetailResponse getPaymentDetail(String paymentId, String currentUserId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Payment not found"));

        String ownerId = payment.getUser() == null ? null : payment.getUser().getId();
        if (ownerId == null || !ownerId.equals(currentUserId)) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED);
        }

        Subscription subscription = payment.getSubscription();
        PricingPlan pricingPlan = subscription == null ? null : subscription.getPricingPlan();

        return PaymentDetailResponse.builder()
                .paymentId(payment.getId())
                .transactionCode(payment.getTransactionCode())
                .amount(payment.getAmount())
                .paymentMethod(payment.getPaymentMethod())
                .status(payment.getStatus())
                .createdAt(payment.getCreatedAt())
                .subscriptionId(subscription == null ? null : subscription.getId())
                .subscriptionStatus(subscription == null ? null : subscription.getStatus())
                .pricingPlanId(pricingPlan == null ? null : pricingPlan.getId())
                .pricingPlanName(pricingPlan == null ? null : pricingPlan.getName())
                .credits(pricingPlan == null ? null : pricingPlan.getCredits())
                .subscriptionStartDate(subscription == null ? null : subscription.getStartDate())
                .subscriptionEndDate(subscription == null ? null : subscription.getEndDate())
                .build();
    }
}
