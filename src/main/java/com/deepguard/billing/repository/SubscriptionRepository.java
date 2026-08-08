package com.deepguard.billing.repository;

import com.deepguard.billing.entity.Subscription;
import com.deepguard.billing.enums.SubscriptionStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.time.LocalDateTime;

public interface SubscriptionRepository extends JpaRepository<Subscription, String> {

    List<Subscription> findByUser_IdOrderByStartDateDesc(String userId);

    Optional<Subscription> findFirstByUser_IdAndStatusOrderByEndDateDesc(String userId, SubscriptionStatus status);

    Optional<Subscription> findFirstByUser_IdAndStatusAndEndDateAfterOrderByEndDateDesc(
            String userId,
            SubscriptionStatus status,
            LocalDateTime now
    );
}
