package com.deepguard.billing.service.impl;

import com.deepguard.auth.entity.User;
import com.deepguard.billing.config.SepayProperties;
import com.deepguard.billing.dto.request.SepayWebhookRequest;
import com.deepguard.billing.dto.response.SepayWebhookResponse;
import com.deepguard.billing.entity.CreditTransaction;
import com.deepguard.billing.entity.Payment;
import com.deepguard.billing.entity.PricingPlan;
import com.deepguard.billing.entity.Subscription;
import com.deepguard.billing.entity.UserCredit;
import com.deepguard.billing.enums.PaymentStatus;
import com.deepguard.billing.enums.SubscriptionStatus;
import com.deepguard.billing.enums.TransactionType;
import com.deepguard.billing.repository.CreditTransactionRepository;
import com.deepguard.billing.repository.PaymentRepository;
import com.deepguard.billing.repository.SubscriptionRepository;
import com.deepguard.billing.repository.UserCreditRepository;
import com.deepguard.billing.service.SepayWebhookService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
@Slf4j
public class SepayWebhookServiceImpl implements SepayWebhookService {

    private static final String PAYMENT_METHOD_SEPAY = "SEPAY";
    private static final String HMAC_SHA256 = "HmacSHA256";
    private static final Duration MAX_TIMESTAMP_SKEW = Duration.ofMinutes(5);

    private final ObjectMapper objectMapper;
    private final SepayProperties sepayProperties;
    private final PaymentRepository paymentRepository;
    private final UserCreditRepository userCreditRepository;
    private final CreditTransactionRepository creditTransactionRepository;
    private final SubscriptionRepository subscriptionRepository;

    @Override
    @Transactional
    public SepayWebhookResponse handleWebhook(String rawBody, String signatureHeader, String timestampHeader) {
        log.info("Handling SePay webhook with timestamp={}", timestampHeader);

        if (!verifySignature(rawBody, signatureHeader, timestampHeader)
                || !isTimestampFresh(timestampHeader)) {
            log.warn("SePay webhook authentication failed");
            return SepayWebhookResponse.builder()
                    .code("97")
                    .message("Invalid signature")
                    .build();
        }

        SepayWebhookRequest payload = parsePayload(rawBody);
        if (payload == null) {
            return SepayWebhookResponse.builder()
                    .code("01")
                    .message("Invalid payload")
                    .build();
        }

        if (!"in".equalsIgnoreCase(safeText(payload.getTransferType()))) {
            return SepayWebhookResponse.builder()
                    .code("00")
                    .message("Ignored non-in transaction")
                    .build();
        }

        Payment payment = findMatchingPayment(payload);
        if (payment == null) {
            log.warn("SePay webhook could not match payment. content={}, description={}, referenceCode={}",
                    payload.getContent(), payload.getDescription(), payload.getReferenceCode());
            return SepayWebhookResponse.builder()
                    .code("00")
                    .message("Payment not found or ignored")
                    .build();
        }

        if (PaymentStatus.SUCCESS.equals(payment.getStatus())) {
            return SepayWebhookResponse.builder()
                    .code("00")
                    .message("Already processed")
                    .build();
        }

        if (!PaymentStatus.PENDING.equals(payment.getStatus())) {
            return SepayWebhookResponse.builder()
                    .code("00")
                    .message("Payment not pending")
                    .build();
        }

        BigDecimal transferAmount = payload.getTransferAmount();
        if (transferAmount == null || payment.getAmount() == null || transferAmount.compareTo(payment.getAmount()) != 0) {
            log.warn("SePay webhook amount mismatch for paymentId={}. expected={}, actual={}",
                    payment.getId(), payment.getAmount(), transferAmount);
            return SepayWebhookResponse.builder()
                    .code("04")
                    .message("Invalid amount")
                    .build();
        }

        if (!isValidAccountNumber(payload.getAccountNumber())) {
            log.warn("SePay webhook account number mismatch for paymentId={}. expected={}, actual={}",
                    payment.getId(), sepayProperties.getBankAccountNo(), payload.getAccountNumber());
            return SepayWebhookResponse.builder()
                    .code("05")
                    .message("Invalid account number")
                    .build();
        }

        completePaymentSuccess(payment);

        return SepayWebhookResponse.builder()
                .code("00")
                .message("Success")
                .build();
    }

    private SepayWebhookRequest parsePayload(String rawBody) {
        try {
            return objectMapper.readValue(rawBody, SepayWebhookRequest.class);
        } catch (Exception ex) {
            log.error("Cannot parse SePay webhook payload: {}", ex.getMessage(), ex);
            return null;
        }
    }

    private Payment findMatchingPayment(SepayWebhookRequest payload) {
        String content = safeText(payload.getContent());
        if (content.isBlank()) {
            content = safeText(payload.getDescription());
        }
        String normalizedContent = normalize(content);
        if (normalizedContent.isBlank()) {
            return null;
        }

        Payment pendingPayment = findByNormalizedContent(normalizedContent,
                paymentRepository.findByPaymentMethodAndStatusForUpdate(PAYMENT_METHOD_SEPAY, PaymentStatus.PENDING));
        if (pendingPayment != null) {
            return pendingPayment;
        }

        return findByNormalizedContent(normalizedContent,
                paymentRepository.findByPaymentMethodAndStatus(PAYMENT_METHOD_SEPAY, PaymentStatus.SUCCESS));
    }

