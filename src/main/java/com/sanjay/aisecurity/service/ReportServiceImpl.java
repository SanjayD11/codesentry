package com.sanjay.aisecurity.service;

import com.lowagie.text.Chunk;
import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.sanjay.aisecurity.constants.MessageConstants;
import com.sanjay.aisecurity.dto.response.ReportResponse;
import com.sanjay.aisecurity.entity.Project;
import com.sanjay.aisecurity.entity.Report;
import com.sanjay.aisecurity.entity.ScanHistory;
import com.sanjay.aisecurity.entity.Vulnerability;
import com.sanjay.aisecurity.enums.Severity;
import com.sanjay.aisecurity.exception.ResourceNotFoundException;
import com.sanjay.aisecurity.repository.ReportRepository;
import com.sanjay.aisecurity.repository.ScanHistoryRepository;
import com.sanjay.aisecurity.repository.VulnerabilityRepository;
import com.sanjay.aisecurity.util.SecurityUtils;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.awt.Color;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * PDF Report Generator using OpenPDF (librepdf fork of iText2).
 *
 * @author Sanjay
 * @version 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReportServiceImpl implements ReportService {

    private static final DateTimeFormatter DT_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final ScanHistoryRepository scanHistoryRepository;
    private final VulnerabilityRepository vulnerabilityRepository;
    private final ReportRepository reportRepository;

    @Value("${REPORT_DIR:reports}")
    private String baseDir;
    
    private final java.util.concurrent.ConcurrentMap<Long, java.util.concurrent.locks.ReentrantLock> reportGenerationLocks = new java.util.concurrent.ConcurrentHashMap<>();

    @Override
    @Transactional
    public ReportResponse generateReport(Long scanId) {
        java.util.concurrent.locks.ReentrantLock lock = reportGenerationLocks.computeIfAbsent(scanId, k -> new java.util.concurrent.locks.ReentrantLock());
        if (!lock.tryLock()) {
            throw new com.sanjay.aisecurity.exception.DuplicateRequestException("Report generation is already in progress for this scan.");
        }
        
        try {
            String email = SecurityUtils.requireCurrentUserEmail();
            ScanHistory scan = resolveOwnedScan(scanId, email);
            Project project = scan.getProject();

            // Idempotent generation: check if report already exists for this scan
            Optional<Report> existingReportOpt = reportRepository.findFirstByScanHistoryId(scanId);
            if (existingReportOpt.isPresent()) {
                Report existingReport = existingReportOpt.get();
                if (java.nio.file.Files.exists(Paths.get(existingReport.getReportPath()))) {
                    log.info("Report already exists for scan {}. Returning existing report.", scanId);
                    return toResponse(existingReport, project);
                } else {
                    log.warn("Report record exists but file is missing for scan {}. Regenerating...", scanId);
                    reportRepository.delete(existingReport);
                }
            }

            List<Vulnerability> vulnerabilities = vulnerabilityRepository.findByScanHistoryId(scanId);
            // Create directory
            Path reportDir = Paths.get(baseDir,
                    String.valueOf(project.getUser().getId()),
                    String.valueOf(project.getId()));
            Files.createDirectories(reportDir);

            String fileName = "security-report-scan-" + scanId + "-"
                    + System.currentTimeMillis() + ".pdf";
            Path reportPath = reportDir.resolve(fileName);

            buildPdf(reportPath, scan, project, vulnerabilities, email);

            long fileSize = Files.size(reportPath);

            Report report = Report.builder()
                    .reportName(fileName)
                    .reportPath(reportPath.toString())
                    .reportType("PDF")
                    .reportSize(fileSize)
                    .generatedAt(LocalDateTime.now())
                    .scanHistoryId(scanId)
                    .generatedBy(email)
                    .project(project)
                    .build();

            report = reportRepository.save(report);
            log.info("Generated PDF report ID {} for scan {}", report.getId(), scanId);
            return toResponse(report, project);

        } catch (Exception e) {
            log.error("Failed to generate PDF report for scan {}: {}", scanId, e.getMessage(), e);
            throw new RuntimeException("PDF generation failed: " + e.getMessage(), e);
        } finally {
            lock.unlock();
            reportGenerationLocks.remove(scanId);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Resource downloadReport(Long reportId) {
        String email = SecurityUtils.requireCurrentUserEmail();
        Report report = resolveOwnedReport(reportId, email);

        try {
            Path path = Paths.get(report.getReportPath()).normalize();
            Resource resource = new UrlResource(path.toUri());
            if (!resource.exists() || !resource.isReadable()) {
                throw new ResourceNotFoundException("Report file not found on disk.");
            }
            return resource;
        } catch (Exception e) {
            throw new RuntimeException("Could not read report file.", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReportResponse> listMyReports() {
        String email = SecurityUtils.requireCurrentUserEmail();
        return reportRepository.findByProjectUserEmail(email,
                        org.springframework.data.domain.Pageable.unpaged())
                .stream()
                .map(r -> toResponse(r, r.getProject()))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReportResponse> getProjectReports(Long projectId) {
        return reportRepository.findByProjectId(projectId)
                .stream()
                .map(r -> toResponse(r, r.getProject()))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void deleteReport(Long reportId) {
        String email = SecurityUtils.requireCurrentUserEmail();
        Report report = resolveOwnedReport(reportId, email);

        try {
            Files.deleteIfExists(Paths.get(report.getReportPath()));
        } catch (Exception e) {
            log.warn("Could not delete report file: {}", report.getReportPath());
        }
        reportRepository.delete(report);
        log.info("Deleted report ID {}", reportId);
    }

    // =========================================================================
    // PDF GENERATION (OpenPDF)
    // =========================================================================

    private void buildPdf(Path outputPath, ScanHistory scan, Project project,
                          List<Vulnerability> vulns, String generatedBy) throws Exception {

        Document doc = new Document(PageSize.A4, 40, 40, 60, 40);
        PdfWriter.getInstance(doc, new FileOutputStream(outputPath.toFile()));
        doc.open();

        // Fonts
        Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 20, Color.WHITE);
        Font h1Font = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14, new Color(30, 60, 114));
        Font h2Font = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11, Color.DARK_GRAY);
        Font bodyFont = FontFactory.getFont(FontFactory.HELVETICA, 10, Color.BLACK);
        Font smallFont = FontFactory.getFont(FontFactory.HELVETICA, 8, Color.GRAY);
        Font whiteSmall = FontFactory.getFont(FontFactory.HELVETICA, 9, Color.WHITE);

        // ===== HEADER BANNER =====
        PdfPTable header = new PdfPTable(1);
        header.setWidthPercentage(100);
        PdfPCell headerCell = new PdfPCell(new Phrase("AI Security Analysis Platform — Security Report", titleFont));
        headerCell.setBackgroundColor(new Color(30, 60, 114));
        headerCell.setPaddingTop(20f);
        headerCell.setPaddingBottom(20f);
        headerCell.setHorizontalAlignment(Element.ALIGN_CENTER);
        headerCell.setBorder(Rectangle.NO_BORDER);
        header.addCell(headerCell);
        header.setSpacingAfter(20f);
        doc.add(header);

        // ===== PROJECT INFO =====
        Paragraph infoTitle = new Paragraph("Project Information", h1Font);
        infoTitle.setSpacingAfter(10f);
        doc.add(infoTitle);
        
        addInfoTable(doc, bodyFont, new String[][]{
                {"Project Name", project.getName()},
                {"Project Type", project.getProjectType().name()},
                {"Security Score", String.format("%.1f / 100", scan.getSecurityScore())},
                {"Scan ID", String.valueOf(scan.getId())},
                {"Scan Started", format(scan.getScanStart())},
                {"Scan Completed", format(scan.getScanEnd())},
                {"Total Files Scanned", String.valueOf(scan.getScannedFiles())},
                {"Total Vulnerabilities", String.valueOf(scan.getTotalVulnerabilities())},
                {"Report Generated By", generatedBy},
                {"Report Date", LocalDateTime.now().format(DT_FORMAT)}
        });

        // ===== SUMMARY BY SEVERITY =====
        Paragraph summaryTitle = new Paragraph("Vulnerability Summary", h1Font);
        summaryTitle.setSpacingBefore(20f);
        summaryTitle.setSpacingAfter(10f);
        doc.add(summaryTitle);
        PdfPTable summaryTable = new PdfPTable(2);
        summaryTable.setWidthPercentage(60);
        summaryTable.setHorizontalAlignment(Element.ALIGN_LEFT);
        summaryTable.setSpacingAfter(20f);

        for (Severity sev : Arrays.asList(Severity.CRITICAL, Severity.HIGH, Severity.MEDIUM, Severity.LOW)) {
            long count = vulns.stream().filter(v -> v.getSeverity() == sev).count();
            PdfPCell sevCell = new PdfPCell(new Phrase(sev.name(), whiteSmall));
            sevCell.setBackgroundColor(severityColor(sev));
            sevCell.setPadding(8f);
            sevCell.setBorder(Rectangle.NO_BORDER);
            sevCell.setVerticalAlignment(Element.ALIGN_MIDDLE);

            PdfPCell countCell = new PdfPCell(new Phrase(String.valueOf(count), bodyFont));
            countCell.setPadding(8f);
            countCell.setBorder(Rectangle.BOX);
            countCell.setBorderColor(Color.LIGHT_GRAY);
            countCell.setHorizontalAlignment(Element.ALIGN_CENTER);
            countCell.setVerticalAlignment(Element.ALIGN_MIDDLE);

            summaryTable.addCell(sevCell);
            summaryTable.addCell(countCell);
        }
        doc.add(summaryTable);

        // ===== VULNERABILITY DETAILS =====
        if (vulns.isEmpty()) {
            Paragraph noVuln = new Paragraph("No vulnerabilities detected in this scan.", bodyFont);
            noVuln.setSpacingBefore(10f);
            doc.add(noVuln);
        } else {
            Paragraph detailsTitle = new Paragraph("Vulnerability Details", h1Font);
            detailsTitle.setSpacingBefore(15f);
            detailsTitle.setSpacingAfter(15f);
            doc.add(detailsTitle);

            int index = 1;
            for (Severity sev : Arrays.asList(Severity.CRITICAL, Severity.HIGH, Severity.MEDIUM, Severity.LOW)) {
                List<Vulnerability> group = vulns.stream()
                        .filter(v -> v.getSeverity() == sev)
                        .collect(Collectors.toList());
                if (group.isEmpty()) continue;

                // Group header
                PdfPTable grpHeader = new PdfPTable(1);
                grpHeader.setWidthPercentage(100);
                grpHeader.setSpacingBefore(15f);
                grpHeader.setSpacingAfter(10f);
                PdfPCell grpCell = new PdfPCell(new Phrase(" " + sev.name() + " SEVERITY (" + group.size() + ")", whiteSmall));
                grpCell.setBackgroundColor(severityColor(sev));
                grpCell.setPadding(8f);
                grpCell.setBorder(Rectangle.NO_BORDER);
                grpHeader.addCell(grpCell);
                doc.add(grpHeader);

                for (Vulnerability v : group) {
                    Paragraph vulnTitle = new Paragraph(index + ". " + v.getVulnerabilityType(), h2Font);
                    vulnTitle.setSpacingBefore(15f);
                    vulnTitle.setSpacingAfter(8f);
                    doc.add(vulnTitle);

                    addInfoTable(doc, bodyFont, new String[][]{
                            {"File", v.getFileName()},
                            {"Line", String.valueOf(v.getLineNumber())},
                            {"Confidence", String.format("%.0f%%", v.getConfidenceScore() * 100)}
                    });

                    if (v.getCodeSnippet() != null) {
                        Font codeFont = FontFactory.getFont(FontFactory.COURIER, 8, Color.BLACK);
                        PdfPTable codeTable = new PdfPTable(1);
                        codeTable.setWidthPercentage(100);
                        codeTable.setSpacingBefore(8f);
                        codeTable.setSpacingAfter(8f);
                        PdfPCell codeCell = new PdfPCell(new Phrase(v.getCodeSnippet(), codeFont));
                        codeCell.setBackgroundColor(new Color(245, 245, 245));
                        codeCell.setPadding(10f);
                        codeCell.setBorder(Rectangle.BOX);
                        codeCell.setBorderColor(Color.LIGHT_GRAY);
                        codeTable.addCell(codeCell);
                        doc.add(codeTable);
                    }

                    Paragraph desc = new Paragraph("Description: " + v.getDescription(), bodyFont);
                    desc.setSpacingBefore(8f);
                    desc.setSpacingAfter(4f);
                    doc.add(desc);

                    Paragraph rec = new Paragraph("Recommendation: " + v.getRecommendation(), bodyFont);
                    rec.setSpacingAfter(10f);
                    doc.add(rec);

                    if (v.getAiExplanation() != null) {
                        Paragraph exp = new Paragraph("AI Analysis (Explanation):\n" + cleanAiText(v.getAiExplanation()), smallFont);
                        exp.setSpacingAfter(8f);
                        doc.add(exp);
                    }
                    if (v.getAiRecommendation() != null) {
                        Paragraph aiRec = new Paragraph("AI Analysis (Root Cause & Fix):\n" + cleanAiText(v.getAiRecommendation()), smallFont);
                        aiRec.setSpacingAfter(8f);
                        doc.add(aiRec);
                    }
                    if (v.getBusinessImpact() != null) {
                        Paragraph biz = new Paragraph("AI Analysis (Business Impact):\n" + cleanAiText(v.getBusinessImpact()), smallFont);
                        biz.setSpacingAfter(8f);
                        doc.add(biz);
                    }
                    if (v.getSecureCodeExample() != null) {
                        Font codeFont = FontFactory.getFont(FontFactory.COURIER, 8, new Color(0, 100, 0));
                        PdfPTable fixTable = new PdfPTable(1);
                        fixTable.setWidthPercentage(100);
                        fixTable.setSpacingBefore(8f);
                        fixTable.setSpacingAfter(15f);
                        PdfPCell fixCell = new PdfPCell(new Phrase("Secure Fix:\n" + cleanAiText(v.getSecureCodeExample()), codeFont));
                        fixCell.setBackgroundColor(new Color(240, 255, 240));
                        fixCell.setPadding(10f);
                        fixCell.setBorder(Rectangle.BOX);
                        fixCell.setBorderColor(Color.LIGHT_GRAY);
                        fixTable.addCell(fixCell);
                        doc.add(fixTable);
                    }
                    index++;
                }
            }
        }

        // ===== FOOTER =====
        Paragraph footer = new Paragraph("Generated by AI Security Analysis Platform | Confidential", smallFont);
        footer.setAlignment(Element.ALIGN_CENTER);
        footer.setSpacingBefore(30f);
        doc.add(footer);
        doc.close();
    }

    private void addInfoTable(Document doc, Font font, String[][] rows) throws Exception {
        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{30f, 70f});
        table.setSpacingBefore(5f);
        table.setSpacingAfter(5f);

        for (String[] row : rows) {
            Font labelFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, Color.DARK_GRAY);
            PdfPCell label = new PdfPCell(new Phrase(row[0], labelFont));
            label.setBackgroundColor(new Color(240, 240, 240));
            label.setPadding(8f);
            label.setBorder(Rectangle.BOX);
            label.setBorderColor(Color.LIGHT_GRAY);

            PdfPCell value = new PdfPCell(new Phrase(row[1] != null ? row[1] : "-", font));
            value.setPadding(8f);
            value.setBorder(Rectangle.BOX);
            value.setBorderColor(Color.LIGHT_GRAY);

            table.addCell(label);
            table.addCell(value);
        }
        doc.add(table);
    }

    private Color severityColor(Severity sev) {
        return switch (sev) {
            case CRITICAL -> new Color(200, 0, 0);
            case HIGH -> new Color(230, 80, 0);
            case MEDIUM -> new Color(200, 140, 0);
            case LOW -> new Color(60, 130, 60);
            case INFORMATIONAL -> new Color(100, 149, 237); // Cornflower blue
        };
    }

    /**
     * Cleans AI-generated text for inclusion in the PDF report.
     * Removes chain-of-thought blocks, JSON/markdown fences, internal
     * instruction text, and normalises excessive whitespace.
     * Does NOT strip legitimate security content.
     */
    private String cleanAiText(String raw) {
        if (raw == null) return "";
        String s = raw;
        // 1. Strip <think>...</think> reasoning blocks (including truncated)
        s = s.replaceAll("(?si)<think>.*?(?:</think>|$)", "");
        // 2. Strip ```json ... ``` or ``` ... ``` fences
        s = s.replaceAll("(?s)```(?:json|java|xml|yaml|bash|python|javascript|typescript)?\\s*", "").replace("```", "");
        // 3. Remove known internal instruction leakage
        s = s.replaceAll("(?i)CRITICAL INSTRUCTION \\d+:.*?(\\n|$)", "");
        // 4. Remove raw JSON curly-brace wrappers that leaked out
        s = s.replaceAll("^\\s*\\{\\s*\"|\"\\s*\\}\\s*$", "");
        // 5. Replace markdown ## headings with plain uppercase labels for PDF readability
        s = s.replaceAll("(?m)^##+ ", "");
        // 6. Collapse 3+ blank lines to 2
        s = s.replaceAll("\\n{3,}", "\n\n");
        return s.trim();
    }

    private String format(LocalDateTime dt) {
        return dt != null ? dt.format(DT_FORMAT) : "-";
    }

    // =========================================================================
    // SECURITY HELPERS
    // =========================================================================

    private ScanHistory resolveOwnedScan(Long scanId, String email) {
        ScanHistory scan = scanHistoryRepository.findById(scanId)
                .orElseThrow(() -> new ResourceNotFoundException(MessageConstants.SCAN_NOT_FOUND));
        if (!scan.getProject().getUser().getEmail().equals(email)) {
            throw new org.springframework.security.access.AccessDeniedException("Access Denied: You do not own this scan.");
        }
        return scan;
    }

    private Report resolveOwnedReport(Long reportId, String email) {
        Report report = reportRepository.findById(reportId)
                .orElseThrow(() -> new ResourceNotFoundException(MessageConstants.REPORT_NOT_FOUND));
                
        if (!report.getProject().getUser().getEmail().equals(email)) {
            throw new org.springframework.security.access.AccessDeniedException("Access Denied: You do not own this report.");
        }
        return report;
    }

    private ReportResponse toResponse(Report report, Project project) {
        Double securityScore = null;
        Integer totalVulnerabilities = null;
        if (report.getScanHistoryId() != null) {
            ScanHistory scan = scanHistoryRepository.findById(report.getScanHistoryId()).orElse(null);
            if (scan != null) {
                securityScore = scan.getSecurityScore();
                totalVulnerabilities = scan.getTotalVulnerabilities();
            }
        }
        return ReportResponse.builder()
                .id(report.getId())
                .projectId(project != null ? project.getId() : (report.getProject() != null ? report.getProject().getId() : null))
                .projectName(project != null ? project.getName() : (report.getProject() != null ? report.getProject().getName() : null))
                .scanHistoryId(report.getScanHistoryId())
                .reportName(report.getReportName())
                .reportType(report.getReportType())
                .reportSizeBytes(report.getReportSize())
                .securityScore(securityScore)
                .totalVulnerabilities(totalVulnerabilities)
                .generatedAt(report.getGeneratedAt())
                .generatedBy(report.getGeneratedBy())
                .downloadUrl("/api/v1/reports/" + report.getId() + "/download")
                .build();
    }
}
