package com.deepguard.billing.repository;

import com.deepguard.billing.entity.Payment;
import com.deepguard.billing.enums.PaymentStatus;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.domain.Specification;

public interface PaymentRepository extends JpaRepository<Payment, String>, JpaSpecificationExecutor<Payment> {

    interface DailyRevenueProjection {
        LocalDate getSummaryDate();
        BigDecimal getRevenue();
        long getTransactions();
    }

    Optional<Payment> findByTransactionCode(String transactionCode);

    List<Payment> findByUser_IdOrderByCreatedAtDesc(String userId);

    List<Payment> findByPaymentMethodAndStatus(String paymentMethod, PaymentStatus status);

    /**
     * Finds a completed purchase whose subscription is still within its paid
     * period. This is also used to recover subscriptions created before the
     * payment webhook finished updating their status.
     */
    @Query("""
            select p
            from Payment p
            join fetch p.subscription subscription
            join fetch subscription.pricingPlan
            where p.user.id = :userId
              and p.status = :status
              and subscription.endDate > :now
            order by subscription.endDate desc, p.createdAt desc
            """)
    List<Payment> findSuccessfulPaymentsWithUnexpiredSubscription(
            @Param("userId") String userId,
            @Param("status") PaymentStatus status,
            @Param("now") java.time.LocalDateTime now,
            Pageable pageable);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select p
            from Payment p
            join fetch p.user
            join fetch p.subscription subscription
            join fetch subscription.pricingPlan
            where p.paymentMethod = :paymentMethod and p.status = :status
            """)
    List<Payment> findByPaymentMethodAndStatusForUpdate(
            @Param("paymentMethod") String paymentMethod,
            @Param("status") PaymentStatus status);

    boolean existsByTransactionCode(String transactionCode);

    @Query("select payment.status, count(payment) from Payment payment group by payment.status")
    List<Object[]> countByStatusForAdminAnalytics();

    @Query("select payment.paymentMethod, count(payment) from Payment payment group by payment.paymentMethod")
    List<Object[]> countByPaymentMethodForAdminAnalytics();

    @Query("""
            select pricingPlan.name, count(payment)
            from Payment payment
            join payment.subscription subscription
            join subscription.pricingPlan pricingPlan
            group by pricingPlan.name
            """)
    List<Object[]> countByPricingPlanForAdminAnalytics();

    @Query("select sum(payment.amount) from Payment payment where payment.status = :status")
    BigDecimal sumAmountByStatus(@Param("status") PaymentStatus status);

    @Query(value = """
            select cast(payment.created_at as date) as "summaryDate",
                   coalesce(sum(case when payment.status = 'SUCCESS' then payment.amount else 0 end), 0) as revenue,
                   count(*) as transactions
            from payments payment
            where payment.created_at >= :startAt
            group by cast(payment.created_at as date)
            order by "summaryDate"
            """, nativeQuery = true)
    List<DailyRevenueProjection> findDailyRevenueSince(@Param("startAt") LocalDateTime startAt);

    @Override
    @EntityGraph(attributePaths = {"user", "subscription", "subscription.pricingPlan"})
    Page<Payment> findAll(Specification<Payment> specification, Pageable pageable);
}
