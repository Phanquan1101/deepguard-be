package com.deepguard.billing.service;

import com.deepguard.billing.dto.response.CurrentSubscriptionResponse;

public interface SubscriptionService {
    CurrentSubscriptionResponse getMyCurrentSubscription(String userId);
}
