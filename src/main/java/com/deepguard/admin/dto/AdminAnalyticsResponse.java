package com.deepguard.admin.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Compact aggregate payload for the admin analytics screen. It deliberately
 * contains no media, scan-job, or payment rows, so the dashboard does not
 * need to download large lists just to calculate counters and charts.
 */
public record AdminAnalyticsResponse(
        long totalScanJobs,
        long totalMediaFiles,
        Map<String, Long> scanJobStatusCounts,
        long totalTransactions,
        BigDecimal totalRevenue,
        Map<String, Long> paymentMethodCounts,
        Map<String, Long> paymentStatusCounts,
        Map<String, Long> pricingPlanCounts,
        List<DailyRevenue> dailyRevenue
) {
    public record DailyRevenue(LocalDate date, BigDecimal revenue, long transactions) {
    }
}
