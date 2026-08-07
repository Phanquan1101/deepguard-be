package com.deepguard.billing.controller;

import com.deepguard.billing.dto.response.SepayWebhookResponse;
import com.deepguard.billing.service.SepayWebhookService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/billing/payments/sepay")
@Slf4j
@RequiredArgsConstructor
public class SepayWebhookController {

    private final SepayWebhookService sepayWebhookService;

    @PostMapping("/webhook")
    public ResponseEntity<SepayWebhookResponse> handleWebhook(
            @RequestBody String rawBody,
            @RequestHeader Map<String, String> headers
    ) {
        String signatureHeader = findHeaderIgnoreCase(headers, "signature", "x-sepay-signature", "x-signature");
        String timestampHeader = findHeaderIgnoreCase(headers, "timestamp", "x-sepay-timestamp", "x-timestamp");
        log.info("SePay webhook received: payloadLength={}, hasSignature={}, hasTimestamp={}",
                rawBody.length(), signatureHeader != null, timestampHeader != null);

        SepayWebhookResponse response = sepayWebhookService.handleWebhook(rawBody, signatureHeader, timestampHeader);
        if ("97".equals(response.getCode())) {
            response.setSuccess(false);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
        }

        // SePay treats a 2xx response as completed only when success is true.
        response.setSuccess("00".equals(response.getCode()));
        return ResponseEntity.ok(response);
    }

    private String findHeaderIgnoreCase(Map<String, String> headers, String... candidates) {
        if (headers == null || headers.isEmpty()) {
            return null;
        }

        for (String candidate : candidates) {
            for (Map.Entry<String, String> header : headers.entrySet()) {
                if (header.getKey() != null && header.getKey().equalsIgnoreCase(candidate)) {
                    return header.getValue();
                }
            }
        }
        return null;
    }
}
