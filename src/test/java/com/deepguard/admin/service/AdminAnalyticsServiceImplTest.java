package com.deepguard.admin.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.deepguard.admin.dto.AdminAnalyticsResponse;
import com.deepguard.billing.enums.PaymentStatus;
import com.deepguard.billing.repository.PaymentRepository;
import com.deepguard.media.repository.MediaFileRepository;
import com.deepguard.scan.enums.ScanJobStatus;
import com.deepguard.scan.repository.ScanJobRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;

class AdminAnalyticsServiceImplTest {

    private final ScanJobRepository scanJobRepository = mock(ScanJobRepository.class);
    private final MediaFileRepository mediaFileRepository = mock(MediaFileRepository.class);
    private final PaymentRepository paymentRepository = mock(PaymentRepository.class);
    private final AdminAnalyticsServiceImpl analyticsService = new AdminAnalyticsServiceImpl(
            scanJobRepository,
            mediaFileRepository,
            paymentRepository
    );

    @Test
    void returnsCompactAggregateAndReusesTheShortLivedCache() {
        LocalDate today = LocalDate.now();
        PaymentRepository.DailyRevenueProjection todayRevenue = new DailyRevenueProjectionStub(
                today,
                new BigDecimal("99.00"),
                2L
        );
        when(scanJobRepository.count()).thenReturn(12L);
        when(mediaFileRepository.count()).thenReturn(9L);
        when(mediaFileRepository.countByFileTypeForAdminAnalytics())
                .thenReturn(List.<Object[]>of(new Object[]{"IMAGE", 7L}, new Object[]{"VIDEO", 2L}));
        when(scanJobRepository.countByStatusForAdminAnalytics()).thenReturn(List.<Object[]>of(
                new Object[]{ScanJobStatus.COMPLETED, 10L},
                new Object[]{ScanJobStatus.FAILED, 2L}
        ));
        when(paymentRepository.count()).thenReturn(4L);
        when(paymentRepository.sumAmountByStatus(PaymentStatus.SUCCESS))
                .thenReturn(new BigDecimal("99.00"));
        when(paymentRepository.countByStatusForAdminAnalytics()).thenReturn(List.<Object[]>of(
                new Object[]{PaymentStatus.SUCCESS, 2L},
                new Object[]{PaymentStatus.PENDING, 1L},
                new Object[]{PaymentStatus.FAILED, 1L}
        ));
        when(paymentRepository.countByPaymentMethodForAdminAnalytics())
                .thenReturn(List.<Object[]>of(new Object[]{"BANK_TRANSFER", 4L}));
        when(paymentRepository.countByPricingPlanForAdminAnalytics())
                .thenReturn(List.<Object[]>of(new Object[]{"Premium", 4L}));
        when(paymentRepository.findDailyRevenueSince(any(LocalDateTime.class)))
                .thenReturn(List.of(todayRevenue));

        AdminAnalyticsResponse first = analyticsService.getAnalytics();
        AdminAnalyticsResponse second = analyticsService.getAnalytics();

        assertThat(first).isSameAs(second);
        assertThat(first.totalScanJobs()).isEqualTo(12L);
        assertThat(first.totalMediaFiles()).isEqualTo(9L);
        assertThat(first.mediaTypeCounts()).containsEntry("IMAGE", 7L);
        assertThat(first.scanJobStatusCounts())
                .containsEntry(ScanJobStatus.COMPLETED.name(), 10L)
                .containsEntry(ScanJobStatus.FAILED.name(), 2L)
                .containsEntry(ScanJobStatus.QUEUED.name(), 0L);
        assertThat(first.paymentStatusCounts())
                .containsEntry(PaymentStatus.SUCCESS.name(), 2L)
                .containsEntry(PaymentStatus.REFUNDED.name(), 0L);
        assertThat(first.dailyRevenue()).hasSize(7);
        assertThat(first.dailyRevenue().getLast().revenue())
                .isEqualByComparingTo(new BigDecimal("99.00"));
        verify(scanJobRepository, times(1)).countByStatusForAdminAnalytics();
        verify(paymentRepository, times(1)).findDailyRevenueSince(any(LocalDateTime.class));
    }

    private record DailyRevenueProjectionStub(
            LocalDate summaryDate,
            BigDecimal revenue,
            long transactions
    ) implements PaymentRepository.DailyRevenueProjection {
        @Override
        public LocalDate getSummaryDate() {
            return summaryDate;
        }

        @Override
        public BigDecimal getRevenue() {
            return revenue;
        }

        @Override
        public long getTransactions() {
            return transactions;
        }
    }
}
