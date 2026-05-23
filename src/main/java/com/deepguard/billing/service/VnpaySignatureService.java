package com.deepguard.billing.service;

import com.deepguard.billing.entity.Payment;
import com.deepguard.billing.entity.PricingPlan;

import java.util.Map;

public interface VnpaySignatureService {

    String buildPaymentUrl(Payment payment, PricingPlan pricingPlan, String clientIp);

    boolean verifySignature(Map<String, String> params);
}
