package com.deepguard.billing.repository;

import com.deepguard.billing.entity.Subscription;
import com.deepguard.billing.enums.SubscriptionStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SubscriptionRepository extends JpaRepository<Subscription, String> {

    List<Subscription> findByUser_IdOrderByStartDateDesc(String userId);

    Optional<Subscription> findFirstByUser_IdAndStatusOrderByEndDateDesc(String userId, SubscriptionStatus status);
}
