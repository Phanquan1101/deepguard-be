package com.deepguard.billing.repository;

import com.deepguard.auth.entity.User;
import com.deepguard.billing.entity.UserCredit;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface UserCreditRepository extends JpaRepository<UserCredit, String> {

    Optional<UserCredit> findByUser_Id(String userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<UserCredit> findByUser_IdForUpdate(String userId);

    boolean existsByUser_Id(String userId);

    @Query("""
    SELECT uc
    FROM UserCredit uc
    WHERE uc.remainingCredits < 25
""")
    List<UserCredit> findUsersNeedRefill();

    UserCredit findByUserId(String userId);
}
