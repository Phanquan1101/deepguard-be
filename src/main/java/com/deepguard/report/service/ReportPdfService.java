package com.deepguard.report.service;

/**
 * Service for generating PDF reports for scan jobs.
 */
public interface ReportPdfService {

    /**
     * Generate a PDF report for the given scan job.
     * Fetches data from: user_profiles, scan_jobs, media_files, detection_results.
     *
     * @param scanJobId the ID of the scan job to generate a report for
     * @return raw PDF bytes
     */
    byte[] generateScanReportPdf(String scanJobId);
}
