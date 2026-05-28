package com.deepguard.billing.repository;

import com.deepguard.billing.entity.CreditRule;
import com.deepguard.billing.enums.ActionType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CreditRuleRepository extends JpaRepository<CreditRule, String> {

    Optional<CreditRule> findFirstByActionTypeAndIsActiveTrue(ActionType actionType);

    List<CreditRule> findByIsActiveTrue();

    CreditRule findByActionTypeAndIsActiveTrue(ActionType actionType);
}
