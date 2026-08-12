package com.sanjay.aisecurity.controller;

import com.sanjay.aisecurity.common.ApiResponse;
import com.sanjay.aisecurity.constants.MessageConstants;
import com.sanjay.aisecurity.dto.request.DirectScanRequest;
import com.sanjay.aisecurity.dto.request.ScanConfigurationDto;
import com.sanjay.aisecurity.dto.request.TriggerScanRequest;
import com.sanjay.aisecurity.dto.response.ScanSummaryResponse;
import com.sanjay.aisecurity.service.ScanService;
import com.sanjay.aisecurity.util.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * REST Controller for Static Code Analysis operations.
 *
 * @author Sanjay
 * @version 1.0.0
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/scan")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Static Code Analysis", description = "Endpoints for triggering and retrieving security scans.")
public class ScanController {

    private final ScanService scanService;

    @PostMapping("/{projectId}")
    @Operation(
        summary = "Trigger async scan",
        description = "Initializes a new security scan for the project and runs it asynchronously. " +
                      "Optionally accepts a ScanConfigurationDto in the request body. If absent, defaults are used.")
    public ResponseEntity<ApiResponse<Map<String, Long>>> triggerScan(
            @PathVariable Long projectId,
            @Valid @RequestBody(required = false) TriggerScanRequest request) {

        log.info("REST request to trigger scan for project {}", projectId);

        ScanConfigurationDto config = (request != null)
                ? request.getConfigurationOrDefaults()
                : ScanConfigurationDto.defaults();

        // Create the scan synchronously (validates config, persists configurationJson)
        Long scanId = scanService.triggerScan(projectId, config);

        // Trigger the asynchronous background worker with the same config
        scanService.executeScanAsync(scanId, projectId, config);

        Map<String, Long> responseMap = new HashMap<>();
        responseMap.put("scanId", scanId);

        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(ApiResponse.success("Scan triggered successfully and is running in the background.", responseMap));
    }

    @GetMapping("/{scanId}")
    @Operation(summary = "Get scan result", description = "Retrieves the full summary and vulnerabilities for a specific scan.")
    public ResponseEntity<ApiResponse<ScanSummaryResponse>> getScanResult(@PathVariable Long scanId) {
        ScanSummaryResponse result = scanService.getScanResult(scanId);
        return ResponseEntity.ok(ApiResponse.success(MessageConstants.SUCCESS, result));
    }

    @org.springframework.web.bind.annotation.DeleteMapping("/{scanId}")
    @Operation(summary = "Delete scan", description = "Deletes a scan and its associated history.")
    public ResponseEntity<ApiResponse<Void>> deleteScan(@PathVariable Long scanId) {
        scanService.deleteScan(scanId);
        return ResponseEntity.ok(ApiResponse.success("Scan deleted successfully."));
    }

    @GetMapping("/project/{projectId}")
    @Operation(summary = "Get project scans", description = "Retrieves all historical scans for a specific project.")
    public ResponseEntity<ApiResponse<List<ScanSummaryResponse>>> getProjectScans(@PathVariable Long projectId) {
        List<ScanSummaryResponse> results = scanService.getProjectScans(projectId);
        return ResponseEntity.ok(ApiResponse.success(MessageConstants.SUCCESS, results));
    }

    @GetMapping("/my")
    @Operation(summary = "Get all user scans", description = "Retrieves all historical scans across all projects for the authenticated user.")
    public ResponseEntity<ApiResponse<List<ScanSummaryResponse>>> getAllUserScans() {
        List<ScanSummaryResponse> results = scanService.getAllUserScans();
        return ResponseEntity.ok(ApiResponse.success(MessageConstants.SUCCESS, results));
    }

    @PostMapping("/direct")
    @Operation(summary = "Direct inline scan",
               description = "Scans code pasted directly or uploaded as text without requiring a project.")
    public ResponseEntity<ApiResponse<ScanSummaryResponse>> directScan(
            @Valid @RequestBody DirectScanRequest request) {
        String email = SecurityUtils.requireCurrentUserEmail();
        log.info("Direct scan requested by {}, language={}, filename={}",
                email, request.getLanguage(), request.getFilename());
        ScanSummaryResponse result = scanService.directScan(request, email);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Scan completed.", result));
    }

    @PostMapping(value = "/direct/file", consumes = org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Direct file/ZIP scan",
               description = "Scans an uploaded file or ZIP archive directly without requiring a project.")
    public ResponseEntity<ApiResponse<ScanSummaryResponse>> directScanFile(
            @org.springframework.web.bind.annotation.RequestParam("file") org.springframework.web.multipart.MultipartFile file) {
        String email = SecurityUtils.requireCurrentUserEmail();
        log.info("Direct file scan requested by {}, filename={}", email, file.getOriginalFilename());
        ScanSummaryResponse result = scanService.directScanFile(file, email);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Scan completed.", result));
    }

    @PostMapping("/quick")
    @Operation(summary = "Quick inline scan",
               description = "Scans a code snippet and returns the full result synchronously.")
    public ResponseEntity<ApiResponse<ScanSummaryResponse>> quickScan(
            @Valid @RequestBody com.sanjay.aisecurity.dto.request.QuickScanRequest request) {
        String email = SecurityUtils.requireCurrentUserEmail();
        log.info("Quick scan requested by {}, language={}, filename={}",
                email, request.getLanguage(), request.getFilename());
        ScanSummaryResponse result = scanService.quickScan(request, email);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Quick Scan completed.", result));
    }
}
