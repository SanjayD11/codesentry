package com.sanjay.aisecurity.service;

import com.sanjay.aisecurity.dto.request.ScanConfigurationDto;
import com.sanjay.aisecurity.dto.request.TriggerScanRequest;
import com.sanjay.aisecurity.dto.response.ScanSummaryResponse;

import java.util.List;

/**
 * Service for triggering and managing static code analysis scans.
 *
 * @author Sanjay
 * @version 2.0.0
 */
public interface ScanService {

    /**
     * Initializes a new scan with default configuration.
     * Backward-compatible overload — delegates to {@link #triggerScan(Long, ScanConfigurationDto)}.
     *
     * @param projectId the project ID to scan
     * @return the newly created Scan ID
     */
    default Long triggerScan(Long projectId) {
        return triggerScan(projectId, ScanConfigurationDto.defaults());
    }

    /**
     * Initializes a new scan with the provided configuration, validates it,
     * persists the config snapshot to ScanHistory, and returns the scan ID.
     *
     * @param projectId     the project ID to scan
     * @param configuration the validated scan configuration
     * @return the newly created Scan ID
     */
    Long triggerScan(Long projectId, ScanConfigurationDto configuration);

    /**
     * Executes the background scan pipeline with default configuration.
     * Backward-compatible overload.
     *
     * @param scanId    the scan ID
     * @param projectId the project ID
     */
    default void executeScanAsync(Long scanId, Long projectId) {
        executeScanAsync(scanId, projectId, ScanConfigurationDto.defaults());
    }

    /**
     * Executes the full config-driven background scan pipeline.
     * Logs effective configuration, runs only enabled rule categories,
     * applies confidence threshold, strips OWASP/CWE if disabled,
     * enforces timeout with cancellation, and wires AI config flags.
     *
     * @param scanId        the scan ID
     * @param projectId     the project ID
     * @param configuration the scan configuration to apply
     */
    void executeScanAsync(Long scanId, Long projectId, ScanConfigurationDto configuration);

    /**
     * Retrieves the results of a specific scan.
     *
     * @param scanId the scan history ID
     * @return the scan summary and vulnerabilities
     */
    ScanSummaryResponse getScanResult(Long scanId);

    /**
     * Retrieves all scan histories for a specific project.
     *
     * @param projectId the project ID
     * @return list of scan summaries
     */
    List<ScanSummaryResponse> getProjectScans(Long projectId);

    /**
     * Retrieves all scans belonging to the currently authenticated user.
     *
     * @return list of all scan summaries for the user
     */
    List<ScanSummaryResponse> getAllUserScans();

    /**
     * Deletes a specific scan and its associated vulnerabilities.
     *
     * @param scanId ID of the scan to delete
     */
    void deleteScan(Long scanId);

    /**
     * Performs a direct inline scan on code pasted or uploaded without a project.
     *
     * @param request   the direct scan payload
     * @param userEmail the authenticated user's email
     * @return the completed scan summary including all vulnerabilities
     */
    ScanSummaryResponse directScan(com.sanjay.aisecurity.dto.request.DirectScanRequest request, String userEmail);

    /**
     * Performs a direct scan on an uploaded MultipartFile (handles ZIPs and plain text).
     *
     * @param file      the uploaded file
     * @param userEmail the authenticated user's email
     * @return the completed scan summary
     */
    ScanSummaryResponse directScanFile(org.springframework.web.multipart.MultipartFile file, String userEmail);

    /**
     * Performs a quick scan on a source code snippet reusing the full pipeline.
     *
     * @param request   the quick scan payload
     * @param userEmail the authenticated user's email
     * @return the completed scan summary
     */
    ScanSummaryResponse quickScan(com.sanjay.aisecurity.dto.request.QuickScanRequest request, String userEmail);
}
