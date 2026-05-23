package com.deepguard.billing.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PricingPlanResponse {

    private String id;
    private String name;
    private BigDecimal price;
    private Integer credits;
    private String description;
}
