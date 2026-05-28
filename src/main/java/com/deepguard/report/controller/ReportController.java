package com.deepguard.report.controller;

import com.deepguard.report.service.ReportPdfService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/reports")
@RequiredArgsConstructor
@Tag(name = "Report Export", description = "APIs for exporting scan detection reports as PDF")
public class ReportController {

    private final ReportPdfService reportPdfService;

    /**
     * GET /api/v1/reports/scan/{scanJobId}/pdf
     *
     * <p>Generates and downloads a PDF report for the specified scan job.
     * The PDF contains aggregated data from:
     * <ul>
     *   <li>user_profiles  – user display name</li>
     *   <li>scan_jobs      – job status, timestamps, error logs</li>
     *   <li>media_files    – file name, type, size, duration, upload info</li>
     *   <li>detection_results – fake score, confidence, label, model version</li>
     * </ul>
     *
     * @param scanJobId the UUID of the scan job
     * @return PDF file as application/pdf
     */
    @GetMapping("/scan/{scanJobId}/pdf")
    @PreAuthorize("isAuthenticated()")
    @Operation(
            summary = "Export scan report as PDF",
            description = "Generates a detailed PDF report for a specific scan job, including user info, " +
                    "scan job metadata, media file details, and AI detection results."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "PDF generated successfully",
                    content = @Content(mediaType = "application/pdf")),
            @ApiResponse(responseCode = "404", description = "Scan job not found"),
            @ApiResponse(responseCode = "401", description = "Authentication required"),
            @ApiResponse(responseCode = "500", description = "Failed to generate PDF report")
    })
    public ResponseEntity<byte[]> exportScanReportPdf(
            @Parameter(description = "ID of the scan job to export", required = true)
            @PathVariable String scanJobId) {

        byte[] pdfBytes = reportPdfService.generateScanReportPdf(scanJobId);

        String filename = "deepguard-scan-report-" + scanJobId + ".pdf";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("attachment", filename);
        headers.setContentLength(pdfBytes.length);

        return new ResponseEntity<>(pdfBytes, headers, HttpStatus.OK);
    }
}
