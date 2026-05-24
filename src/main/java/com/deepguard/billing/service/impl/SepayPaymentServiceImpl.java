package com.deepguard.billing.service.impl;

import com.deepguard.auth.entity.User;
import com.deepguard.auth.repository.UserRepository;
import com.deepguard.billing.config.SepayProperties;
import com.deepguard.billing.dto.request.CreateSepayPaymentRequest;
import com.deepguard.billing.dto.response.CreateSepayPaymentResponse;
import com.deepguard.billing.entity.Payment;
import com.deepguard.billing.entity.PricingPlan;
import com.deepguard.billing.entity.Subscription;
import com.deepguard.billing.enums.PaymentStatus;
import com.deepguard.billing.enums.SubscriptionStatus;
import com.deepguard.billing.repository.PaymentRepository;
import com.deepguard.billing.repository.PricingPlanRepository;
import com.deepguard.billing.repository.SubscriptionRepository;
import com.deepguard.billing.service.SepayPaymentService;
import com.deepguard.common.exception.BusinessException;
import com.deepguard.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SepayPaymentServiceImpl implements SepayPaymentService {

    private static final String PAYMENT_METHOD_SEPAY = "SEPAY";
    private static final int MAX_TRANSACTION_CODE_ATTEMPTS = 5;
    private static final String DEFAULT_TRANSFER_PREFIX = "SEPAY";

    private final PricingPlanRepository pricingPlanRepository;
    private final PaymentRepository paymentRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final UserRepository userRepository;
    private final SepayProperties sepayProperties;

    @Override
    @Transactional
    public CreateSepayPaymentResponse createPayment(CreateSepayPaymentRequest request, String currentUserId) {
        PricingPlan pricingPlan = pricingPlanRepository.findById(request.getPricingPlanId())
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Pricing plan not found"));

        User currentUser = userRepository.findById(currentUserId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        LocalDateTime now = LocalDateTime.now();
        Subscription subscription = Subscription.builder()
                .user(currentUser)
                .pricingPlan(pricingPlan)
                .startDate(now)
                .endDate(now.plusMonths(1))
                .status(SubscriptionStatus.PENDING)
                .build();
        Subscription savedSubscription = subscriptionRepository.save(subscription);

        Payment payment = Payment.builder()
                .user(currentUser)
                .subscription(savedSubscription)
                .amount(pricingPlan.getPrice())
                .paymentMethod(PAYMENT_METHOD_SEPAY)
                .transactionCode(generateUniqueTransactionCode())
                .status(PaymentStatus.PENDING)
                .build();
        Payment savedPayment = paymentRepository.save(payment);

        String transferContent = savedPayment.getTransactionCode();
        String qrUrl = buildQrUrl(savedPayment.getAmount(), transferContent);

        return CreateSepayPaymentResponse.builder()
                .paymentId(savedPayment.getId())
                .transactionCode(savedPayment.getTransactionCode())
                .amount(savedPayment.getAmount())
                .status(savedPayment.getStatus())
                .planId(pricingPlan.getId())
                .planName(pricingPlan.getName())
                .credits(pricingPlan.getCredits())
                .bankCode(sepayProperties.getBankCode())
                .bankAccountNo(sepayProperties.getBankAccountNo())
                .bankAccountName(sepayProperties.getBankAccountName())
                .transferContent(transferContent)
                .qrUrl(qrUrl)
                .build();
    }

    private String generateUniqueTransactionCode() {
        String prefix = getTransferPrefix();
        for (int i = 0; i < MAX_TRANSACTION_CODE_ATTEMPTS; i++) {
            String randomPart = UUID.randomUUID().toString().replace("-", "").substring(0, 20).toUpperCase();
            String transactionCode = prefix + "_" + randomPart;
            if (!paymentRepository.existsByTransactionCode(transactionCode)) {
                return transactionCode;
            }
        }
        throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR, "Failed to generate unique SePay transaction code");
    }

    private String getTransferPrefix() {
        String configured = sepayProperties.getTransferContentPrefix();
        if (configured == null || configured.isBlank()) {
            return DEFAULT_TRANSFER_PREFIX;
        }
        return configured.trim().toUpperCase();
    }

    private String buildQrUrl(BigDecimal amount, String transferContent) {
        String qrBaseUrl = sepayProperties.getQrBaseUrl();
        String bankCode = sepayProperties.getBankCode();
        String bankAccountNo = sepayProperties.getBankAccountNo();
        String qrTemplate = sepayProperties.getQrTemplate();
        String bankAccountName = sepayProperties.getBankAccountName();

        String amountParam = amount.setScale(0, RoundingMode.HALF_UP).toPlainString();
        String encodedAddInfo = urlEncodeUtf8(transferContent);
        String encodedAccountName = urlEncodeUtf8(bankAccountName);

        return String.format(
                "%s/%s-%s-%s.png?amount=%s&addInfo=%s&accountName=%s",
                trimTrailingSlash(qrBaseUrl),
                bankCode,
                bankAccountNo,
                qrTemplate,
                amountParam,
                encodedAddInfo,
                encodedAccountName
        );
    }

    private String urlEncodeUtf8(String value) {
        String safeValue = value == null ? "" : value;
        return URLEncoder.encode(safeValue, StandardCharsets.UTF_8).replace("+", "%20");
    }

    private String trimTrailingSlash(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }
}
