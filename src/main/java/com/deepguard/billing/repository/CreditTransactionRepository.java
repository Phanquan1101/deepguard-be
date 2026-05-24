package com.deepguard.billing.repository;

import com.deepguard.billing.entity.CreditTransaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CreditTransactionRepository extends JpaRepository<CreditTransaction, String> {

    List<CreditTransaction> findByUserCredit_User_IdOrderByCreatedAtDesc(String userId);
}
