package com.sanjay.aisecurity.controller;

import com.sanjay.aisecurity.constants.ApiConstants;
import com.sanjay.aisecurity.common.ApiResponse;
import com.sanjay.aisecurity.repository.ProjectRepository;
import com.sanjay.aisecurity.repository.ScanHistoryRepository;
import com.sanjay.aisecurity.repository.UserRepository;
import com.sanjay.aisecurity.repository.VulnerabilityRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping(ApiConstants.API_V1 + "/public")
@RequiredArgsConstructor
@Tag(name = "Public Operations", description = "Publicly accessible endpoints")
public class PublicController {

    private final ScanHistoryRepository scanHistoryRepository;
    private final UserRepository userRepository;
    private final ProjectRepository projectRepository;
    private final VulnerabilityRepository vulnerabilityRepository;

    @GetMapping("/stats")
    @Operation(summary = "Get public global statistics for landing page")
    public ResponseEntity<ApiResponse<Map<String, Long>>> getPublicStats() {
        try {
            Map<String, Long> stats = new HashMap<>();
            stats.put("totalScans", scanHistoryRepository.count());
            stats.put("vulnerabilitiesFound", vulnerabilityRepository.count());
            stats.put("registeredUsers", userRepository.count());
            stats.put("activeProjects", projectRepository.count());

            return ResponseEntity.ok(ApiResponse.success(
                    "Public stats fetched successfully",
                    stats
            ));
        } catch (Exception e) {
            log.error("Failed to fetch public stats: {}", e.getMessage());
            // Return zeroes gracefully rather than a 500
            Map<String, Long> empty = new HashMap<>();
            empty.put("totalScans", 0L);
            empty.put("vulnerabilitiesFound", 0L);
            empty.put("registeredUsers", 0L);
            empty.put("activeProjects", 0L);
            return ResponseEntity.ok(ApiResponse.success("Public stats fetched", empty));
        }
    }
}
