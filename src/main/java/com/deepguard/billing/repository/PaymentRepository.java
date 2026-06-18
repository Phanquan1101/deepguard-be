package com.deepguard.billing.repository;

import com.deepguard.billing.entity.Payment;
import com.deepguard.billing.enums.PaymentStatus;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PaymentRepository extends JpaRepository<Payment, String> {

    Optional<Payment> findByTransactionCode(String transactionCode);

    List<Payment> findByUser_IdOrderByCreatedAtDesc(String userId);

    List<Payment> findByPaymentMethodAndStatus(String paymentMethod, PaymentStatus status);

    boolean existsByTransactionCode(String transactionCode);

    @EntityGraph(attributePaths = {"user", "subscription", "subscription.pricingPlan"})
    @Query("""
            select p
            from Payment p
            left join p.user u
            where (:keyword is null
                   or lower(p.transactionCode) like lower(concat('%', :keyword, '%'))
                   or lower(u.email) like lower(concat('%', :keyword, '%'))
                   or lower(u.username) like lower(concat('%', :keyword, '%')))
              and (:status is null or p.status = :status)
              and (:paymentMethod is null or lower(p.paymentMethod) = lower(:paymentMethod))
              and (:startDate is null or p.createdAt >= :startDate)
              and (:endDate is null or p.createdAt <= :endDate)
            """)
    Page<Payment> findAllWithAdminFilters(
            @Param("keyword") String keyword,
            @Param("status") PaymentStatus status,
            @Param("paymentMethod") String paymentMethod,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            Pageable pageable);
}
