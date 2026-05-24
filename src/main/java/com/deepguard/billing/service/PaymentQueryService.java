package com.deepguard.billing.service;

import com.deepguard.billing.dto.response.PaymentDetailResponse;

public interface PaymentQueryService {

    PaymentDetailResponse getPaymentDetail(String paymentId, String currentUserId);
}
