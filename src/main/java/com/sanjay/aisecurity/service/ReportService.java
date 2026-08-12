package com.sanjay.aisecurity.service;

import com.sanjay.aisecurity.dto.response.ReportResponse;
import org.springframework.core.io.Resource;

import java.util.List;

/**
 * Service interface for PDF security report generation and retrieval.
 *
 * @author Sanjay
 * @version 1.0.0
 */
public interface ReportService {

    /**
     * Generates a PDF security report for a completed scan and persists the metadata.
     *
     * @param scanId the scan history ID
     * @return the report metadata response
     */
    ReportResponse generateReport(Long scanId);

    /**
     * Downloads the physical PDF file for a given report.
     *
     * @param reportId the report ID
     * @return the PDF file as a Spring Resource
     */
    Resource downloadReport(Long reportId);

    /**
     * Lists all reports visible to the authenticated user.
     *
     * @return list of report responses
     */
    List<ReportResponse> listMyReports();

    /**
     * Retrieves all reports for a specific project.
     *
     * @param projectId the project ID
     * @return list of report responses
     */
    List<ReportResponse> getProjectReports(Long projectId);

    /**
     * Deletes a report (metadata + physical file).
     *
     * @param reportId the report ID
     */
    void deleteReport(Long reportId);
}