    private Payment findByNormalizedContent(String normalizedContent, List<Payment> payments) {
        for (Payment payment : payments) {
            String normalizedTransactionCode = normalize(payment.getTransactionCode());
            if (!normalizedTransactionCode.isBlank() && normalizedContent.contains(normalizedTransactionCode)) {
                return payment;
            }
        }
        return null;
    }

    private String normalize(String value) {
        if (value == null) {
            return "";
        }
        String upper = value.toUpperCase(Locale.ROOT);
        return upper.replaceAll("[^A-Z0-9]", "");
    }

    private String safeText(String value) {
        return value == null ? "" : value.trim();
    }

    private boolean isValidAccountNumber(String payloadAccountNumber) {
        String configuredAccountNo = safeText(sepayProperties.getBankAccountNo());
        if (configuredAccountNo.isBlank()) {
            return true;
        }
        return configuredAccountNo.equals(safeText(payloadAccountNumber));
    }

    private void completePaymentSuccess(Payment payment) {
        payment.setStatus(PaymentStatus.SUCCESS);
        paymentRepository.save(payment);

        Subscription subscription = payment.getSubscription();
        LocalDateTime now = LocalDateTime.now();
        subscription.setStatus(SubscriptionStatus.ACTIVE);
        subscription.setStartDate(now);
        subscription.setEndDate(now.plusMonths(1));
        subscriptionRepository.save(subscription);

        PricingPlan pricingPlan = subscription.getPricingPlan();
        User user = payment.getUser();

        UserCredit userCredit = userCreditRepository.findByUserIdForUpdate(user.getId())
                .orElseGet(() -> UserCredit.builder()
                        .user(user)
                        .remainingCredits(0)
                        .usedCredits(0)
                        .build());

        int currentRemaining = userCredit.getRemainingCredits() == null ? 0 : userCredit.getRemainingCredits();
        int planCredits = pricingPlan.getCredits() == null ? 0 : pricingPlan.getCredits();
        userCredit.setRemainingCredits(currentRemaining + planCredits);

        UserCredit savedUserCredit = userCreditRepository.save(userCredit);

        CreditTransaction creditTransaction = CreditTransaction.builder()
                .userCredit(savedUserCredit)
                .amount(planCredits)
                .transactionType(TransactionType.PURCHASE)
                .description("Purchase credits via SePay - plan " + pricingPlan.getName())
                .build();
        creditTransactionRepository.save(creditTransaction);
    }

    private boolean verifySignature(String rawBody, String signatureHeader, String timestampHeader) {
        String secret = sepayProperties.getWebhookSecret();
        if (secret == null || secret.isBlank()
                || signatureHeader == null || signatureHeader.isBlank()
                || timestampHeader == null || timestampHeader.isBlank()) {
            return false;
        }

        String normalizedSignature = signatureHeader.trim();
        if (normalizedSignature.regionMatches(true, 0, "sha256=", 0, "sha256=".length())) {
            normalizedSignature = normalizedSignature.substring("sha256=".length());
        }

        String safeRawBody = rawBody == null ? "" : rawBody;
        String signedPayload = timestampHeader.trim() + "." + safeRawBody;
        String expected = hmacSha256Hex(secret, signedPayload);
        String actual = normalizedSignature.toLowerCase(Locale.ROOT);

        if (log.isDebugEnabled()) {
            log.debug("SePay signature verify payloadLength={}, timestamp={}", signedPayload.length(), timestampHeader);
            log.debug("SePay signature verify expected={}, actual={}", expected, actual);
        }

        return MessageDigest.isEqual(
                expected.getBytes(StandardCharsets.UTF_8),
                actual.getBytes(StandardCharsets.UTF_8)
        );
    }

    private boolean isTimestampFresh(String timestampHeader) {
        try {
            long timestampSeconds = Long.parseLong(timestampHeader.trim());
            Instant signedAt = Instant.ofEpochSecond(timestampSeconds);
            return Duration.between(signedAt, Instant.now()).abs().compareTo(MAX_TIMESTAMP_SKEW) <= 0;
        } catch (RuntimeException ex) {
            return false;
        }
    }

    private String hmacSha256Hex(String secretKey, String data) {
        try {
            Mac mac = Mac.getInstance(HMAC_SHA256);
            mac.init(new SecretKeySpec(secretKey.getBytes(StandardCharsets.UTF_8), HMAC_SHA256));
            byte[] hash = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            StringBuilder result = new StringBuilder(hash.length * 2);
            for (byte b : hash) {
                result.append(String.format("%02x", b));
            }
            return result.toString();
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to verify SePay signature", ex);
        }
    }
}
