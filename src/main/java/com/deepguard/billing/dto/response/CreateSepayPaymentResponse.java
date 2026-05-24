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
public class CreateSepayPaymentResponse {

    private String paymentId;
    private String transactionCode;
    private BigDecimal amount;
    private PaymentStatus status;
    private String planId;
    private String planName;
    private Integer credits;
    private String bankCode;
    private String bankAccountNo;
    private String bankAccountName;
    private String transferContent;
    private String qrUrl;
}
