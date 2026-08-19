package com.sanjay.aisecurity.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sanjay.aisecurity.analyzer.LanguageDetector;
import com.sanjay.aisecurity.analyzer.LanguageDetector.Language;
import com.sanjay.aisecurity.analyzer.RuleEngine;
import com.sanjay.aisecurity.analyzer.VulnerabilityResult;
import com.sanjay.aisecurity.analyzer.java.JavaRuleEngine;
import com.sanjay.aisecurity.analyzer.js.JavaScriptRuleEngine;
import com.sanjay.aisecurity.analyzer.python.PythonRuleEngine;
import com.sanjay.aisecurity.analyzer.sql.SqlRuleEngine;
import com.sanjay.aisecurity.constants.MessageConstants;
import com.sanjay.aisecurity.dto.request.DirectScanRequest;
import com.sanjay.aisecurity.dto.request.ScanConfigurationDto;
import com.sanjay.aisecurity.dto.response.ScanSummaryResponse;
import com.sanjay.aisecurity.dto.response.VulnerabilityResponse;
import com.sanjay.aisecurity.entity.Project;
import com.sanjay.aisecurity.entity.ScanHistory;
import com.sanjay.aisecurity.entity.UploadedFile;
import com.sanjay.aisecurity.entity.User;
import com.sanjay.aisecurity.entity.Vulnerability;
import com.sanjay.aisecurity.enums.AiStatus;
import com.sanjay.aisecurity.enums.ProjectType;
import com.sanjay.aisecurity.enums.ScanStatus;
import com.sanjay.aisecurity.exception.ResourceNotFoundException;
import com.sanjay.aisecurity.repository.ProjectRepository;
import com.sanjay.aisecurity.repository.ScanHistoryRepository;
import com.sanjay.aisecurity.repository.UploadedFileRepository;
import com.sanjay.aisecurity.repository.UserRepository;
import com.sanjay.aisecurity.repository.VulnerabilityRepository;
import com.sanjay.aisecurity.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.io.FileInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import org.apache.commons.io.FilenameUtils;

import com.sanjay.aisecurity.ai.AiProvider;
import com.sanjay.aisecurity.ai.PromptBuilder;

