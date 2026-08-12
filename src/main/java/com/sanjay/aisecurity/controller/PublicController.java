package com.sanjay.aisecurity.controller;

import com.sanjay.aisecurity.constants.ApiConstants;
import com.sanjay.aisecurity.common.ApiResponse;
import com.sanjay.aisecurity.repository.ProjectRepository;
import com.sanjay.aisecurity.repository.ScanHistoryRepository;
import com.sanjay.aisecurity.repository.UserRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping(ApiConstants.API_V1 + "/public")
@RequiredArgsConstructor
@Tag(name = "Public Operations", description = "Publicly accessible endpoints")
public class PublicController {

    private final ScanHistoryRepository scanHistoryRepository;
    private final UserRepository userRepository;
    private final ProjectRepository projectRepository;

    @GetMapping("/stats")
    @Operation(summary = "Get public global statistics for landing page")
    public ResponseEntity<ApiResponse<Map<String, Long>>> getPublicStats() {
        Map<String, Long> stats = new HashMap<>();
        stats.put("totalScans", scanHistoryRepository.count());
        stats.put("vulnerabilitiesFound", scanHistoryRepository.countByStatus(com.sanjay.aisecurity.enums.ScanStatus.FAILED)); 
        // We will approximate vulnerabilities by the number of failed scans for simplicity, 
        // or just count all scans to avoid showing 0 if no vulns logic exists yet.
        stats.put("registeredUsers", userRepository.count());
        stats.put("activeProjects", projectRepository.count());

        return ResponseEntity.ok(ApiResponse.success(
                "Public stats fetched successfully",
                stats
        ));
    }
}
