package com.deepguard.billing.repository;

import com.deepguard.billing.entity.UserCredit;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserCreditRepository extends JpaRepository<UserCredit, String> {

    Optional<UserCredit> findByUser_Id(String userId);

    boolean existsByUser_Id(String userId);
}
