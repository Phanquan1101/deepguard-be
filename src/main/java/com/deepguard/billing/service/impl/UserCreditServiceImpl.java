package com.deepguard.billing.service.impl;

import com.deepguard.auth.entity.User;
import com.deepguard.billing.dto.response.UserCreditResponse;
import com.deepguard.billing.entity.CreditRule;
import com.deepguard.billing.entity.CreditTransaction;
import com.deepguard.billing.entity.UserCredit;
import com.deepguard.billing.enums.ActionType;
import com.deepguard.billing.enums.TransactionType;
import com.deepguard.billing.repository.CreditRuleRepository;
import com.deepguard.billing.repository.CreditTransactionRepository;
import com.deepguard.billing.repository.UserCreditRepository;
import com.deepguard.billing.service.UserCreditService;
import com.deepguard.common.exception.BusinessException;
import com.deepguard.common.exception.ErrorCode;
import com.deepguard.security.userdetails.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class UserCreditServiceImpl implements UserCreditService {

    private static final int WELCOME_BONUS_CREDITS = 25;

    private final UserCreditRepository userCreditRepository;
    private final CreditTransactionRepository creditTransactionRepository;
    private final CreditRuleRepository creditRuleRepository;

    @Override
    public void grantWelcomeBonus(User user) {
        if (userCreditRepository.existsByUser_Id(user.getId())) {
            return; // User already has credits, do not grant welcome bonus again
        }
        // Bonus credits for registration verification
        UserCredit userCredit = UserCredit.builder()
                .user(user)
                .remainingCredits(WELCOME_BONUS_CREDITS)
                .usedCredits(0)
                .createdAt(LocalDateTime.now())
                .updatedAt(null)
                .build();
        userCreditRepository.save(userCredit);

        // Transaction credits for registration verification
        CreditTransaction creditTransaction = CreditTransaction.builder()
                .userCredit(userCredit)
                .amount(WELCOME_BONUS_CREDITS)
                .transactionType(TransactionType.BONUS)
                .description("Bonus credits for email verification during registration")
                .build();
        creditTransactionRepository.save(creditTransaction);
    }

    @Scheduled(cron = "0 0 0 * * *", zone = "Asia/Ho_Chi_Minh")
    @Transactional
    public void refillDailyCredit() {
        List<UserCredit> userCredits = userCreditRepository.findUsersNeedRefill();
        for (UserCredit userCredit : userCredits) {

            int currentCredits = userCredit.getRemainingCredits();
            int refillCredits = 25;

            userCredit.setRemainingCredits(refillCredits);

            userCreditRepository.save(userCredit);

            CreditTransaction creditTransaction = CreditTransaction.builder()
                    .userCredit(userCredit)
                    .amount(refillCredits - currentCredits)
                    .transactionType(TransactionType.REFILL)
                    .description("Daily credit refill")
                    .build();

            creditTransactionRepository.save(creditTransaction);
        }
    }

    @Override
    public void validateEnoughCredits(User user, ActionType actionType) {

        UserCredit userCredit = userCreditRepository.findByUserId(user.getId());

        CreditRule rule = creditRuleRepository.findByActionTypeAndIsActiveTrue(actionType);

        if (userCredit.getRemainingCredits() < rule.getCreditCost()) {
            throw new BusinessException(ErrorCode.CREDIT_INSUFFICIENT);
        }
    }

    @Override
    public void consumeCredits(User user, ActionType actionType) {

        UserCredit userCredit = userCreditRepository.findByUserId(user.getId());

        CreditRule rule = creditRuleRepository.findByActionTypeAndIsActiveTrue(actionType);

        int cost = rule.getCreditCost();

        // DEDUCT
        userCredit.setRemainingCredits(userCredit.getRemainingCredits() - cost);
        userCredit.setUsedCredits(userCredit.getUsedCredits() + cost);
        userCreditRepository.save(userCredit);

        CreditTransaction creditTransaction = CreditTransaction.builder()
                .userCredit(userCredit)
                .amount(cost)
                .transactionType(TransactionType.USAGE)
                .description("Consumed credits for action: " + actionType.name())
                .build();
        creditTransactionRepository.save(creditTransaction);
    }

    @Override
    public void refundCredit(User user, ActionType actionType) {
        UserCredit userCredit = userCreditRepository.findByUserId(user.getId());

        CreditRule rule = creditRuleRepository.findByActionTypeAndIsActiveTrue(actionType);

        int refundAmount = rule.getCreditCost();

        // REFUND
        userCredit.setRemainingCredits(userCredit.getRemainingCredits() + refundAmount);

        userCredit.setUsedCredits(Math.max(0, userCredit.getUsedCredits() - refundAmount));

        userCreditRepository.save(userCredit);

        // SAVE TRANSACTION
        CreditTransaction transaction = CreditTransaction.builder()
                .userCredit(userCredit)
                .amount(refundAmount)
                .transactionType(TransactionType.REFUND)
                .description("Refund credits for failed " + actionType.name())
                .build();

        creditTransactionRepository.save(transaction);
    }

    private User getCurrentAuthenticatedUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof CustomUserDetails principal)) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        return principal.getUser();
    }

    @Override
    public UserCreditResponse getMyCredit() {
        User user = getCurrentAuthenticatedUser();

        UserCredit userCredit = userCreditRepository.findByUserId(user.getId());
        if (userCredit == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
        return UserCreditResponse.builder()
                .userId(user.getId())
                .remainingCredits(userCredit.getRemainingCredits())
                .usedCredits(userCredit.getUsedCredits())
                .build();
    }
}
