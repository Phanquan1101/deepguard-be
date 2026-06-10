package com.deepguard.billing.service;

import com.deepguard.billing.dto.response.PaymentDetailResponse;
import com.deepguard.billing.dto.response.PaymentStatusResponse;

public interface PaymentQueryService {

    PaymentDetailResponse getPaymentDetail(String paymentId, String currentUserId);

    PaymentStatusResponse getPaymentStatus(String paymentId, String currentUserId);
}
