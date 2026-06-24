package com.deepguard.billing.repository;

import com.deepguard.billing.entity.Payment;
import com.deepguard.billing.enums.PaymentStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.domain.Specification;

public interface PaymentRepository extends JpaRepository<Payment, String>, JpaSpecificationExecutor<Payment> {

    Optional<Payment> findByTransactionCode(String transactionCode);

    List<Payment> findByUser_IdOrderByCreatedAtDesc(String userId);

    List<Payment> findByPaymentMethodAndStatus(String paymentMethod, PaymentStatus status);

    boolean existsByTransactionCode(String transactionCode);

    @Override
    @EntityGraph(attributePaths = {"user", "subscription", "subscription.pricingPlan"})
    Page<Payment> findAll(Specification<Payment> specification, Pageable pageable);
}
