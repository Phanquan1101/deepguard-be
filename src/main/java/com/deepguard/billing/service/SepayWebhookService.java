package com.deepguard.billing.service;

import com.deepguard.billing.dto.response.SepayWebhookResponse;

public interface SepayWebhookService {

    SepayWebhookResponse handleWebhook(String rawBody, String signatureHeader, String timestampHeader);
}