/**
 * Service implementation for the Static Code Analysis Engine.
 *
 * @author Sanjay
 * @version 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ScanServiceImpl implements ScanService {

    private final ScanHistoryRepository scanHistoryRepository;
    private final ProjectRepository projectRepository;
    private final UploadedFileRepository uploadedFileRepository;
    private final VulnerabilityRepository vulnerabilityRepository;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    private final com.sanjay.aisecurity.analyzer.pipeline.FileDiscoveryService fileDiscoveryService;
    private final com.sanjay.aisecurity.analyzer.pipeline.ScannerSelector scannerSelector;
    private final com.sanjay.aisecurity.analyzer.pipeline.VulnerabilityDeduplicator vulnerabilityDeduplicator;
    private final org.springframework.beans.factory.ObjectProvider<java.util.concurrent.Executor> scanTaskExecutorProvider;
    private final com.sanjay.aisecurity.service.analytics.RiskScoreService riskScoreService;
    private final com.sanjay.aisecurity.service.analytics.ScanAnalyticsService scanAnalyticsService;
    private final com.sanjay.aisecurity.service.ai.AiEnrichmentService aiEnrichmentService;
    private final UploadService uploadService;
    private final org.springframework.transaction.support.TransactionTemplate transactionTemplate;
    
    private final java.util.Set<Long> activeScanTriggers = java.util.concurrent.ConcurrentHashMap.newKeySet();

    @Override
    @Transactional
    public Long triggerScan(Long projectId) {
        return triggerScan(projectId, ScanConfigurationDto.defaults());
    }

    @Override
    @Transactional
    public Long triggerScan(Long projectId, ScanConfigurationDto configuration) {
        String email = SecurityUtils.requireCurrentUserEmail();
        Project project = resolveOwnedProject(projectId, email);

        // --- Validate configuration ---
        ScanConfigurationDto config = (configuration != null) ? configuration : ScanConfigurationDto.defaults();
        if (config.getTimeoutSeconds() <= 0)
            throw new IllegalArgumentException("Scan configuration: timeoutSeconds must be > 0");
        if (config.getConfidenceThreshold() < 0 || config.getConfidenceThreshold() > 100)
            throw new IllegalArgumentException("Scan configuration: confidenceThreshold must be 0-100");
        if (config.getMaxFileSizeMB() <= 0)
            throw new IllegalArgumentException("Scan configuration: maxFileSizeMB must be > 0");

        if (!activeScanTriggers.add(projectId)) {
            throw new com.sanjay.aisecurity.exception.DuplicateRequestException("A scan is already being triggered for this project.");
        }
        
        try {
            List<ScanHistory> activeScans = scanHistoryRepository.findByProjectIdAndStatusIn(projectId, java.util.List.of(ScanStatus.PENDING, ScanStatus.RUNNING));
            for (ScanHistory activeScan : activeScans) {
                if (activeScan.getCreatedAt() != null && activeScan.getCreatedAt().isBefore(LocalDateTime.now().minusMinutes(1))) {
                    activeScan.setStatus(ScanStatus.FAILED);
                    scanHistoryRepository.save(activeScan);
                } else {
                    throw new com.sanjay.aisecurity.exception.DuplicateRequestException("A scan is already running for this project. Please wait for it to finish.");
                }
            }

            log.info("Triggering new scan for project ID {} with configuration", projectId);

            // --- Serialize config to JSON for audit ---
            String configJson = null;
            try {
                configJson = objectMapper.writeValueAsString(config);
            } catch (Exception e) {
                log.warn("Failed to serialize scan configuration to JSON: {}", e.getMessage());
            }

            ScanHistory scanHistory = ScanHistory.builder()
                    .project(project)
                    .status(ScanStatus.PENDING)
                    .progressPercentage(0)
                    .scanStart(LocalDateTime.now())
                    .configurationJson(configJson)
                    .build();

            scanHistory = scanHistoryRepository.save(scanHistory);
            return scanHistory.getId();
        } finally {
            activeScanTriggers.remove(projectId);
        }
    }

    @Async("taskExecutor")
    public void executeScanAsync(Long scanId, Long projectId) {
        executeScanAsync(scanId, projectId, ScanConfigurationDto.defaults());
    }

    @Override
    @Async("taskExecutor")
    public void executeScanAsync(Long scanId, Long projectId, ScanConfigurationDto configuration) {
        final ScanConfigurationDto config = (configuration != null) ? configuration : ScanConfigurationDto.defaults();

        log.info("Executing async scan ID {} for project ID {}", scanId, projectId);
        log.info(config.toLogString());

        ScanHistory scanHistory = scanHistoryRepository.findById(scanId).orElse(null);
        if (scanHistory == null) return;
        
        scanHistory.setStatus(ScanStatus.RUNNING);
        scanHistory.setScanStart(LocalDateTime.now());
        scanHistoryRepository.save(scanHistory);

        java.util.concurrent.ExecutorService scanTaskExecutor = java.util.concurrent.ForkJoinPool.commonPool();

        try {
            long tStart = System.currentTimeMillis();
            
            long tDiscoveryStart = System.currentTimeMillis();
            List<UploadedFile> files = uploadedFileRepository.findByProjectIdAndIsDeletedFalse(projectId, org.springframework.data.domain.Pageable.unpaged()).getContent();
            List<com.sanjay.aisecurity.analyzer.pipeline.DiscoveredFile> discoveredFiles = new ArrayList<>();
            
            // 1. Discover all files — respects maxFileSizeMB, ignoreDirectories, skipGeneratedFiles
            for (UploadedFile file : files) {
                if (file.getFileExtension().equalsIgnoreCase(".zip")) {
                    try (FileInputStream fis = new FileInputStream(file.getStoragePath())) {
                        discoveredFiles.addAll(fileDiscoveryService.discoverFromZip(fis, config));
                    } catch (Exception e) {
                        log.error("Failed to extract ZIP: {}", file.getOriginalFileName(), e);
                    }
                } else {
                    try (FileInputStream fis = new FileInputStream(file.getStoragePath())) {
                        com.sanjay.aisecurity.analyzer.pipeline.DiscoveredFile df =
                                fileDiscoveryService.discoverSingleFile(fis, file.getOriginalFileName(), config);
                        if (df != null) discoveredFiles.add(df);
                    } catch (Exception e) {
                        log.error("Failed to read file: {}", file.getOriginalFileName(), e);
                    }
                }
            }

            int totalFiles = discoveredFiles.size();
            long tDiscoveryEnd = System.currentTimeMillis();

            
            executeScanPipeline(scanId, projectId, discoveredFiles, totalFiles, tStart, tDiscoveryStart, tDiscoveryEnd, config, scanHistory, scanTaskExecutor, false);
        } catch (Exception e) {
            log.error("Scan ID {} failed entirely: {}", scanId, e.getMessage(), e);
            scanHistory.setStatus(ScanStatus.FAILED);
            scanHistory.setScanEnd(LocalDateTime.now());
            scanHistoryRepository.save(scanHistory);
        } finally {
            // Always securely wipe uploaded source files from the server.
            // This runs regardless of scan success, failure, or timeout.
            try {
                uploadService.securelyDeleteProjectFiles(projectId);
            } catch (Exception cleanupEx) {
                log.error("[Security] Failed to clean up files for project {} after scan {}: {}",
                        projectId, scanId, cleanupEx.getMessage(), cleanupEx);
            }
        }
    }

    private ScanSummaryResponse executeScanPipeline(Long scanId, Long projectId, List<com.sanjay.aisecurity.analyzer.pipeline.DiscoveredFile> discoveredFiles, 
                                     int totalFiles, long tStart, long tDiscoveryStart, long tDiscoveryEnd, 
                                     ScanConfigurationDto config, ScanHistory scanHistory, 
                                     java.util.concurrent.ExecutorService scanTaskExecutor, boolean isQuickScan) {
        long tScanStart = System.currentTimeMillis();
            java.util.concurrent.atomic.AtomicInteger filesProcessed = new java.util.concurrent.atomic.AtomicInteger(0);
            java.util.concurrent.atomic.AtomicLong lastDbUpdate = new java.util.concurrent.atomic.AtomicLong(System.currentTimeMillis());
            
            java.util.concurrent.atomic.AtomicInteger successfulScans = new java.util.concurrent.atomic.AtomicInteger(0);
            java.util.concurrent.atomic.AtomicInteger skippedScans = new java.util.concurrent.atomic.AtomicInteger(0);
            java.util.concurrent.atomic.AtomicInteger failedScans = new java.util.concurrent.atomic.AtomicInteger(0);

            // 2. Map to CompletableFutures — config-filtered engine selection per file
            List<java.util.concurrent.CompletableFuture<List<Vulnerability>>> futures = discoveredFiles.stream().map(df -> {
                java.util.concurrent.CompletableFuture<List<Vulnerability>> cf = new java.util.concurrent.CompletableFuture<>();
                // Use AsyncTaskExecutor so we can cancel the Future if timeout occurs
                java.util.concurrent.Future<?> underlyingTask = 
                    scanTaskExecutor.submit(() -> {
                    List<Vulnerability> localVulns = new ArrayList<>();
                    try {
                        String content = new String(df.getContent(), StandardCharsets.UTF_8);
                        
                        // Config-aware engine selection: only rules enabled in config will run
                        RuleEngine engine = scannerSelector.select(df, scanHistory.getProject().getProjectType(), config).orElse(null);
                        
                        if (engine != null) {
                            log.info("Using engine {} for file {}", engine.getClass().getSimpleName(), df.getFileName());
                            List<VulnerabilityResult> results = engine.scan(content, df.getFileName());
                            log.info("Found {} vulnerabilities in file {}", results.size(), df.getFileName());
                            for (VulnerabilityResult res : results) {
                                localVulns.add(Vulnerability.builder()
                                        .scanHistory(scanHistory)
                                        .vulnerabilityType(res.getType())
                                        .severity(res.getSeverity())
                                        .description(res.getDescription())
                                        .recommendation(res.getRecommendation())
                                        .fileName(res.getFileName())
                                        .lineNumber(res.getLineNumber())
                                        .codeSnippet(res.getCodeSnippet())
                                        .confidenceScore(res.getConfidenceScore())
                                        .evidence(res.getEvidence())
                                        .owaspCategory(res.getOwaspCategory())
                                        .cweId(res.getCweId())
                                        .ruleId(res.getRuleId())
                                        .detectionSource(res.getDetectionSource())
                                        .build());
                            }
                            successfulScans.incrementAndGet();
                        } else {
                            skippedScans.incrementAndGet();
                        }
                    } catch (Exception e) {
                        log.error("Scan failed for file: {}", df.getFileName(), e);
                        failedScans.incrementAndGet();
                        // Retry once for transient errors
                        try {
                            log.info("Retrying scan for file: {}", df.getFileName());
                            String content = new String(df.getContent(), StandardCharsets.UTF_8);
                            RuleEngine engine = scannerSelector.select(df, scanHistory.getProject().getProjectType(), config).orElse(null);
                            if (engine != null) {
                                List<VulnerabilityResult> results = engine.scan(content, df.getFileName());
                                localVulns.clear();
                                for (VulnerabilityResult res : results) {
                                    localVulns.add(Vulnerability.builder()
                                            .scanHistory(scanHistory)
                                            .vulnerabilityType(res.getType())
                                            .severity(res.getSeverity())
                                            .description(res.getDescription())
                                            .recommendation(res.getRecommendation())
                                            .fileName(res.getFileName())
                                            .lineNumber(res.getLineNumber())
                                            .codeSnippet(res.getCodeSnippet())
                                            .confidenceScore(res.getConfidenceScore())
                                            .evidence(res.getEvidence())
                                            .owaspCategory(res.getOwaspCategory())
                                            .cweId(res.getCweId())
                                            .ruleId(res.getRuleId())
                                            .detectionSource(res.getDetectionSource())
                                            .build());
                                }
                                failedScans.decrementAndGet();
                                successfulScans.incrementAndGet();
                            }
                        } catch (Exception retryEx) {
                            log.error("Retry failed for file: {}", df.getFileName(), retryEx);
                        }
                    }
                    cf.complete(localVulns);
                });
                
                // If CF is cancelled, interrupt the underlying task
                cf.whenComplete((res, ex) -> {
                    if (cf.isCancelled()) {
                        underlyingTask.cancel(true);
                    }
                });
                
                return cf
                // Per-file timeout with configured value
                .orTimeout(config.getTimeoutSeconds(), java.util.concurrent.TimeUnit.SECONDS)
                .exceptionally(ex -> {
                    if (ex instanceof java.util.concurrent.TimeoutException) {
                        log.warn("[Timeout] File scan exceeded {}s timeout — cancelled: {}",
                                config.getTimeoutSeconds(), df.getFileName());
                    } else {
                        log.error("[Scan] Future failed for file {}: {}", df.getFileName(), ex.getMessage());
                    }
                    failedScans.incrementAndGet();
                    return new ArrayList<>();
                })
                .thenApply(result -> {
                    // Update progress safely
                    int currentCompleted = filesProcessed.incrementAndGet();
                    long now = System.currentTimeMillis();
                    long lastUpdate = lastDbUpdate.get();
                    
                    // Throttle DB updates: 500ms minimum interval
                    if (totalFiles > 0 && (now - lastUpdate) > 500) {
                        if (lastDbUpdate.compareAndSet(lastUpdate, now)) {
                            int pct = (int) (((double) currentCompleted / totalFiles) * 100);
                            scanHistoryRepository.updateProgress(scanId, pct);
                        }
                    }
                    return result;
                });
            }).collect(Collectors.toList());

            // 3. Wait for all futures — overall scan timeout guard with Future.cancel(true)
            java.util.concurrent.CompletableFuture<Void> allOf =
                    java.util.concurrent.CompletableFuture.allOf(futures.toArray(new java.util.concurrent.CompletableFuture[0]));
            try {
                allOf.get(config.getTimeoutSeconds(), java.util.concurrent.TimeUnit.SECONDS);
            } catch (java.util.concurrent.TimeoutException te) {
                log.warn("[Scan] Overall scan timeout reached ({}s). Cancelling all remaining futures.",
                        config.getTimeoutSeconds());
                futures.forEach(f -> f.cancel(true));
                scanHistory.setStatus(ScanStatus.FAILED);
                scanHistory.setScanEnd(LocalDateTime.now());
                scanHistory.setAiSummary("Scan aborted due to timeout (" + config.getTimeoutSeconds() + "s limit)");
                scanHistoryRepository.save(scanHistory);
                return isQuickScan ? getScanResult(scanId) : null;
            } catch (Exception e) {
                log.warn("[Scan] allOf.get() interrupted: {}", e.getMessage());
            }

            // 4. Aggregate results
            List<Vulnerability> allVulnerabilities = futures.stream()
                    .filter(f -> !f.isCancelled())
                    .map(f -> {
                        try { return f.join(); }
                        catch (Exception e) { return new ArrayList<Vulnerability>(); }
                    })
                    .flatMap(List::stream)
                    .collect(Collectors.toList());
            long tScanEnd = System.currentTimeMillis();

            // 5. Deduplication
            long tDedupStart = System.currentTimeMillis();
            List<Vulnerability> deduped = vulnerabilityDeduplicator.deduplicate(allVulnerabilities);
            long tDedupEnd = System.currentTimeMillis();

            // 6. Confidence threshold filter — discard findings below configured threshold
            int beforeConfidence = deduped.size();
            double thresholdFraction = config.getConfidenceThreshold() / 100.0;
            deduped.removeIf(v -> v.getConfidenceScore() < thresholdFraction);
            int removedByConfidence = beforeConfidence - deduped.size();
            if (removedByConfidence > 0) {
                log.info("[Config] Confidence threshold ({}%): removed {} findings below threshold.",
                        config.getConfidenceThreshold(), removedByConfidence);
            }

            // 7. OWASP / CWE metadata strip — does NOT remove findings, only nullifies classification fields
            if (!config.isOwasp()) {
                deduped.forEach(v -> v.setOwaspCategory(null));
                log.info("[Config] OWASP classifications stripped from {} findings (owasp=OFF).", deduped.size());
            }
            if (!config.isCwe()) {
                deduped.forEach(v -> v.setCweId(null));
                log.info("[Config] CWE IDs stripped from {} findings (cwe=OFF).", deduped.size());
            }

            final List<Vulnerability> finalVulnerabilities = deduped;

            // 8. Persist findings
            long tPersistStart = System.currentTimeMillis();
            vulnerabilityRepository.saveAll(finalVulnerabilities);
            long tPersistMid = System.currentTimeMillis();
            
            // 9. Risk Score Calculation
            long tRiskStart = System.currentTimeMillis();
            double finalScore = riskScoreService.calculateScore(finalVulnerabilities);
            long tRiskEnd = System.currentTimeMillis();

            // 10. Analytics Generation
            long tAnalyticsStart = System.currentTimeMillis();
            com.sanjay.aisecurity.service.analytics.ScanSummary summary = scanAnalyticsService.generateSummary(
                    scanHistory, finalVulnerabilities, totalFiles, successfulScans.get(), skippedScans.get(), failedScans.get(), finalScore);
            log.info(summary.formatForLogs());
            long tAnalyticsEnd = System.currentTimeMillis();

            // 11. Persist core scan results
            transactionTemplate.execute(status -> {
                ScanHistory dbScan = scanHistoryRepository.findById(scanId).orElse(null);
                if (dbScan != null) {
                    dbScan.setStatus(ScanStatus.COMPLETED);
                    dbScan.setScanEnd(LocalDateTime.now());
                    dbScan.setDuration(Duration.between(dbScan.getScanStart(), dbScan.getScanEnd()).toMillis());
                    dbScan.setScannedFiles(successfulScans.get());
                    dbScan.setTotalFiles(totalFiles);
                    dbScan.setTotalVulnerabilities(finalVulnerabilities.size());
                    dbScan.setSecurityScore(finalScore);
                    dbScan.setProgressPercentage(100);

                    dbScan.getVulnerabilities().clear();
                    dbScan.getVulnerabilities().addAll(finalVulnerabilities);

                    scanHistoryRepository.save(dbScan);

                    Project project = dbScan.getProject();
                    project.setSecurityScore(finalScore);
                    project.setLastScan(LocalDateTime.now());
                    projectRepository.save(project);
                }
                return null;
            });
            long tPersistEnd = System.currentTimeMillis();

            long tTotalEnd = System.currentTimeMillis();
            
            // Log stage durations
            log.info("--- Scan Stage Durations ---");
            log.info("Discovery .......... {} s", (tDiscoveryEnd - tDiscoveryStart) / 1000.0);
            log.info("Scanning ........... {} s", (tScanEnd - tScanStart) / 1000.0);
            log.info("Deduplication ...... {} s", (tDedupEnd - tDedupStart) / 1000.0);
            log.info("Risk Score ......... {} s", (tRiskEnd - tRiskStart) / 1000.0);
            log.info("Analytics .......... {} s", (tAnalyticsEnd - tAnalyticsStart) / 1000.0);
            log.info("Persistence ........ {} s", ((tPersistMid - tPersistStart) + (tPersistEnd - tAnalyticsEnd)) / 1000.0);
            log.info("AI ................. (Async Triggered)");
            log.info("Total .............. {} s", (tTotalEnd - tStart) / 1000.0);
            log.info("----------------------------");

            // 12. Async AI Enrichment — fire & forget, passes AI config flags
            if (isQuickScan) {
                aiEnrichmentService.enrichScanSummary(
                        scanId, finalVulnerabilities, successfulScans.get(),
                        summary.getCriticalCount(), summary.getHighCount(),
                        summary.getMediumCount(), summary.getLowCount(),
                        false, config);
            } else {
                aiEnrichmentService.enrichScanSummaryAsync(
                        scanId, finalVulnerabilities, successfulScans.get(),
                        summary.getCriticalCount(), summary.getHighCount(),
                        summary.getMediumCount(), summary.getLowCount(),
                        false, config);
            }

        
        if (isQuickScan) {
            return getScanResult(scanId);
        }
        return null;
    }


    @Override
    @Transactional(readOnly = true)
    public ScanSummaryResponse getScanResult(Long scanId) {
        String email = SecurityUtils.requireCurrentUserEmail();
        ScanHistory scan = resolveOwnedScan(scanId, email);

        List<VulnerabilityResponse> vulns = scan.getVulnerabilities().stream()
                .map(this::toVulnerabilityResponse)
                .collect(Collectors.toList());

        ScanSummaryResponse response = toScanSummaryResponse(scan);
        response.setVulnerabilities(vulns);
        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ScanSummaryResponse> getProjectScans(Long projectId) {
        String email = SecurityUtils.requireCurrentUserEmail();
        resolveOwnedProject(projectId, email); // Security check
        
        List<ScanHistory> scans = scanHistoryRepository.findByProjectIdOrderByScanStartDesc(projectId);
        return scans.stream().map(this::toScanSummaryResponse).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public ScanSummaryResponse directScan(DirectScanRequest request, String userEmail) {
        // 1. Resolve the user entity
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found."));

        // 2. Find or create a hidden system project for direct scans
        final String DIRECT_PROJECT_NAME = "[Direct Scans]";
        Project project = projectRepository.findByUserEmailAndNameAndActiveTrue(userEmail, DIRECT_PROJECT_NAME)
                .orElseGet(() -> projectRepository.save(
                        Project.builder()
                                .name(DIRECT_PROJECT_NAME)
                                .description("Auto-created project for inline/direct scans.")
                                .projectType(ProjectType.OTHER)
                                .user(user)
                                .active(true)
                                .build()));

        // 3. Determine language from provided hint
        String lang = request.getLanguage() != null ? request.getLanguage() : "txt";
        Language language = LanguageDetector.detect(lang, project.getProjectType());
        String filename = (request.getFilename() != null && !request.getFilename().isBlank())
                ? request.getFilename() : "inline-code." + lang;

        // 4. Create scan record
        ScanHistory scan = ScanHistory.builder()
                .project(project)
                .status(ScanStatus.RUNNING)
                .progressPercentage(0)
                .scanStart(LocalDateTime.now())
                .build();
        scan = scanHistoryRepository.save(scan);

        // 5. Run the rule engine synchronously
        RuleEngine engine = scannerSelector.select(
                com.sanjay.aisecurity.analyzer.pipeline.DiscoveredFile.builder()
                        .path(filename)
                        .fileName(filename)
                        .extension(language.name())
                        .build(), 
                project.getProjectType()).orElse(null);
        List<Vulnerability> allVulnerabilities = new ArrayList<>();

        if (engine != null) {
            List<VulnerabilityResult> results = engine.scan(request.getCode(), filename);
            for (VulnerabilityResult res : results) {
                allVulnerabilities.add(Vulnerability.builder()
                        .scanHistory(scan)
                        .vulnerabilityType(res.getType())
                        .severity(res.getSeverity())
                        .description(res.getDescription())
                        .recommendation(res.getRecommendation())
                        .fileName(res.getFileName())
                        .lineNumber(res.getLineNumber())
                        .codeSnippet(res.getCodeSnippet())
                        .confidenceScore(res.getConfidenceScore())
                        .build());
            }
        }
        vulnerabilityRepository.saveAll(allVulnerabilities);

        // 6. Compute security score
        double score = riskScoreService.calculateScore(allVulnerabilities);

        scan.setStatus(ScanStatus.COMPLETED);
        scan.setScanEnd(LocalDateTime.now());
        scan.setDuration(Duration.between(scan.getScanStart(), scan.getScanEnd()).toMillis());
        scan.setScannedFiles(1);
        scan.setTotalFiles(1);
        scan.setTotalVulnerabilities(allVulnerabilities.size());
        scan.setSecurityScore(score);
        scan.setProgressPercentage(100);
        scanHistoryRepository.save(scan);

        // 7. Build and return full response with vulnerabilities
        List<VulnerabilityResponse> vulnResponses = allVulnerabilities.stream()
                .map(this::toVulnerabilityResponse)
                .collect(Collectors.toList());

        ScanSummaryResponse response = toScanSummaryResponse(scan);
        response.setVulnerabilities(vulnResponses);
        return response;
    }

    @Override
    @Transactional
    public ScanSummaryResponse directScanFile(org.springframework.web.multipart.MultipartFile file, String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found."));

        final String DIRECT_PROJECT_NAME = "[Direct Scans]";
        Project project = projectRepository.findByUserEmailAndNameAndActiveTrue(userEmail, DIRECT_PROJECT_NAME)
                .orElseGet(() -> projectRepository.save(
                        Project.builder()
                                .name(DIRECT_PROJECT_NAME)
                                .description("Auto-created project for inline/direct scans.")
                                .projectType(ProjectType.OTHER)
                                .user(user)
                                .active(true)
                                .build()));

        ScanHistory scan = ScanHistory.builder()
                .project(project)
                .status(ScanStatus.RUNNING)
                .progressPercentage(0)
                .scanStart(LocalDateTime.now())
                .build();
        scan = scanHistoryRepository.save(scan);

        List<Vulnerability> allVulnerabilities = new ArrayList<>();
        int fileCount = 0;
        int actualScannedFiles = 0;

        try {
            String filename = file.getOriginalFilename() != null ? file.getOriginalFilename() : "uploaded_file";
            
            if (filename.toLowerCase().endsWith(".zip")) {
                try (java.util.zip.ZipInputStream zis = new java.util.zip.ZipInputStream(file.getInputStream())) {
                    java.util.zip.ZipEntry entry;
                    while ((entry = zis.getNextEntry()) != null) {
                        if (entry.isDirectory()) {
                            zis.closeEntry();
                            continue;
                        }
                        String entryName = entry.getName();
                        if (entryName.contains("node_modules/") || entryName.contains("target/") ||
                            entryName.contains("build/") || entryName.contains(".git/") ||
                            entryName.contains(".idea/")) {
                            zis.closeEntry();
                            continue;
                        }
                        if (entry.getSize() > 1024 * 1024 * 5) { // 5MB limit inside zip
                            zis.closeEntry();
                            continue;
                        }
                        
                        String extension = org.apache.commons.io.FilenameUtils.getExtension(entryName);
                        if (extension != null && !extension.isEmpty()) extension = "." + extension;
                        
                        Language lang = LanguageDetector.detect(extension, project.getProjectType());
                        RuleEngine engine = scannerSelector.select(
                                com.sanjay.aisecurity.analyzer.pipeline.DiscoveredFile.builder()
                                        .path(entryName)
                                        .fileName(entryName)
                                        .extension(extension)
                                        .build(), 
                                project.getProjectType()).orElse(null);
                        if (engine != null && lang != Language.UNKNOWN) {
                            java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
                            byte[] buffer = new byte[4096];
                            int len;
                            long totalRead = 0;
                            while ((len = zis.read(buffer)) > 0) {
                                totalRead += len;
                                if (totalRead > 1024 * 1024 * 5) break;
                                baos.write(buffer, 0, len);
                            }
                            if (totalRead <= 1024 * 1024 * 5) {
                                String content = baos.toString(java.nio.charset.StandardCharsets.UTF_8);
                                List<VulnerabilityResult> results = engine.scan(content, entryName);
                                for (VulnerabilityResult res : results) {
                                    allVulnerabilities.add(Vulnerability.builder()
                                            .scanHistory(scan)
                                            .vulnerabilityType(res.getType())
                                            .severity(res.getSeverity())
                                            .description(res.getDescription())
                                            .recommendation(res.getRecommendation())
                                            .fileName(res.getFileName())
                                            .lineNumber(res.getLineNumber())
                                            .codeSnippet(res.getCodeSnippet())
                                            .confidenceScore(res.getConfidenceScore())
                                            .evidence(res.getEvidence())
                                            .owaspCategory(res.getOwaspCategory())
                                            .cweId(res.getCweId())
                                            .build());
                                }
                                actualScannedFiles++;
                            }
                        }
                        fileCount++;
                        zis.closeEntry();
                    }
                }
            } else {
                fileCount = 1;
                String extension = org.apache.commons.io.FilenameUtils.getExtension(filename);
                if (extension != null && !extension.isEmpty()) extension = "." + extension;
                Language lang = LanguageDetector.detect(extension, project.getProjectType());
                RuleEngine engine = scannerSelector.select(
                        com.sanjay.aisecurity.analyzer.pipeline.DiscoveredFile.builder()
                                .path(filename)
                                .fileName(filename)
                                .extension(extension)
                                .build(), 
                        project.getProjectType()).orElse(null);
                if (engine != null && lang != Language.UNKNOWN) {
                    String content = new String(file.getBytes(), java.nio.charset.StandardCharsets.UTF_8);
                    List<VulnerabilityResult> results = engine.scan(content, filename);
                    for (VulnerabilityResult res : results) {
                        allVulnerabilities.add(Vulnerability.builder()
                                .scanHistory(scan)
                                .vulnerabilityType(res.getType())
                                .severity(res.getSeverity())
                                .description(res.getDescription())
                                .recommendation(res.getRecommendation())
                                .fileName(res.getFileName())
                                .lineNumber(res.getLineNumber())
                                .codeSnippet(res.getCodeSnippet())
                                .confidenceScore(res.getConfidenceScore())
                                .evidence(res.getEvidence())
                                .owaspCategory(res.getOwaspCategory())
                                .cweId(res.getCweId())
                                .aiStatus(AiStatus.PENDING)
                                .build());
                    }
                    actualScannedFiles++;
                }
            }
        } catch (Exception e) {
            log.error("Error processing direct scan file", e);
        }

        vulnerabilityRepository.saveAll(allVulnerabilities);

        double score = riskScoreService.calculateScore(allVulnerabilities);

        scan.setStatus(ScanStatus.COMPLETED);
        scan.setScanEnd(LocalDateTime.now());
        scan.setDuration(java.time.Duration.between(scan.getScanStart(), scan.getScanEnd()).toMillis());
        scan.setScannedFiles(actualScannedFiles);
        scan.setTotalFiles(fileCount);
        scan.setTotalVulnerabilities(allVulnerabilities.size());
        scan.setSecurityScore(score);
        scan.setProgressPercentage(100);
        scanHistoryRepository.save(scan);

        List<VulnerabilityResponse> vulnResponses = allVulnerabilities.stream()
                .map(this::toVulnerabilityResponse)
                .collect(Collectors.toList());

        ScanSummaryResponse response = toScanSummaryResponse(scan);
        response.setVulnerabilities(vulnResponses);
        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ScanSummaryResponse> getAllUserScans() {
        String email = SecurityUtils.requireCurrentUserEmail();
        List<ScanHistory> scans = scanHistoryRepository.findByProjectUserEmailOrderByCreatedAtDesc(email);
        return scans.stream().map(this::toScanSummaryResponse).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void deleteScan(Long scanId) {
        String email = SecurityUtils.requireCurrentUserEmail();
        ScanHistory scan = resolveOwnedScan(scanId, email);
        
        // Remove vulnerabilities first to avoid foreign key constraint violations
        vulnerabilityRepository.deleteAll(scan.getVulnerabilities());
        
        // Remove the scan
        scanHistoryRepository.delete(scan);
        log.info("Deleted scan {} requested by {}", scanId, email);
    }

    // =========================================================================
    // HELPERS

    @Override
    @Transactional
    public ScanSummaryResponse quickScan(com.sanjay.aisecurity.dto.request.QuickScanRequest request, String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found."));

        final String QUICK_PROJECT_NAME = "[Quick Scans]";
        Project project = projectRepository.findByUserEmailAndNameAndActiveTrue(userEmail, QUICK_PROJECT_NAME)
                .orElseGet(() -> projectRepository.save(
                        Project.builder()
                                .name(QUICK_PROJECT_NAME)
                                .description("Auto-created project for inline quick scans.")
                                .projectType(com.sanjay.aisecurity.enums.ProjectType.OTHER)
                                .user(user)
                                .active(true)
                                .build()));

        String langHint = request.getLanguage() != null ? request.getLanguage() : "txt";
        String ext = "";
        if (!langHint.equals("txt") && !langHint.equals("auto")) {
            ext = "." + langHint.toLowerCase();
        }
        String filename = (request.getFilename() != null && !request.getFilename().isBlank())
                ? request.getFilename() : ("QuickScan" + ext);

        int lineCount = request.getSourceCode() != null ? request.getSourceCode().split("\r?\n").length : 0;

        ScanHistory scan = ScanHistory.builder()
                .project(project)
                .status(ScanStatus.RUNNING)
                .scanType(com.sanjay.aisecurity.enums.ScanType.QUICK_SCAN)
                .snippetLanguage(langHint)
                .snippetFilename(filename)
                .snippetLines(lineCount)
                .progressPercentage(0)
                .scanStart(LocalDateTime.now())
                .configurationJson("{}")
                .build();
        scan = scanHistoryRepository.save(scan);

        java.nio.file.Path tempFile = null;
        try {
            tempFile = Files.createTempFile("quickscan_", ext);
            Files.writeString(tempFile, request.getSourceCode(), java.nio.charset.StandardCharsets.UTF_8);

            ScanConfigurationDto config = request.getConfiguration() != null ? request.getConfiguration() : ScanConfigurationDto.defaults();

            try (java.io.FileInputStream fis = new java.io.FileInputStream(tempFile.toFile())) {
                com.sanjay.aisecurity.analyzer.pipeline.DiscoveredFile df = fileDiscoveryService.discoverSingleFile(fis, filename, config);
                List<com.sanjay.aisecurity.analyzer.pipeline.DiscoveredFile> discoveredFiles = new ArrayList<>();
                if (df != null) discoveredFiles.add(df);

                java.util.concurrent.ExecutorService scanTaskExecutor = java.util.concurrent.ForkJoinPool.commonPool();
                
                long tStart = System.currentTimeMillis();
                long tDiscoveryStart = tStart;
                long tDiscoveryEnd = System.currentTimeMillis();
                
                ScanSummaryResponse response = executeScanPipeline(scan.getId(), project.getId(), discoveredFiles, 1, tStart, tDiscoveryStart, tDiscoveryEnd, config, scan, scanTaskExecutor, true);
                
                return response;
            }
        } catch (Exception e) {
            log.error("[Quick Scan] Failed to execute scan", e);
            scan.setStatus(ScanStatus.FAILED);
            scan.setScanEnd(LocalDateTime.now());
            scanHistoryRepository.save(scan);
            throw new RuntimeException("Quick Scan failed: " + e.getMessage(), e);
        } finally {
            if (tempFile != null) {
                try {
                    Files.deleteIfExists(tempFile);
                    log.info("[Security] Quick Scan temporary file securely deleted: {}", tempFile);
                } catch (Exception e) {
                    log.error("[Security] Failed to delete quick scan temp file {}", tempFile, e);
                }
            }
        }
    }

    // =========================================================================

    private Project resolveOwnedProject(Long projectId, String email) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException(MessageConstants.PROJECT_NOT_FOUND));
                
        if (!project.getUser().getEmail().equals(email)) {
            throw new org.springframework.security.access.AccessDeniedException("Access Denied: You do not own this project.");
        }
        
        if (!project.isActive()) {
            throw new ResourceNotFoundException(MessageConstants.PROJECT_NOT_FOUND);
        }
        return project;
    }

    private ScanHistory resolveOwnedScan(Long scanId, String email) {
        ScanHistory scan = scanHistoryRepository.findById(scanId)
                .orElseThrow(() -> new ResourceNotFoundException("Scan not found."));
                
        if (!scan.getProject().getUser().getEmail().equals(email)) {
            throw new org.springframework.security.access.AccessDeniedException("Access Denied: You do not own this scan.");
        }
        return scan;
    }



    private String readPhysicalFile(String storagePath) {
        try {
            Path path = Paths.get(storagePath);
            if (Files.exists(path) && Files.isReadable(path)) {
                return Files.readString(path);
            }
        } catch (Exception e) {
            log.warn("Could not read file at {}", storagePath, e);
        }
        return null;
    }
    
    private double getPenalty(Vulnerability v) {
        double basePenalty = 0.0;
        switch (v.getSeverity()) {
            case CRITICAL: basePenalty = 15.0; break;
            case HIGH: basePenalty = 10.0; break;
            case MEDIUM: basePenalty = 5.0; break;
            case LOW: basePenalty = 1.0; break;
            default: basePenalty = 0.0; break;
        }
        return basePenalty * v.getConfidenceScore();
    }

    private ScanSummaryResponse toScanSummaryResponse(ScanHistory scan) {
        String lang = scan.getProject().getProjectType() != null ? scan.getProject().getProjectType().name() : "Unknown";
        if ("OTHER".equals(lang) || "Unknown".equals(lang)) {
            // For direct ZIP scans or mixed projects, provide a better default than "Unknown"
            lang = "Mixed / Auto-detected";
        }

        List<com.sanjay.aisecurity.dto.response.VulnerabilityResponse> vulns = null;
        if (scan.getVulnerabilities() != null && !scan.getVulnerabilities().isEmpty()) {
            vulns = scan.getVulnerabilities().stream()
                    .map(this::toVulnerabilityResponse)
                    .collect(Collectors.toList());
        }

        return ScanSummaryResponse.builder()
                .scanId(scan.getId())
                .projectId(scan.getProject().getId())
                .status(scan.getStatus().name())
                .scanStart(scan.getScanStart() != null ? scan.getScanStart().toString() : null)
                .scanEnd(scan.getScanEnd() != null ? scan.getScanEnd().toString() : null)
                .durationSeconds(scan.getDuration() / 1000.0)
                .progressPercentage(scan.getProgressPercentage())
                .scannedFiles(scan.getScannedFiles())
                .totalFiles(scan.getTotalFiles())
                .totalVulnerabilities(scan.getTotalVulnerabilities())
                .securityScore(scan.getSecurityScore())
                .configurationJson(scan.getConfigurationJson())
                .language(lang)
                .scanType(scan.getScanType() != null ? scan.getScanType().name() : "PROJECT")
                .snippetFilename(scan.getSnippetFilename())
                .snippetLanguage(scan.getSnippetLanguage())
                .snippetLines(scan.getSnippetLines())
                .ruleEngineVersion("v2.1.0-Hybrid")
                .rulePackVersion("2026-07")
                .aiModel("Primary-LLM-Groq")
                .vulnerabilities(vulns)
                .build();
    }

    private VulnerabilityResponse toVulnerabilityResponse(Vulnerability vuln) {
        return VulnerabilityResponse.builder()
                .id(vuln.getId())
                .type(vuln.getVulnerabilityType())
                .severity(vuln.getSeverity().name())
                .description(vuln.getDescription())
                .recommendation(vuln.getRecommendation())
                .fileName(vuln.getFileName())
                .lineNumber(vuln.getLineNumber())
                .codeSnippet(vuln.getCodeSnippet())
                .confidenceScore(vuln.getConfidenceScore())
                .owaspCategory(vuln.getOwaspCategory())
                .cweId(vuln.getCweId())
                .aiExplanation(vuln.getAiExplanation())
                .aiRecommendation(vuln.getAiRecommendation())
                .businessImpact(vuln.getBusinessImpact())
                .secureCodeExample(vuln.getSecureCodeExample())
                .providerName(vuln.getProviderName())
                .aiGeneratedAt(vuln.getAiGeneratedAt() != null ? vuln.getAiGeneratedAt().toString() : null)
                .aiStatus(vuln.getAiStatus() != null ? vuln.getAiStatus().name() : null)
                .enriched(vuln.getAiExplanation() != null)
                .build();
    }
}
