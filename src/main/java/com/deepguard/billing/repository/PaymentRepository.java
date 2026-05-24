package com.deepguard.billing.repository;

import com.deepguard.billing.entity.Payment;
import com.deepguard.billing.enums.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, String> {

    Optional<Payment> findByTransactionCode(String transactionCode);

    List<Payment> findByUser_IdOrderByCreatedAtDesc(String userId);

    List<Payment> findByPaymentMethodAndStatus(String paymentMethod, PaymentStatus status);

    boolean existsByTransactionCode(String transactionCode);
}
