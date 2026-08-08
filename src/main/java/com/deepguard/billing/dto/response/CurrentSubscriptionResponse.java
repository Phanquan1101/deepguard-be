package com.deepguard.billing.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CurrentSubscriptionResponse {

    private String subscriptionId;
    private String pricingPlanId;
    private String pricingPlanName;
    private String status;
    private Integer credits;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
}
