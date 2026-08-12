package com.sanjay.aisecurity.controller;

import com.sanjay.aisecurity.common.ApiResponse;
import com.sanjay.aisecurity.constants.MessageConstants;
import com.sanjay.aisecurity.dto.response.DashboardResponse;
import com.sanjay.aisecurity.service.DashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller for user dashboard analytics.
 *
 * @author Sanjay
 * @version 1.0.0
 */
@RestController
@RequestMapping("/api/v1/dashboard")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Dashboard", description = "Analytics and risk score endpoints.")
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping
    @Operation(summary = "Get my dashboard", description = "Returns total projects, scans, vulnerabilities by severity, risk score trends, and recent scan history for the authenticated user.")
    public ResponseEntity<ApiResponse<DashboardResponse>> getMyDashboard() {
        DashboardResponse response = dashboardService.getMyDashboard();
        return ResponseEntity.ok(ApiResponse.success(MessageConstants.SUCCESS, response));
    }
}
