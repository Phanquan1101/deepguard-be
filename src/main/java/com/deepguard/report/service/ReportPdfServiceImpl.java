package com.deepguard.report.service;

import com.deepguard.common.exception.BusinessException;
import com.deepguard.common.exception.ErrorCode;
import com.deepguard.report.dto.ScanReportData;
import com.deepguard.scan.entity.DetectionResult;
import com.deepguard.scan.entity.ScanJob;
import com.deepguard.scan.repository.DetectionResultRepository;
import com.deepguard.scan.repository.ScanJobRepository;
import com.deepguard.user.entity.UserProfile;
import com.deepguard.user.repository.UserProfileRepository;
import com.itextpdf.io.font.constants.StandardFonts;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.events.Event;
import com.itextpdf.kernel.events.IEventHandler;
import com.itextpdf.kernel.events.PdfDocumentEvent;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.geom.Rectangle;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfPage;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.kernel.pdf.canvas.PdfCanvas;
import com.itextpdf.layout.Canvas;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.borders.Border;
import com.itextpdf.layout.borders.SolidBorder;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.LineSeparator;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import com.itextpdf.layout.properties.VerticalAlignment;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ReportPdfServiceImpl implements ReportPdfService {

    private final ScanJobRepository scanJobRepository;
    private final DetectionResultRepository detectionResultRepository;
    private final UserProfileRepository userProfileRepository;

    // ── Palette ────────────────────────────────────────────────────────────────
    private static final DeviceRgb C_NAVY     = new DeviceRgb(15,  23,  42);   // #0F172A
    private static final DeviceRgb C_INDIGO   = new DeviceRgb(67,  56, 202);   // #4338CA
    private static final DeviceRgb C_INDIGO_L = new DeviceRgb(99, 102, 241);   // #6366F1
    private static final DeviceRgb C_RED      = new DeviceRgb(220, 38,  38);   // #DC2626
    private static final DeviceRgb C_RED_L    = new DeviceRgb(254, 226, 226);  // #FEE2E2
    private static final DeviceRgb C_GREEN    = new DeviceRgb(22,  163,  74);  // #16A34A
    private static final DeviceRgb C_GREEN_L  = new DeviceRgb(220, 252, 231);  // #DCFCE7
    private static final DeviceRgb C_SLATE_50 = new DeviceRgb(248, 250, 252);
    private static final DeviceRgb C_SLATE_100= new DeviceRgb(241, 245, 249);
    private static final DeviceRgb C_SLATE_200= new DeviceRgb(226, 232, 240);
    private static final DeviceRgb C_SLATE_400= new DeviceRgb(148, 163, 184);
    private static final DeviceRgb C_SLATE_600= new DeviceRgb(71,  85, 105);
    private static final DeviceRgb C_SLATE_700= new DeviceRgb(51,  65,  85);
    private static final DeviceRgb C_WHITE    = new DeviceRgb(255, 255, 255);

    private static final DateTimeFormatter DATE_FMT  = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
    private static final DateTimeFormatter DATE_ONLY = DateTimeFormatter.ofPattern("dd MMMM yyyy");

    // ── Public API ─────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    @Override
    public byte[] generateScanReportPdf(String scanJobId) {
        ScanJob scanJob = scanJobRepository.findWithDetailsById(scanJobId)
                .orElseThrow(() -> new BusinessException(ErrorCode.SCAN_JOB_NOT_FOUND));

        Optional<DetectionResult> drOpt = detectionResultRepository.findByScanJobId(scanJobId);
        Optional<UserProfile>     upOpt = userProfileRepository.findByUserId(scanJob.getUser().getId());

        ScanReportData data = buildData(scanJob, drOpt.orElse(null), upOpt.orElse(null));
        try {
            return render(data);
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.REPORT_EXPORT_FAILED);
        }
    }

    // ── Data builder ───────────────────────────────────────────────────────────

    private ScanReportData buildData(ScanJob s, DetectionResult d, UserProfile p) {
        ScanReportData.ScanReportDataBuilder b = ScanReportData.builder()
                .userEmail(s.getUser().getEmail())
                .userFullName(p != null ? p.getFullName() : null)
                .scanJobId(s.getId())
                .scanJobStatus(s.getStatus().name())
                .scanStartedAt(s.getStartedAt())
                .scanFinishedAt(s.getFinishedAt())
                .scanErrorLoggings(s.getErrorLoggings())
                .mediaFileId(s.getMediaFile().getId())
                .mediaFileName(s.getMediaFile().getFileName())
                .mediaOriginalUrl(s.getMediaFile().getOriginalUrl())
                .mediaFileType(s.getMediaFile().getFileType().name())
                .mediaFileSizeBytes(s.getMediaFile().getFileSize())
                .mediaDurationSeconds(s.getMediaFile().getDuration())
                .mediaUploadStatus(s.getMediaFile().getUploadStatus().name())
                .mediaUploadedAt(s.getMediaFile().getUploadedAt());
        if (d != null) {
            b.detectionResultId(d.getId())
             .fakeScore(d.getFakeScore())
             .confidence(d.getConfidence())
             .resultLabel(d.getResultLabel().name())
             .modelVersion("DeepGuard Detection Engine")
             .processedAt(d.getProcessedAt());
        }
        return b.build();
    }

    // ── PDF Renderer ───────────────────────────────────────────────────────────

    private byte[] render(ScanReportData data) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PdfDocument pdf = new PdfDocument(new PdfWriter(baos));

        // Register footer/header event handler
        pdf.addEventHandler(PdfDocumentEvent.END_PAGE, new PageFooterHandler());

        Document doc = new Document(pdf, PageSize.A4);
        doc.setMargins(0, 36, 50, 36);

        PdfFont bold    = PdfFontFactory.createFont(StandardFonts.HELVETICA_BOLD);
        PdfFont normal  = PdfFontFactory.createFont(StandardFonts.HELVETICA);
        PdfFont oblique = PdfFontFactory.createFont(StandardFonts.HELVETICA_OBLIQUE);

        boolean isFake = "FAKE".equalsIgnoreCase(data.getResultLabel());

        // ── 1. Explicitly create page 1 so PdfCanvas can draw on it ───────────
        // iText 7 creates pages lazily; we must add the page before calling
        // pdf.getPage(1) in addCoverHeader, otherwise IndexOutOfBoundsException.
        pdf.addNewPage();
        addCoverHeader(doc, pdf, bold, normal, data, isFake);

        // ── 2. Executive summary card row ──────────────────────────────────────
        doc.add(summaryCards(data, isFake, bold, normal));

        // ── 3. Risk score bar ──────────────────────────────────────────────────
        if (data.getFakeScore() != null) {
            doc.add(riskBar(data.getFakeScore(), isFake, bold, normal));
        }

        addDivider();

        // ── 4. Sections ────────────────────────────────────────────────────────
        doc.add(divider());
        doc.add(sectionHeader("01  ANALYST INFORMATION", bold));
        doc.add(infoTable(new String[][]{
                {"Submitted By",  safe(data.getUserFullName())},
                {"Email Address", safe(data.getUserEmail())},
                {"Report Date",   LocalDateTime.now().format(DATE_ONLY)},
        }, normal, bold));

        doc.add(sectionHeader("02  SCAN JOB DETAILS", bold));
        doc.add(infoTable(new String[][]{
                {"Scan Job ID",  safe(data.getScanJobId())},
                {"Status",       badge(data.getScanJobStatus())},
                {"Started At",   fmt(data.getScanStartedAt())},
                {"Finished At",  fmt(data.getScanFinishedAt())},
                {"Error Logs",   safe(data.getScanErrorLoggings())},
        }, normal, bold));

        doc.add(sectionHeader("03  MEDIA FILE METADATA", bold));
        doc.add(infoTable(new String[][]{
                {"File Name",     safe(data.getMediaFileName())},
                {"File Type",     safe(data.getMediaFileType())},
                {"File Size",     bytes(data.getMediaFileSizeBytes())},
                {"Duration",      data.getMediaDurationSeconds() != null
                        ? data.getMediaDurationSeconds() + " seconds" : "—"},
                {"Upload Status", safe(data.getMediaUploadStatus())},
                {"Uploaded At",   fmt(data.getMediaUploadedAt())},
                {"Media ID",      safe(data.getMediaFileId())},
                {"Original URL",  safe(data.getMediaOriginalUrl())},
        }, normal, bold));

        doc.add(sectionHeader("04  AI DETECTION RESULT", bold));
        if (data.getDetectionResultId() != null) {
            doc.add(infoTable(new String[][]{
                    {"Result Label",  safe(data.getResultLabel())},
                    {"Fake Score",    pct(data.getFakeScore())},
                    {"Confidence",    pct(data.getConfidence())},
                    {"Model Version", safe(data.getModelVersion())},
                    {"Processed At",  fmt(data.getProcessedAt())},
                    {"Result ID",     safe(data.getDetectionResultId())},
            }, normal, bold));
        } else {
            doc.add(new Paragraph("No detection result is available. The scan job may have failed or is still processing.")
                    .setFont(oblique).setFontSize(10).setFontColor(C_SLATE_400)
                    .setMarginTop(6).setMarginBottom(16));
        }

        // ── 5. Disclaimer ──────────────────────────────────────────────────────
        doc.add(divider());
        doc.add(new Paragraph(
                "DISCLAIMER: This report is generated automatically by the DeepGuard AI detection system. " +
                "Results are based on machine learning analysis and should be reviewed by a qualified analyst " +
                "before any legal or editorial action is taken.")
                .setFont(oblique).setFontSize(8).setFontColor(C_SLATE_400)
                .setTextAlignment(TextAlignment.JUSTIFIED)
                .setMarginTop(8).setMarginBottom(4));

        doc.close();
        return baos.toByteArray();
    }

    // ── Cover Header ───────────────────────────────────────────────────────────

    private void addCoverHeader(Document doc, PdfDocument pdf,
                                PdfFont bold, PdfFont normal,
                                ScanReportData data, boolean isFake) {
        // Page 1 was pre-created by pdf.addNewPage() in render(), safe to access.
        PdfPage  firstPage = pdf.getPage(1);
        PdfCanvas canvas   = new PdfCanvas(firstPage);

        float pageWidth = PageSize.A4.getWidth();

        // Navy background
        canvas.setFillColor(C_NAVY)
              .rectangle(0, PageSize.A4.getHeight() - 130, pageWidth, 130)
              .fill();

        // Indigo accent stripe at bottom of banner
        canvas.setFillColor(C_INDIGO)
              .rectangle(0, PageSize.A4.getHeight() - 134, pageWidth, 4)
              .fill();

        // Left decorative bar
        canvas.setFillColor(C_INDIGO_L)
              .rectangle(0, PageSize.A4.getHeight() - 130, 5, 130)
              .fill();

        canvas.release();

        // Overlay text via layout canvas
        Rectangle headerRect = new Rectangle(36, PageSize.A4.getHeight() - 126, pageWidth - 72, 118);
        Canvas c = new Canvas(new PdfCanvas(firstPage), headerRect);
        c.setFont(bold);

        // "DEEPGUARD" brand
        c.add(new Paragraph("DEEPGUARD")
                .setFont(bold).setFontSize(11).setFontColor(C_INDIGO_L)
                .setCharacterSpacing(3).setMarginBottom(2));

        c.add(new Paragraph("AI Deepfake Detection Report")
                .setFont(bold).setFontSize(22).setFontColor(C_WHITE)
                .setMarginBottom(4));

        c.add(new Paragraph(
                "Scan ID: " + data.getScanJobId() + "     |     " +
                "Generated: " + LocalDateTime.now().format(DATE_FMT))
                .setFont(normal).setFontSize(8.5f).setFontColor(C_SLATE_400));
        c.close();

        // Push doc cursor below the banner
        doc.add(new Paragraph("").setMarginTop(140).setMarginBottom(16));
    }

    // ── Summary cards ──────────────────────────────────────────────────────────

    private Table summaryCards(ScanReportData data, boolean isFake,
                               PdfFont bold, PdfFont normal) {
        Table t = new Table(UnitValue.createPercentArray(new float[]{1, 1, 1}))
                .useAllAvailableWidth().setMarginBottom(18);

        DeviceRgb verdictBg   = isFake ? C_RED_L   : C_GREEN_L;
        DeviceRgb verdictColor= isFake ? C_RED      : C_GREEN;
        String    verdictTxt  = isFake ? "DEEPFAKE" : "AUTHENTIC";

        t.addCell(kpiCard("VERDICT",       verdictTxt,    verdictColor, verdictBg, bold, normal));
        t.addCell(kpiCard("FAKE SCORE",    pct(data.getFakeScore()), isFake ? C_RED : C_GREEN,
                          C_SLATE_50, bold, normal));
        t.addCell(kpiCard("CONFIDENCE",    pct(data.getConfidence()), C_INDIGO,
                          C_SLATE_50, bold, normal));
        return t;
    }

    private Cell kpiCard(String label, String value, DeviceRgb valueColor,
                         DeviceRgb bg, PdfFont bold, PdfFont normal) {
        return new Cell()
                .setBackgroundColor(bg)
                .setBorder(new SolidBorder(C_SLATE_200, 1))
                .setPadding(14)
                .setTextAlignment(TextAlignment.CENTER)
                .add(new Paragraph(label)
                        .setFont(normal).setFontSize(8)
                        .setFontColor(C_SLATE_600)
                        .setCharacterSpacing(1.5f)
                        .setMarginBottom(6))
                .add(new Paragraph(value)
                        .setFont(bold).setFontSize(20)
                        .setFontColor(valueColor));
    }

    // ── Risk bar ───────────────────────────────────────────────────────────────

    private Table riskBar(BigDecimal score, boolean isFake, PdfFont bold, PdfFont normal) {
        double pct = score.doubleValue() * 100.0;

        Table wrapper = new Table(UnitValue.createPercentArray(new float[]{1}))
                .useAllAvailableWidth()
                .setBorder(Border.NO_BORDER)
                .setMarginBottom(18);

        // Label row
        Table labelRow = new Table(UnitValue.createPercentArray(new float[]{1, 1}))
                .useAllAvailableWidth().setBorder(Border.NO_BORDER);
        labelRow.addCell(new Cell().setBorder(Border.NO_BORDER)
                .add(new Paragraph("RISK SCORE METER").setFont(bold).setFontSize(8)
                        .setFontColor(C_SLATE_600).setCharacterSpacing(1.5f)));
        labelRow.addCell(new Cell().setBorder(Border.NO_BORDER)
                .setTextAlignment(TextAlignment.RIGHT)
                .add(new Paragraph(String.format("%.1f%%", pct)).setFont(bold).setFontSize(8)
                        .setFontColor(isFake ? C_RED : C_GREEN)
                        .setTextAlignment(TextAlignment.RIGHT)));

        // Track + fill drawn as nested tables
        Table track = new Table(UnitValue.createPercentArray(new float[]{(float) pct, (float)(100 - pct)}))
                .useAllAvailableWidth().setBorder(Border.NO_BORDER).setHeight(12);
        track.addCell(new Cell().setBackgroundColor(isFake ? C_RED : C_GREEN)
                .setBorder(Border.NO_BORDER).setHeight(12));
        if (pct < 100) {
            track.addCell(new Cell().setBackgroundColor(C_SLATE_200)
                    .setBorder(Border.NO_BORDER).setHeight(12));
        }

        wrapper.addCell(new Cell().setBorder(Border.NO_BORDER).add(labelRow).setMarginBottom(4));
        wrapper.addCell(new Cell().setBorder(Border.NO_BORDER).add(track));
        return wrapper;
    }

    // ── Section header ─────────────────────────────────────────────────────────

    private Paragraph sectionHeader(String text, PdfFont bold) {
        return new Paragraph(text)
                .setFont(bold).setFontSize(9)
                .setFontColor(C_INDIGO)
                .setCharacterSpacing(1.5f)
                .setBorderLeft(new SolidBorder(C_INDIGO, 3))
                .setPaddingLeft(8)
                .setMarginTop(18).setMarginBottom(6);
    }

    private LineSeparator divider() {
        return new LineSeparator(
                new com.itextpdf.kernel.pdf.canvas.draw.SolidLine(0.5f))
                .setStrokeColor(C_SLATE_200)
                .setMarginTop(4).setMarginBottom(4);
    }

    private void addDivider() { /* spacer placeholder */ }

    // ── Info table ─────────────────────────────────────────────────────────────

    private Table infoTable(String[][] rows, PdfFont normal, PdfFont bold) {
        Table t = new Table(UnitValue.createPercentArray(new float[]{32, 68}))
                .useAllAvailableWidth().setMarginBottom(4)
                .setBorder(new SolidBorder(C_SLATE_200, 0.5f));

        for (int i = 0; i < rows.length; i++) {
            DeviceRgb bg = (i % 2 == 0) ? C_WHITE : C_SLATE_50;

            t.addCell(new Cell()
                    .setBackgroundColor(C_SLATE_100)
                    .setBorder(new SolidBorder(C_SLATE_200, 0.5f))
                    .setPaddingTop(6).setPaddingBottom(6).setPaddingLeft(10).setPaddingRight(6)
                    .add(new Paragraph(rows[i][0])
                            .setFont(bold).setFontSize(8.5f).setFontColor(C_SLATE_700)));

            t.addCell(new Cell()
                    .setBackgroundColor(bg)
                    .setBorder(new SolidBorder(C_SLATE_200, 0.5f))
                    .setPaddingTop(6).setPaddingBottom(6).setPaddingLeft(10).setPaddingRight(6)
                    .add(new Paragraph(rows[i][1])
                            .setFont(normal).setFontSize(8.5f).setFontColor(C_SLATE_700)));
        }
        return t;
    }

    // ── Page Footer Event Handler ──────────────────────────────────────────────

    private static class PageFooterHandler implements IEventHandler {
        private static final DeviceRgb BG    = new DeviceRgb(15, 23, 42);
        private static final DeviceRgb TEXT  = new DeviceRgb(148, 163, 184);
        private static final DeviceRgb ACCT  = new DeviceRgb(99, 102, 241);

        @Override
        public void handleEvent(Event event) {
            PdfDocumentEvent docEvent = (PdfDocumentEvent) event;
            PdfDocument pdf  = docEvent.getDocument();
            PdfPage     page = docEvent.getPage();
            int         num  = pdf.getPageNumber(page);
            float       w    = page.getPageSize().getWidth();

            PdfCanvas canvas = new PdfCanvas(page);
            // Footer bar
            canvas.setFillColor(BG)
                  .rectangle(0, 0, w, 36)
                  .fill();
            // Accent top line
            canvas.setFillColor(ACCT)
                  .rectangle(0, 36, w, 2)
                  .fill();
            canvas.release();

            // Footer text
            try {
                PdfFont f = PdfFontFactory.createFont(StandardFonts.HELVETICA);
                Rectangle r = new Rectangle(36, 8, w - 72, 22);
                Canvas c = new Canvas(new PdfCanvas(page), r);
                Table row = new Table(UnitValue.createPercentArray(new float[]{1, 1}))
                        .useAllAvailableWidth().setBorder(Border.NO_BORDER);
                row.addCell(new Cell().setBorder(Border.NO_BORDER)
                        .add(new Paragraph("DEEPGUARD  |  Confidential – AI Detection Report")
                                .setFont(f).setFontSize(7).setFontColor(TEXT)));
                row.addCell(new Cell().setBorder(Border.NO_BORDER).setTextAlignment(TextAlignment.RIGHT)
                        .add(new Paragraph("Page " + num)
                                .setFont(f).setFontSize(7).setFontColor(TEXT)
                                .setTextAlignment(TextAlignment.RIGHT)));
                c.add(row);
                c.close();
            } catch (IOException ignored) {}
        }
    }

    // ── Formatters ─────────────────────────────────────────────────────────────

    private String safe(String v) { return (v == null || v.isBlank()) ? "—" : v; }
    private String fmt(LocalDateTime dt) { return dt == null ? "—" : dt.format(DATE_FMT); }
    private String pct(BigDecimal v) {
        return v == null ? "—" : String.format("%.1f%%", v.multiply(BigDecimal.valueOf(100)));
    }
    private String badge(String status) { return status == null ? "—" : "[" + status + "]"; }
    private String bytes(Long b) {
        if (b == null) return "—";
        if (b < 1024) return b + " B";
        if (b < 1024 * 1024) return String.format("%.1f KB", b / 1024.0);
        if (b < 1024L * 1024 * 1024) return String.format("%.1f MB", b / (1024.0 * 1024));
        return String.format("%.1f GB", b / (1024.0 * 1024 * 1024));
    }
}
