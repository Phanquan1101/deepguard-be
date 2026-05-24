package com.deepguard.billing.service.impl;

import com.deepguard.billing.config.VnpayProperties;
import com.deepguard.billing.entity.Payment;
import com.deepguard.billing.entity.PricingPlan;
import com.deepguard.billing.service.VnpaySignatureService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class VnpaySignatureServiceImpl implements VnpaySignatureService {

    private static final DateTimeFormatter VNPAY_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private static final String HMAC_SHA512 = "HmacSHA512";
    private static final ZoneId VNPAY_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    private final VnpayProperties vnpayProperties;

    @Override
    public String buildPaymentUrl(Payment payment, PricingPlan pricingPlan, String clientIp) {
        ZonedDateTime createDate = ZonedDateTime.now(VNPAY_ZONE);
        ZonedDateTime expireDate = createDate.plusMinutes(15);

        Map<String, String> vnpParams = new HashMap<>();
        vnpParams.put("vnp_Version", valueOrDefault(vnpayProperties.getVersion(), "2.1.0"));
        vnpParams.put("vnp_Command", valueOrDefault(vnpayProperties.getCommand(), "pay"));
        vnpParams.put("vnp_TmnCode", vnpayProperties.getTmnCode());
        vnpParams.put("vnp_Amount", toVnpayAmount(payment.getAmount()));
        vnpParams.put("vnp_CurrCode", valueOrDefault(vnpayProperties.getCurrCode(), "VND"));
        vnpParams.put("vnp_TxnRef", payment.getTransactionCode());
        vnpParams.put("vnp_OrderInfo", buildOrderInfo(pricingPlan, payment));
        vnpParams.put("vnp_OrderType", valueOrDefault(vnpayProperties.getOrderType(), "other"));
        vnpParams.put("vnp_Locale", valueOrDefault(vnpayProperties.getLocale(), "vn"));
        vnpParams.put("vnp_ReturnUrl", vnpayProperties.getReturnUrl());
        vnpParams.put("vnp_IpAddr", clientIp);
        vnpParams.put("vnp_CreateDate", createDate.format(VNPAY_DATE_FORMATTER));
        vnpParams.put("vnp_ExpireDate", expireDate.format(VNPAY_DATE_FORMATTER));

        SignPayload payload = buildSignPayload(vnpParams);
        String secureHash = hmacSha512(vnpayProperties.getHashSecret(), payload.hashData());
        String paymentUrl = vnpayProperties.getPayUrl() + "?" + payload.query() + "&vnp_SecureHash=" + secureHash;

        log.debug("VNPAY sorted params: {}", payload.sortedFields());
        log.debug("VNPAY hashData: {}", payload.hashData());
        log.debug("VNPAY paymentUrl: {}", paymentUrl);

        return paymentUrl;
    }

    @Override
    public boolean verifySignature(Map<String, String> params) {
        String secureHash = params.get("vnp_SecureHash");
        if (secureHash == null || secureHash.isBlank()) {
            return false;
        }

        Map<String, String> signParams = new HashMap<>();
        for (Map.Entry<String, String> entry : params.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            if (key == null || !key.startsWith("vnp_")) {
                continue;
            }
            if ("vnp_SecureHash".equals(key) || "vnp_SecureHashType".equals(key)) {
                continue;
            }
            if (value == null || value.isBlank()) {
                continue;
            }
            signParams.put(key, value);
        }

        SignPayload payload = buildSignPayload(signParams);
        String calculatedHash = hmacSha512(vnpayProperties.getHashSecret(), payload.hashData());
        return calculatedHash.equalsIgnoreCase(secureHash);
    }

    private String toVnpayAmount(BigDecimal amount) {
        return amount.multiply(BigDecimal.valueOf(100))
                .setScale(0, RoundingMode.HALF_UP)
                .toPlainString();
    }

    private String buildOrderInfo(PricingPlan pricingPlan, Payment payment) {
        return "Pay plan " + pricingPlan.getName() + " " + payment.getTransactionCode();
    }

    private SignPayload buildSignPayload(Map<String, String> vnpParams) {
        List<String> fieldNames = new ArrayList<>();
        for (Map.Entry<String, String> entry : vnpParams.entrySet()) {
            if (entry.getValue() != null && !entry.getValue().isBlank()) {
                fieldNames.add(entry.getKey());
            }
        }
        Collections.sort(fieldNames);

        StringBuilder hashData = new StringBuilder();
        StringBuilder query = new StringBuilder();

        for (int i = 0; i < fieldNames.size(); i++) {
            String fieldName = fieldNames.get(i);
            String fieldValue = vnpParams.get(fieldName);

            hashData.append(fieldName)
                    .append('=')
                    .append(urlEncodeAscii(fieldValue));

            query.append(urlEncodeAscii(fieldName))
                    .append('=')
                    .append(urlEncodeAscii(fieldValue));

            if (i < fieldNames.size() - 1) {
                hashData.append('&');
                query.append('&');
            }
        }

        return new SignPayload(hashData.toString(), query.toString(), fieldNames);
    }

    private String urlEncodeAscii(String value) {
        return URLEncoder.encode(value, StandardCharsets.US_ASCII);
    }

    private String hmacSha512(String secretKey, String data) {
        try {
            Mac mac = Mac.getInstance(HMAC_SHA512);
            mac.init(new SecretKeySpec(secretKey.getBytes(StandardCharsets.UTF_8), HMAC_SHA512));
            byte[] hash = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            StringBuilder result = new StringBuilder(hash.length * 2);
            for (byte b : hash) {
                result.append(String.format("%02x", b));
            }
            return result.toString();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to sign VNPAY request", e);
        }
    }

    private String valueOrDefault(String value, String defaultValue) {
        return value == null || value.isBlank() ? defaultValue : value;
    }

    private record SignPayload(String hashData, String query, List<String> sortedFields) {
    }
}
