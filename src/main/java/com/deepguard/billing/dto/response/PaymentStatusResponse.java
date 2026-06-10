package com.deepguard.billing.dto.response;

import com.deepguard.billing.enums.PaymentStatus;
import com.deepguard.billing.enums.SubscriptionStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentStatusResponse {

    private String paymentId;
    private String transactionCode;
    private PaymentStatus status;
    private BigDecimal amount;
    private String paymentMethod;
    private LocalDateTime createdAt;
    private String subscriptionId;
    private SubscriptionStatus subscriptionStatus;
    private String pricingPlanId;
    private String pricingPlanName;
    private Integer credits;
}
