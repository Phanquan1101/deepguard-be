package com.deepguard.billing.repository;

import com.deepguard.billing.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, String> {

    Optional<Payment> findByTransactionCode(String transactionCode);

    List<Payment> findByUser_IdOrderByCreatedAtDesc(String userId);

    boolean existsByTransactionCode(String transactionCode);
}
