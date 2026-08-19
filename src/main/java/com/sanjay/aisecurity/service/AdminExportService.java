package com.sanjay.aisecurity.service;

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
import com.sanjay.aisecurity.dto.ExportRequest;
import com.sanjay.aisecurity.entity.*;
import com.sanjay.aisecurity.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminExportService {

    private final UserRepository userRepository;
    private final ProjectRepository projectRepository;
    private final ScanHistoryRepository scanHistoryRepository;
    private final ReportRepository reportRepository;
    private final VulnerabilityRepository vulnerabilityRepository;
    private final AuditLogRepository auditLogRepository;

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Transactional(readOnly = true)
    public byte[] generateExport(ExportRequest request) throws Exception {
        String fmt = request.getFormat() != null ? request.getFormat().toLowerCase() : "csv";
        List<String> datasets = request.getDatasets() != null ? request.getDatasets() : List.of();

        log.info("Generating platform data export for format='{}', datasets={}", fmt, datasets);

        if ("excel".equalsIgnoreCase(fmt) || "xlsx".equalsIgnoreCase(fmt)) {
            return generateExcel(datasets);
        } else if ("pdf".equalsIgnoreCase(fmt)) {
            return generatePdf(datasets);
        } else {
            return generateCsv(datasets);
        }
    }

    // =========================================================================
    // EXCEL EXPORT (.xlsx)
    // =========================================================================

    private byte[] generateExcel(List<String> datasets) throws Exception {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            // Header Style
            CellStyle headerStyle = workbook.createCellStyle();
            org.apache.poi.ss.usermodel.Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerFont.setColor(IndexedColors.WHITE.getIndex());
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerStyle.setAlignment(HorizontalAlignment.LEFT);

            // 1. Users
            if (hasDataset(datasets, "users", "userDirectory")) {
                Sheet sheet = workbook.createSheet("User Directory");
                String[] headers = {"User ID", "First Name", "Last Name", "Email", "Role", "Status", "Registered Date", "Last Login"};
                createExcelHeader(sheet, headerStyle, headers);

                List<User> users = userRepository.findAll();
                int rowIdx = 1;
                for (User u : users) {
                    Row row = sheet.createRow(rowIdx++);
                    row.createCell(0).setCellValue(u.getId());
                    row.createCell(1).setCellValue(safe(u.getFirstName()));
                    row.createCell(2).setCellValue(safe(u.getLastName()));
                    row.createCell(3).setCellValue(safe(u.getEmail()));
                    row.createCell(4).setCellValue(u.getRole() != null ? u.getRole().name() : "USER");
                    row.createCell(5).setCellValue(u.isActive() ? "Active" : "Suspended");
                    row.createCell(6).setCellValue(formatDate(u.getCreatedAt()));
                    row.createCell(7).setCellValue(formatDate(u.getLastLogin()));
                }
                autoSizeSheet(sheet, headers.length);
            }

            // 2. Projects
            if (hasDataset(datasets, "projects", "projectMetadata")) {
                Sheet sheet = workbook.createSheet("Projects");
                String[] headers = {"Project ID", "Project Name", "Description", "Owner Email", "Type", "Security Score", "Total Files", "Status", "Last Scan", "Created Date"};
                createExcelHeader(sheet, headerStyle, headers);

                List<Project> projects = projectRepository.findAll();
                int rowIdx = 1;
                for (Project p : projects) {
                    Row row = sheet.createRow(rowIdx++);
                    row.createCell(0).setCellValue(p.getId());
                    row.createCell(1).setCellValue(safe(p.getName()));
                    row.createCell(2).setCellValue(safe(p.getDescription()));
                    row.createCell(3).setCellValue(p.getUser() != null ? safe(p.getUser().getEmail()) : "Unassigned");
                    row.createCell(4).setCellValue(p.getProjectType() != null ? p.getProjectType().name() : "OTHER");
                    row.createCell(5).setCellValue(p.getSecurityScore());
                    row.createCell(6).setCellValue(p.getTotalFiles());
                    row.createCell(7).setCellValue(safe(p.getStatus()));
                    row.createCell(8).setCellValue(formatDate(p.getLastScan()));
                    row.createCell(9).setCellValue(formatDate(p.getCreatedAt()));
                }
                autoSizeSheet(sheet, headers.length);
            }

            // 3. Scans
            if (hasDataset(datasets, "scans", "scanHistory")) {
                Sheet sheet = workbook.createSheet("Scan History");
                String[] headers = {"Scan ID", "Project ID", "Scan Type", "Status", "Security Score", "Vulnerabilities", "Scanned Files", "Duration (s)", "Start Time", "End Time"};
                createExcelHeader(sheet, headerStyle, headers);

                List<ScanHistory> scans = scanHistoryRepository.findAll();
                int rowIdx = 1;
                for (ScanHistory s : scans) {
                    Row row = sheet.createRow(rowIdx++);
                    row.createCell(0).setCellValue(s.getId());
                    row.createCell(1).setCellValue(s.getProject() != null ? s.getProject().getId() : 0);
                    row.createCell(2).setCellValue(s.getScanType() != null ? s.getScanType().name() : "PROJECT");
                    row.createCell(3).setCellValue(s.getStatus() != null ? s.getStatus().name() : "PENDING");
                    row.createCell(4).setCellValue(s.getSecurityScore());
                    row.createCell(5).setCellValue(s.getTotalVulnerabilities());
                    row.createCell(6).setCellValue(s.getScannedFiles());
                    row.createCell(7).setCellValue(s.getDuration() / 1000.0);
                    row.createCell(8).setCellValue(formatDate(s.getScanStart()));
                    row.createCell(9).setCellValue(formatDate(s.getScanEnd()));
                }
                autoSizeSheet(sheet, headers.length);
            }

            // 4. Audit Logs
            if (hasDataset(datasets, "auditLogs", "audit_logs")) {
                Sheet sheet = workbook.createSheet("Audit Logs");
                String[] headers = {"Log ID", "Timestamp", "User Email", "Action", "Resource", "Status", "Details", "IP Address"};
                createExcelHeader(sheet, headerStyle, headers);

                List<AuditLog> logs = auditLogRepository.findAll();
                int rowIdx = 1;
                for (AuditLog l : logs) {
                    Row row = sheet.createRow(rowIdx++);
                    row.createCell(0).setCellValue(l.getId());
                    row.createCell(1).setCellValue(formatDate(l.getCreatedAt()));
                    row.createCell(2).setCellValue(l.getUser() != null ? safe(l.getUser().getEmail()) : "System");
                    row.createCell(3).setCellValue(safe(l.getAction()));
                    row.createCell(4).setCellValue(safe(l.getResource()));
                    row.createCell(5).setCellValue(safe(l.getStatus()));
                    row.createCell(6).setCellValue(safe(l.getDetails()));
                    row.createCell(7).setCellValue(safe(l.getIpAddress()));
                }
                autoSizeSheet(sheet, headers.length);
            }

            // 5. Reports
            if (hasDataset(datasets, "reports", "aiReports")) {
                Sheet sheet = workbook.createSheet("AI Reports");
                String[] headers = {"Report ID", "Report Name", "Project Name", "Type", "Size (KB)", "Generated By", "Generated At"};
                createExcelHeader(sheet, headerStyle, headers);

                List<Report> reports = reportRepository.findAll();
                int rowIdx = 1;
                for (Report r : reports) {
                    Row row = sheet.createRow(rowIdx++);
                    row.createCell(0).setCellValue(r.getId());
                    row.createCell(1).setCellValue(safe(r.getReportName()));
                    row.createCell(2).setCellValue(r.getProject() != null ? safe(r.getProject().getName()) : "Unknown");
                    row.createCell(3).setCellValue(safe(r.getReportType()));
                    row.createCell(4).setCellValue(r.getReportSize() / 1024.0);
                    row.createCell(5).setCellValue(safe(r.getGeneratedBy()));
                    row.createCell(6).setCellValue(formatDate(r.getGeneratedAt()));
                }
                autoSizeSheet(sheet, headers.length);
            }

            // 6. Vulnerabilities (if requested)
            if (hasDataset(datasets, "vulnerabilities")) {
                Sheet sheet = workbook.createSheet("Vulnerabilities");
                String[] headers = {"Vuln ID", "Scan ID", "Type", "Severity", "File Name", "Line Number", "OWASP Category", "CWE ID", "Description"};
                createExcelHeader(sheet, headerStyle, headers);

                List<Vulnerability> vulns = vulnerabilityRepository.findAll();
                int rowIdx = 1;
                for (Vulnerability v : vulns) {
                    Row row = sheet.createRow(rowIdx++);
                    row.createCell(0).setCellValue(v.getId());
                    row.createCell(1).setCellValue(v.getScanHistory() != null ? v.getScanHistory().getId() : 0);
                    row.createCell(2).setCellValue(safe(v.getVulnerabilityType()));
                    row.createCell(3).setCellValue(v.getSeverity() != null ? v.getSeverity().name() : "LOW");
                    row.createCell(4).setCellValue(safe(v.getFileName()));
                    row.createCell(5).setCellValue(v.getLineNumber());
                    row.createCell(6).setCellValue(safe(v.getOwaspCategory()));
                    row.createCell(7).setCellValue(safe(v.getCweId()));
                    row.createCell(8).setCellValue(safe(v.getDescription()));
                }
                autoSizeSheet(sheet, headers.length);
            }

            workbook.write(out);
            return out.toByteArray();
        }
    }

    private void createExcelHeader(Sheet sheet, CellStyle style, String[] headers) {
        Row headerRow = sheet.createRow(0);
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(style);
        }
    }

    private void autoSizeSheet(Sheet sheet, int numCols) {
        for (int i = 0; i < numCols; i++) {
            sheet.autoSizeColumn(i);
            if (sheet.getColumnWidth(i) > 12000) {
                sheet.setColumnWidth(i, 12000);
            }
        }
    }

    // =========================================================================
    // CSV EXPORT (.csv)
    // =========================================================================

    private byte[] generateCsv(List<String> datasets) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (PrintWriter writer = new PrintWriter(out, true, StandardCharsets.UTF_8)) {

            // 1. Users
            if (hasDataset(datasets, "users", "userDirectory")) {
                writer.println("=== USER DIRECTORY ===");
                writer.println("User ID,First Name,Last Name,Email,Role,Status,Registered Date,Last Login");
                userRepository.findAll().forEach(u -> {
                    writer.printf("%d,%s,%s,%s,%s,%s,%s,%s%n",
                            u.getId(), escapeCsv(u.getFirstName()), escapeCsv(u.getLastName()), escapeCsv(u.getEmail()),
                            u.getRole() != null ? u.getRole().name() : "USER",
                            u.isActive() ? "Active" : "Suspended",
                            formatDate(u.getCreatedAt()), formatDate(u.getLastLogin()));
                });
                writer.println();
            }

            // 2. Projects
            if (hasDataset(datasets, "projects", "projectMetadata")) {
                writer.println("=== PROJECTS METADATA ===");
                writer.println("Project ID,Project Name,Owner Email,Type,Security Score,Total Files,Status,Last Scan,Created Date");
                projectRepository.findAll().forEach(p -> {
                    writer.printf("%d,%s,%s,%s,%.2f,%d,%s,%s,%s%n",
                            p.getId(), escapeCsv(p.getName()),
                            p.getUser() != null ? escapeCsv(p.getUser().getEmail()) : "Unassigned",
                            p.getProjectType() != null ? p.getProjectType().name() : "OTHER",
                            p.getSecurityScore(), p.getTotalFiles(), escapeCsv(p.getStatus()),
                            formatDate(p.getLastScan()), formatDate(p.getCreatedAt()));
                });
                writer.println();
            }

            // 3. Scans
            if (hasDataset(datasets, "scans", "scanHistory")) {
                writer.println("=== SCAN HISTORY ===");
                writer.println("Scan ID,Project ID,Type,Status,Security Score,Vulnerabilities,Scanned Files,Duration (s),Start Time,End Time");
                scanHistoryRepository.findAll().forEach(s -> {
                    writer.printf("%d,%d,%s,%s,%.2f,%d,%d,%.2f,%s,%s%n",
                            s.getId(), s.getProject() != null ? s.getProject().getId() : 0,
                            s.getScanType() != null ? s.getScanType().name() : "PROJECT",
                            s.getStatus() != null ? s.getStatus().name() : "PENDING",
                            s.getSecurityScore(), s.getTotalVulnerabilities(), s.getScannedFiles(),
                            s.getDuration() / 1000.0, formatDate(s.getScanStart()), formatDate(s.getScanEnd()));
                });
                writer.println();
            }

            // 4. Audit Logs
            if (hasDataset(datasets, "auditLogs", "audit_logs")) {
                writer.println("=== AUDIT LOGS ===");
                writer.println("Log ID,Timestamp,User Email,Action,Resource,Status,Details,IP Address");
                auditLogRepository.findAll().forEach(l -> {
                    writer.printf("%d,%s,%s,%s,%s,%s,%s,%s%n",
                            l.getId(), formatDate(l.getCreatedAt()),
                            l.getUser() != null ? escapeCsv(l.getUser().getEmail()) : "System",
                            escapeCsv(l.getAction()), escapeCsv(l.getResource()), escapeCsv(l.getStatus()),
                            escapeCsv(l.getDetails()), escapeCsv(l.getIpAddress()));
                });
                writer.println();
            }

            // 5. Reports
            if (hasDataset(datasets, "reports", "aiReports")) {
                writer.println("=== AI REPORTS ===");
                writer.println("Report ID,Report Name,Project Name,Type,Size (KB),Generated By,Generated At");
                reportRepository.findAll().forEach(r -> {
                    writer.printf("%d,%s,%s,%s,%.2f,%s,%s%n",
                            r.getId(), escapeCsv(r.getReportName()),
                            r.getProject() != null ? escapeCsv(r.getProject().getName()) : "Unknown",
                            escapeCsv(r.getReportType()), r.getReportSize() / 1024.0,
                            escapeCsv(r.getGeneratedBy()), formatDate(r.getGeneratedAt()));
                });
                writer.println();
            }

            // 6. Vulnerabilities
            if (hasDataset(datasets, "vulnerabilities")) {
                writer.println("=== VULNERABILITIES ===");
                writer.println("Vuln ID,Scan ID,Type,Severity,File Name,Line Number,OWASP Category,CWE ID,Description");
                vulnerabilityRepository.findAll().forEach(v -> {
                    writer.printf("%d,%d,%s,%s,%s,%d,%s,%s,%s%n",
                            v.getId(), v.getScanHistory() != null ? v.getScanHistory().getId() : 0,
                            escapeCsv(v.getVulnerabilityType()),
                            v.getSeverity() != null ? v.getSeverity().name() : "LOW",
                            escapeCsv(v.getFileName()), v.getLineNumber(),
                            escapeCsv(v.getOwaspCategory()), escapeCsv(v.getCweId()),
                            escapeCsv(v.getDescription()));
                });
                writer.println();
            }

            writer.flush();
        }
        return out.toByteArray();
    }

    // =========================================================================
    // PDF EXPORT (.pdf)
    // =========================================================================

    private byte[] generatePdf(List<String> datasets) throws Exception {
        Document doc = new Document(PageSize.A4.rotate(), 30, 30, 40, 30);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PdfWriter.getInstance(doc, out);
        doc.open();

        Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18, Color.WHITE);
        Font subTitleFont = FontFactory.getFont(FontFactory.HELVETICA, 10, new Color(220, 225, 235));
        Font h2Font = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 13, new Color(30, 60, 114));
        Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, Color.WHITE);
        Font cellFont = FontFactory.getFont(FontFactory.HELVETICA, 8, Color.DARK_GRAY);

        // Header Banner
        PdfPTable header = new PdfPTable(1);
        header.setWidthPercentage(100);
        PdfPCell cell = new PdfPCell();
        cell.setBackgroundColor(new Color(30, 60, 114));
        cell.setPadding(14f);
        cell.setBorder(Rectangle.NO_BORDER);
        cell.addElement(new Paragraph("CodeSentry Enterprise Platform Data Export", titleFont));
        cell.addElement(new Paragraph("Generated on: " + LocalDateTime.now().format(DATE_FMT) + " | Status: Official Audit Report", subTitleFont));
        header.addCell(cell);
        header.setSpacingAfter(15f);
        doc.add(header);

        // 1. Users Table
        if (hasDataset(datasets, "users", "userDirectory")) {
            doc.add(new Paragraph("User Directory", h2Font));
            doc.add(new Paragraph(" ", FontFactory.getFont(FontFactory.HELVETICA, 4)));

            String[] headers = {"User ID", "Name", "Email", "Role", "Status", "Registered", "Last Login"};
            float[] widths = {10f, 20f, 25f, 12f, 10f, 18f, 18f};
            PdfPTable table = createPdfTable(headers, widths, headerFont);

            List<User> users = userRepository.findAll();
            for (int i = 0; i < users.size(); i++) {
                User u = users.get(i);
                Color bg = i % 2 == 0 ? Color.WHITE : new Color(248, 250, 252);
                addPdfCell(table, String.valueOf(u.getId()), cellFont, bg);
                addPdfCell(table, safe(u.getFirstName()) + " " + safe(u.getLastName()), cellFont, bg);
                addPdfCell(table, safe(u.getEmail()), cellFont, bg);
                addPdfCell(table, u.getRole() != null ? u.getRole().name() : "USER", cellFont, bg);
                addPdfCell(table, u.isActive() ? "Active" : "Suspended", cellFont, bg);
                addPdfCell(table, formatDate(u.getCreatedAt()), cellFont, bg);
                addPdfCell(table, formatDate(u.getLastLogin()), cellFont, bg);
            }
            table.setSpacingAfter(15f);
            doc.add(table);
        }

        // 2. Projects Table
        if (hasDataset(datasets, "projects", "projectMetadata")) {
            doc.add(new Paragraph("Projects Metadata", h2Font));
            doc.add(new Paragraph(" ", FontFactory.getFont(FontFactory.HELVETICA, 4)));

            String[] headers = {"ID", "Project Name", "Owner Email", "Type", "Score", "Files", "Status", "Last Scan"};
            float[] widths = {8f, 24f, 26f, 12f, 10f, 8f, 10f, 18f};
            PdfPTable table = createPdfTable(headers, widths, headerFont);

            List<Project> projects = projectRepository.findAll();
            for (int i = 0; i < projects.size(); i++) {
                Project p = projects.get(i);
                Color bg = i % 2 == 0 ? Color.WHITE : new Color(248, 250, 252);
                addPdfCell(table, String.valueOf(p.getId()), cellFont, bg);
                addPdfCell(table, safe(p.getName()), cellFont, bg);
                addPdfCell(table, p.getUser() != null ? safe(p.getUser().getEmail()) : "Unassigned", cellFont, bg);
                addPdfCell(table, p.getProjectType() != null ? p.getProjectType().name() : "OTHER", cellFont, bg);
                addPdfCell(table, String.format("%.1f", p.getSecurityScore()), cellFont, bg);
                addPdfCell(table, String.valueOf(p.getTotalFiles()), cellFont, bg);
                addPdfCell(table, safe(p.getStatus()), cellFont, bg);
                addPdfCell(table, formatDate(p.getLastScan()), cellFont, bg);
            }
            table.setSpacingAfter(15f);
            doc.add(table);
        }

        // 3. Scan History Table
        if (hasDataset(datasets, "scans", "scanHistory")) {
            doc.add(new Paragraph("Scan History", h2Font));
            doc.add(new Paragraph(" ", FontFactory.getFont(FontFactory.HELVETICA, 4)));

            String[] headers = {"Scan ID", "Project ID", "Scan Type", "Status", "Score", "Vulns", "Duration", "Started At"};
            float[] widths = {10f, 12f, 16f, 14f, 10f, 10f, 12f, 18f};
            PdfPTable table = createPdfTable(headers, widths, headerFont);

            List<ScanHistory> scans = scanHistoryRepository.findAll();
            for (int i = 0; i < scans.size(); i++) {
                ScanHistory s = scans.get(i);
                Color bg = i % 2 == 0 ? Color.WHITE : new Color(248, 250, 252);
                addPdfCell(table, String.valueOf(s.getId()), cellFont, bg);
                addPdfCell(table, s.getProject() != null ? String.valueOf(s.getProject().getId()) : "-", cellFont, bg);
                addPdfCell(table, s.getScanType() != null ? s.getScanType().name() : "PROJECT", cellFont, bg);
                addPdfCell(table, s.getStatus() != null ? s.getStatus().name() : "PENDING", cellFont, bg);
                addPdfCell(table, String.format("%.1f", s.getSecurityScore()), cellFont, bg);
                addPdfCell(table, String.valueOf(s.getTotalVulnerabilities()), cellFont, bg);
                addPdfCell(table, String.format("%.1fs", s.getDuration() / 1000.0), cellFont, bg);
                addPdfCell(table, formatDate(s.getScanStart()), cellFont, bg);
            }
            table.setSpacingAfter(15f);
            doc.add(table);
        }

        // 4. Audit Logs Table
        if (hasDataset(datasets, "auditLogs", "audit_logs")) {
            doc.add(new Paragraph("Audit Logs", h2Font));
            doc.add(new Paragraph(" ", FontFactory.getFont(FontFactory.HELVETICA, 4)));

            String[] headers = {"ID", "Timestamp", "User Email", "Action", "Resource", "Status", "IP Address"};
            float[] widths = {8f, 18f, 24f, 18f, 16f, 10f, 14f};
            PdfPTable table = createPdfTable(headers, widths, headerFont);

            List<AuditLog> logs = auditLogRepository.findAll();
            for (int i = 0; i < logs.size(); i++) {
                AuditLog l = logs.get(i);
                Color bg = i % 2 == 0 ? Color.WHITE : new Color(248, 250, 252);
                addPdfCell(table, String.valueOf(l.getId()), cellFont, bg);
                addPdfCell(table, formatDate(l.getCreatedAt()), cellFont, bg);
                addPdfCell(table, l.getUser() != null ? safe(l.getUser().getEmail()) : "System", cellFont, bg);
                addPdfCell(table, safe(l.getAction()), cellFont, bg);
                addPdfCell(table, safe(l.getResource()), cellFont, bg);
                addPdfCell(table, safe(l.getStatus()), cellFont, bg);
                addPdfCell(table, safe(l.getIpAddress()), cellFont, bg);
            }
            table.setSpacingAfter(15f);
            doc.add(table);
        }

        // 5. Reports Table
        if (hasDataset(datasets, "reports", "aiReports")) {
            doc.add(new Paragraph("AI Reports Metadata", h2Font));
            doc.add(new Paragraph(" ", FontFactory.getFont(FontFactory.HELVETICA, 4)));

            String[] headers = {"Report ID", "Report Name", "Project Name", "Type", "Size", "Generated By", "Generated At"};
            float[] widths = {10f, 26f, 22f, 10f, 10f, 18f, 18f};
            PdfPTable table = createPdfTable(headers, widths, headerFont);

            List<Report> reports = reportRepository.findAll();
            for (int i = 0; i < reports.size(); i++) {
                Report r = reports.get(i);
                Color bg = i % 2 == 0 ? Color.WHITE : new Color(248, 250, 252);
                addPdfCell(table, String.valueOf(r.getId()), cellFont, bg);
                addPdfCell(table, safe(r.getReportName()), cellFont, bg);
                addPdfCell(table, r.getProject() != null ? safe(r.getProject().getName()) : "Unknown", cellFont, bg);
                addPdfCell(table, safe(r.getReportType()), cellFont, bg);
                addPdfCell(table, String.format("%.1f KB", r.getReportSize() / 1024.0), cellFont, bg);
                addPdfCell(table, safe(r.getGeneratedBy()), cellFont, bg);
                addPdfCell(table, formatDate(r.getGeneratedAt()), cellFont, bg);
            }
            table.setSpacingAfter(15f);
            doc.add(table);
        }

        doc.close();
        return out.toByteArray();
    }

    private PdfPTable createPdfTable(String[] headers, float[] widths, Font headerFont) throws Exception {
        PdfPTable table = new PdfPTable(headers.length);
        table.setWidthPercentage(100);
        table.setWidths(widths);

        for (String h : headers) {
            PdfPCell c = new PdfPCell(new Phrase(h, headerFont));
            c.setBackgroundColor(new Color(30, 60, 114));
            c.setPadding(6f);
            c.setHorizontalAlignment(Element.ALIGN_LEFT);
            table.addCell(c);
        }
        return table;
    }

    private void addPdfCell(PdfPTable table, String text, Font font, Color bgColor) {
        PdfPCell c = new PdfPCell(new Phrase(text, font));
        c.setBackgroundColor(bgColor);
        c.setPadding(5f);
        c.setHorizontalAlignment(Element.ALIGN_LEFT);
        table.addCell(c);
    }

    // =========================================================================
    // UTILITY HELPERS
    // =========================================================================

    private boolean hasDataset(List<String> datasets, String... keys) {
        if (datasets == null || datasets.isEmpty()) return true;
        for (String k : keys) {
            if (datasets.stream().anyMatch(d -> d.equalsIgnoreCase(k))) {
                return true;
            }
        }
        return false;
    }

    private String safe(String str) {
        return str != null ? str : "";
    }

    private String escapeCsv(String str) {
        if (str == null) return "";
        String escaped = str.replace("\"", "\"\"");
        if (escaped.contains(",") || escaped.contains("\"") || escaped.contains("\n")) {
            return "\"" + escaped + "\"";
        }
        return escaped;
    }

    private String formatDate(LocalDateTime date) {
        return date != null ? date.format(DATE_FMT) : "N/A";
    }
}
