package com.deepguard.billing.service;

import com.deepguard.billing.dto.request.CreateSepayPaymentRequest;
import com.deepguard.billing.dto.response.CreateSepayPaymentResponse;

public interface SepayPaymentService {

    CreateSepayPaymentResponse createPayment(CreateSepayPaymentRequest request, String currentUserId);
}
