package com.deepguard.billing.dto.response;

import com.deepguard.billing.enums.PaymentStatus;
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
public class CreateVnpayPaymentResponse {

    private String paymentId;
    private String transactionCode;
    private String paymentUrl;
    private PaymentStatus status;
    private BigDecimal amount;
    private String planId;
    private String planName;
    private Integer credits;
}
