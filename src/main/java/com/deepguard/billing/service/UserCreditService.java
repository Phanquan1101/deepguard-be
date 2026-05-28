package com.deepguard.billing.service;

import com.deepguard.auth.entity.User;
import com.deepguard.billing.dto.response.UserCreditResponse;
import com.deepguard.billing.enums.ActionType;

public interface UserCreditService {
    void grantWelcomeBonus(User user);
    void validateEnoughCredits(User user, ActionType actionType);
    void consumeCredits(User user, ActionType actionType);
    void refundCredit(User user, ActionType actionType);
    UserCreditResponse getMyCredit();
}
