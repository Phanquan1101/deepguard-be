package com.deepguard.billing.service;

import com.deepguard.billing.dto.request.CreateVnpayPaymentRequest;
import com.deepguard.billing.dto.response.CreateVnpayPaymentResponse;
import com.deepguard.billing.dto.response.PaymentHistoryResponse;
import com.deepguard.billing.dto.response.VnpayIpnResponse;
import com.deepguard.billing.dto.response.VnpayReturnResponse;

import java.util.List;
import java.util.Map;

public interface VnpayPaymentService {

    CreateVnpayPaymentResponse createPayment(CreateVnpayPaymentRequest request, String currentUserId, String clientIp);

    VnpayReturnResponse handleReturn(Map<String, String> params);

    VnpayIpnResponse handleIpn(Map<String, String> params);

    List<PaymentHistoryResponse> getMyPayments(String currentUserId);
}
