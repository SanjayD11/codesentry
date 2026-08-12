package com.sanjay.aisecurity.service;

import com.sanjay.aisecurity.dto.ExportRequest;
import com.sanjay.aisecurity.entity.*;
import com.sanjay.aisecurity.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
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

    @Transactional(readOnly = true)
    public byte[] generateExport(ExportRequest request) throws Exception {
        if ("excel".equalsIgnoreCase(request.getFormat())) {
            return generateExcel(request.getDatasets());
        } else if ("csv".equalsIgnoreCase(request.getFormat())) {
            return generateCsv(request.getDatasets());
        } else {
            // Fallback to CSV for PDF/others for MVP to avoid complex PDF layout generation
            return generateCsv(request.getDatasets());
        }
    }

    private byte[] generateExcel(List<String> datasets) throws Exception {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            
            if (datasets.contains("users")) {
                Sheet sheet = workbook.createSheet("Users");
                Row headerRow = sheet.createRow(0);
                String[] headers = {"User ID", "Name", "Email", "Role", "Status", "Registered Date", "Last Login"};
                for (int i = 0; i < headers.length; i++) headerRow.createCell(i).setCellValue(headers[i]);
                
                List<User> users = userRepository.findAll();
                int rowIdx = 1;
                for (User u : users) {
                    Row row = sheet.createRow(rowIdx++);
                    row.createCell(0).setCellValue(u.getId());
                    row.createCell(1).setCellValue(u.getFirstName() + " " + u.getLastName());
                    row.createCell(2).setCellValue(u.getEmail());
                    row.createCell(3).setCellValue(u.getRole() != null ? u.getRole().name() : "");
                    row.createCell(4).setCellValue(u.isActive() ? "Active" : "Suspended");
                    row.createCell(5).setCellValue(u.getCreatedAt() != null ? u.getCreatedAt().toString() : "");
                    row.createCell(6).setCellValue(u.getLastLogin() != null ? u.getLastLogin().toString() : "");
                }
            }

            if (datasets.contains("projects")) {
                Sheet sheet = workbook.createSheet("Projects");
                Row headerRow = sheet.createRow(0);
                String[] headers = {"Project Name", "Owner", "Created Date", "Last Scan", "Security Score", "Status"};
                for (int i = 0; i < headers.length; i++) headerRow.createCell(i).setCellValue(headers[i]);

                List<Project> projects = projectRepository.findAll();
                int rowIdx = 1;
                for (Project p : projects) {
                    Row row = sheet.createRow(rowIdx++);
                    row.createCell(0).setCellValue(p.getName());
                    row.createCell(1).setCellValue(p.getUser() != null ? p.getUser().getEmail() : "");
                    row.createCell(2).setCellValue(p.getCreatedAt() != null ? p.getCreatedAt().toString() : "");
                    row.createCell(3).setCellValue(p.getLastScan() != null ? p.getLastScan().toString() : "");
                    row.createCell(4).setCellValue(p.getSecurityScore());
                    row.createCell(5).setCellValue(p.getStatus());
                }
            }
            
            if (datasets.contains("auditLogs")) {
                Sheet sheet = workbook.createSheet("Audit Logs");
                Row headerRow = sheet.createRow(0);
                String[] headers = {"Timestamp", "Action", "Resource", "Details"};
                for (int i = 0; i < headers.length; i++) headerRow.createCell(i).setCellValue(headers[i]);

                List<AuditLog> logs = auditLogRepository.findAll();
                int rowIdx = 1;
                for (AuditLog l : logs) {
                    Row row = sheet.createRow(rowIdx++);
                    row.createCell(0).setCellValue(l.getCreatedAt() != null ? l.getCreatedAt().toString() : "");
                    row.createCell(1).setCellValue(l.getAction());
                    row.createCell(2).setCellValue(l.getResource());
                    row.createCell(3).setCellValue(l.getDetails());
                }
            }

            workbook.write(out);
            return out.toByteArray();
        }
    }

    private byte[] generateCsv(List<String> datasets) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (PrintWriter writer = new PrintWriter(out)) {
            if (datasets.contains("users")) {
                writer.println("--- USERS ---");
                writer.println("User ID,Name,Email,Role,Status,Registered Date,Last Login");
                userRepository.findAll().forEach(u -> {
                    writer.printf("%d,%s %s,%s,%s,%s,%s,%s%n",
                            u.getId(), u.getFirstName(), u.getLastName(), u.getEmail(),
                            u.getRole() != null ? u.getRole().name() : "",
                            u.isActive() ? "Active" : "Suspended",
                            u.getCreatedAt(), u.getLastLogin());
                });
                writer.println();
            }

            if (datasets.contains("projects")) {
                writer.println("--- PROJECTS ---");
                writer.println("Project Name,Owner,Created Date,Last Scan,Security Score,Status");
                projectRepository.findAll().forEach(p -> {
                    writer.printf("%s,%s,%s,%s,%.2f,%s%n",
                            p.getName(), p.getUser() != null ? p.getUser().getEmail() : "",
                            p.getCreatedAt(), p.getLastScan(), p.getSecurityScore(), p.getStatus());
                });
                writer.println();
            }
            
            if (datasets.contains("auditLogs")) {
                writer.println("--- AUDIT LOGS ---");
                writer.println("Timestamp,Action,Resource,Details");
                auditLogRepository.findAll().forEach(l -> {
                    writer.printf("%s,%s,%s,%s%n",
                            l.getCreatedAt(), l.getAction(), l.getResource(), l.getDetails().replace(",", ";"));
                });
            }
            writer.flush();
        }
        return out.toByteArray();
    }
}
