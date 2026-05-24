package com.deepguard.billing.service.impl;

import com.deepguard.auth.entity.User;
import com.deepguard.auth.repository.UserRepository;
import com.deepguard.billing.dto.request.CreateVnpayPaymentRequest;
import com.deepguard.billing.dto.response.CreateVnpayPaymentResponse;
import com.deepguard.billing.dto.response.PaymentHistoryResponse;
import com.deepguard.billing.dto.response.VnpayIpnResponse;
import com.deepguard.billing.dto.response.VnpayReturnResponse;
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
import com.deepguard.billing.repository.PricingPlanRepository;
import com.deepguard.billing.repository.SubscriptionRepository;
import com.deepguard.billing.repository.UserCreditRepository;
import com.deepguard.billing.service.VnpayPaymentService;
import com.deepguard.billing.service.VnpaySignatureService;
import com.deepguard.common.exception.BusinessException;
import com.deepguard.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class VnpayPaymentServiceImpl implements VnpayPaymentService {

    private static final String PAYMENT_METHOD_VNPAY = "VNPAY";
    private static final int MAX_TRANSACTION_CODE_ATTEMPTS = 5;

    private final PricingPlanRepository pricingPlanRepository;
    private final PaymentRepository paymentRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final UserRepository userRepository;
    private final UserCreditRepository userCreditRepository;
    private final CreditTransactionRepository creditTransactionRepository;
    private final VnpaySignatureService vnpaySignatureService;

    @Override
    @Transactional
    public CreateVnpayPaymentResponse createPayment(CreateVnpayPaymentRequest request, String currentUserId, String clientIp) {
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
                .paymentMethod(PAYMENT_METHOD_VNPAY)
                .transactionCode(generateUniqueTransactionCode())
                .status(PaymentStatus.PENDING)
                .build();
        Payment savedPayment = paymentRepository.save(payment);

        String paymentUrl = vnpaySignatureService.buildPaymentUrl(savedPayment, pricingPlan, clientIp);

        return CreateVnpayPaymentResponse.builder()
                .paymentId(savedPayment.getId())
                .transactionCode(savedPayment.getTransactionCode())
                .paymentUrl(paymentUrl)
                .status(savedPayment.getStatus())
                .amount(savedPayment.getAmount())
                .planId(pricingPlan.getId())
                .planName(pricingPlan.getName())
                .credits(pricingPlan.getCredits())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public VnpayReturnResponse handleReturn(Map<String, String> params) {
        boolean validSignature = vnpaySignatureService.verifySignature(params);

        String transactionCode = params.get("vnp_TxnRef");
        String responseCode = params.get("vnp_ResponseCode");
        String transactionStatus = params.get("vnp_TransactionStatus");

        if (!validSignature) {
            return VnpayReturnResponse.builder()
                    .validSignature(false)
                    .transactionCode(transactionCode)
                    .responseCode(responseCode)
                    .transactionStatus(transactionStatus)
                    .message("Invalid VNPAY signature")
                    .build();
        }

        Payment payment = transactionCode == null ? null : paymentRepository.findByTransactionCode(transactionCode).orElse(null);
        if (payment == null) {
            return VnpayReturnResponse.builder()
                    .validSignature(true)
                    .transactionCode(transactionCode)
                    .responseCode(responseCode)
                    .transactionStatus(transactionStatus)
                    .message("Payment not found")
                    .build();
        }

        Subscription subscription = payment.getSubscription();
        PricingPlan pricingPlan = subscription != null ? subscription.getPricingPlan() : null;

        boolean successReturn = "00".equals(responseCode) && "00".equals(transactionStatus);

        return VnpayReturnResponse.builder()
                .validSignature(true)
                .transactionCode(payment.getTransactionCode())
                .responseCode(responseCode)
                .transactionStatus(transactionStatus)
                .message(successReturn
                        ? "Payment returned successfully. Waiting for IPN confirmation."
                        : "Payment returned with failure status.")
                .paymentId(payment.getId())
                .paymentStatus(payment.getStatus())
                .amount(payment.getAmount())
                .planId(pricingPlan != null ? pricingPlan.getId() : null)
                .planName(pricingPlan != null ? pricingPlan.getName() : null)
                .credits(pricingPlan != null ? pricingPlan.getCredits() : null)
                .build();
    }

    @Override
    @Transactional
    public VnpayIpnResponse handleIpn(Map<String, String> params) {
        if (!vnpaySignatureService.verifySignature(params)) {
            return VnpayIpnResponse.builder().RspCode("97").Message("Invalid signature").build();
        }

        String transactionCode = params.get("vnp_TxnRef");
        String vnpAmount = params.get("vnp_Amount");
        String responseCode = params.get("vnp_ResponseCode");
        String transactionStatus = params.get("vnp_TransactionStatus");

        Payment payment = paymentRepository.findByTransactionCode(transactionCode).orElse(null);
        if (payment == null) {
            return VnpayIpnResponse.builder().RspCode("01").Message("Order not found").build();
        }

        if (PaymentStatus.SUCCESS.equals(payment.getStatus())) {
            return VnpayIpnResponse.builder().RspCode("02").Message("Order already confirmed").build();
        }

        String expectedAmount = payment.getAmount().multiply(BigDecimal.valueOf(100)).toBigInteger().toString();
        if (vnpAmount == null || !expectedAmount.equals(vnpAmount)) {
            return VnpayIpnResponse.builder().RspCode("04").Message("Invalid amount").build();
        }

        Subscription subscription = payment.getSubscription();
        boolean successPayment = "00".equals(responseCode) && "00".equals(transactionStatus);

        if (successPayment) {
            payment.setStatus(PaymentStatus.SUCCESS);
            paymentRepository.save(payment);

            LocalDateTime now = LocalDateTime.now();
            subscription.setStatus(SubscriptionStatus.ACTIVE);
            subscription.setStartDate(now);
            subscription.setEndDate(now.plusMonths(1));
            subscriptionRepository.save(subscription);

            PricingPlan pricingPlan = subscription.getPricingPlan();
            User user = payment.getUser();

            UserCredit userCredit = userCreditRepository.findByUser_Id(user.getId())
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
                    .description("Purchase credits via VNPAY - plan " + pricingPlan.getName())
                    .build();
            creditTransactionRepository.save(creditTransaction);

            return VnpayIpnResponse.builder().RspCode("00").Message("Confirm Success").build();
        }

        payment.setStatus(PaymentStatus.FAILED);
        paymentRepository.save(payment);

        subscription.setStatus(SubscriptionStatus.CANCELLED);
        subscriptionRepository.save(subscription);

        return VnpayIpnResponse.builder().RspCode("00").Message("Confirm Success").build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<PaymentHistoryResponse> getMyPayments(String currentUserId) {
        return paymentRepository.findByUser_IdOrderByCreatedAtDesc(currentUserId)
                .stream()
                .map(payment -> {
                    Subscription subscription = payment.getSubscription();
                    PricingPlan pricingPlan = subscription != null ? subscription.getPricingPlan() : null;

                    return PaymentHistoryResponse.builder()
                            .paymentId(payment.getId())
                            .transactionCode(payment.getTransactionCode())
                            .amount(payment.getAmount())
                            .paymentMethod(payment.getPaymentMethod())
                            .status(payment.getStatus())
                            .createdAt(payment.getCreatedAt())
                            .subscriptionId(subscription != null ? subscription.getId() : null)
                            .subscriptionStatus(subscription != null ? subscription.getStatus() : null)
                            .pricingPlanId(pricingPlan != null ? pricingPlan.getId() : null)
                            .pricingPlanName(pricingPlan != null ? pricingPlan.getName() : null)
                            .credits(pricingPlan != null ? pricingPlan.getCredits() : null)
                            .build();
                })
                .toList();
    }

    private String generateUniqueTransactionCode() {
        for (int i = 0; i < MAX_TRANSACTION_CODE_ATTEMPTS; i++) {
            String code = "VNPAY_" + UUID.randomUUID().toString().replace("-", "").substring(0, 20);
            if (!paymentRepository.existsByTransactionCode(code)) {
                return code;
            }
        }
        throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR, "Failed to generate unique transaction code");
    }
}
