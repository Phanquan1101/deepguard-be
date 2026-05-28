package com.deepguard.report.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Aggregated data for a single scan report PDF export.
 * Combines data from: user_profiles, scan_jobs, media_files, detection_results.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScanReportData {

    // ── User info ──────────────────────────────────────────────────────────────
    private String userEmail;
    private String userFullName;

    // ── Scan Job info ──────────────────────────────────────────────────────────
    private String scanJobId;
    private String scanJobStatus;
    private LocalDateTime scanStartedAt;
    private LocalDateTime scanFinishedAt;
    private String scanErrorLoggings;

    // ── Media File info ────────────────────────────────────────────────────────
    private String mediaFileId;
    private String mediaFileName;
    private String mediaOriginalUrl;
    private String mediaFileType;
    private Long mediaFileSizeBytes;
    private Integer mediaDurationSeconds;
    private String mediaUploadStatus;
    private LocalDateTime mediaUploadedAt;

    // ── Detection Result info ──────────────────────────────────────────────────
    private String detectionResultId;
    private BigDecimal fakeScore;
    private BigDecimal confidence;
    private String resultLabel;
    private String modelVersion;
    private LocalDateTime processedAt;
}
