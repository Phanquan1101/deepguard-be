package com.deepguard.admin.service;

import com.deepguard.admin.dto.AdminAnalyticsResponse;
import com.deepguard.billing.enums.PaymentStatus;
import com.deepguard.billing.repository.PaymentRepository;
import com.deepguard.media.repository.MediaFileRepository;
import com.deepguard.scan.enums.ScanJobStatus;
import com.deepguard.scan.repository.ScanJobRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AdminAnalyticsServiceImpl implements AdminAnalyticsService {

    private static final long CACHE_TTL_MILLIS = 30_000L;

    private final ScanJobRepository scanJobRepository;
    private final MediaFileRepository mediaFileRepository;
    private final PaymentRepository paymentRepository;

    private volatile CachedAnalytics cachedAnalytics;

    @Override
    @Transactional(readOnly = true)
    public AdminAnalyticsResponse getAnalytics() {
        CachedAnalytics current = cachedAnalytics;
        if (current != null && current.expiresAtMillis() > System.currentTimeMillis()) {
            return current.response();
        }

        synchronized (this) {
            current = cachedAnalytics;
            if (current != null && current.expiresAtMillis() > System.currentTimeMillis()) {
                return current.response();
            }

            AdminAnalyticsResponse response = buildAnalytics();
            cachedAnalytics = new CachedAnalytics(
                    response,
                    System.currentTimeMillis() + CACHE_TTL_MILLIS
            );
            return response;
        }
    }

    private AdminAnalyticsResponse buildAnalytics() {
        Map<String, Long> scanStatusCounts = countByEnum(
                ScanJobStatus.class,
                scanJobRepository.countByStatusForAdminAnalytics()
        );
        Map<String, Long> paymentStatusCounts = countByEnum(
                PaymentStatus.class,
                paymentRepository.countByStatusForAdminAnalytics()
        );

        LocalDate firstDay = LocalDate.now().minusDays(6);
        Map<LocalDate, PaymentRepository.DailyRevenueProjection> revenueByDate = new LinkedHashMap<>();
        for (PaymentRepository.DailyRevenueProjection row : paymentRepository.findDailyRevenueSince(
                firstDay.atStartOfDay())) {
            revenueByDate.put(row.getSummaryDate(), row);
        }

        List<AdminAnalyticsResponse.DailyRevenue> dailyRevenue = new ArrayList<>();
        for (int offset = 0; offset < 7; offset++) {
            LocalDate date = firstDay.plusDays(offset);
            PaymentRepository.DailyRevenueProjection row = revenueByDate.get(date);
            dailyRevenue.add(new AdminAnalyticsResponse.DailyRevenue(
                    date,
                    row != null && row.getRevenue() != null ? row.getRevenue() : BigDecimal.ZERO,
                    row != null ? row.getTransactions() : 0L
            ));
        }

        BigDecimal totalRevenue = paymentRepository.sumAmountByStatus(PaymentStatus.SUCCESS);
        return new AdminAnalyticsResponse(
                scanJobRepository.count(),
                mediaFileRepository.count(),
                toCountMap(mediaFileRepository.countByFileTypeForAdminAnalytics()),
                scanStatusCounts,
                paymentRepository.count(),
                totalRevenue != null ? totalRevenue : BigDecimal.ZERO,
                toCountMap(paymentRepository.countByPaymentMethodForAdminAnalytics()),
                paymentStatusCounts,
                toCountMap(paymentRepository.countByPricingPlanForAdminAnalytics()),
                dailyRevenue
        );
    }

    private <E extends Enum<E>> Map<String, Long> countByEnum(Class<E> enumType, List<Object[]> rows) {
        Map<String, Long> counts = new LinkedHashMap<>();
        EnumSet.allOf(enumType).forEach(value -> counts.put(value.name(), 0L));
        rows.forEach(row -> {
            if (row[0] instanceof Enum<?> value) {
                counts.put(value.name(), ((Number) row[1]).longValue());
            }
        });
        return counts;
    }

    private Map<String, Long> toCountMap(List<Object[]> rows) {
        Map<String, Long> counts = new LinkedHashMap<>();
        rows.forEach(row -> {
            String key = row[0] != null ? String.valueOf(row[0]) : "Unknown";
            counts.put(key, ((Number) row[1]).longValue());
        });
        return counts;
    }

    private record CachedAnalytics(AdminAnalyticsResponse response, long expiresAtMillis) {
    }
}
