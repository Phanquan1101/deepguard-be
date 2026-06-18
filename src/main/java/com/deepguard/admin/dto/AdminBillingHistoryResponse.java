package com.deepguard.admin.dto;

import com.deepguard.billing.enums.PaymentStatus;
import com.deepguard.billing.enums.SubscriptionStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminBillingHistoryResponse {

    private String paymentId;
    private String transactionCode;
    private BigDecimal amount;
    private String paymentMethod;
    private PaymentStatus status;
    private LocalDateTime createdAt;

    private String userId;
    private String userEmail;
    private String username;

    private String subscriptionId;
    private SubscriptionStatus subscriptionStatus;
    private LocalDateTime subscriptionStartDate;
    private LocalDateTime subscriptionEndDate;
    private String pricingPlanId;
    private String pricingPlanName;
    private Integer credits;
}
