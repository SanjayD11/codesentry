import re

path = 'D:/Micro Project/src/main/java/com/sanjay/aisecurity/service/ScanServiceImpl.java'
with open(path, 'r', encoding='utf-8') as f:
    text = f.read()

new_method = '''
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
        com.sanjay.aisecurity.enums.Language detectedLang = com.sanjay.aisecurity.analyzer.pipeline.LanguageDetector.detect(langHint, project.getProjectType());
        if (detectedLang != com.sanjay.aisecurity.enums.Language.UNKNOWN) {
            ext = "." + detectedLang.name().toLowerCase();
        }
        String filename = (request.getFilename() != null && !request.getFilename().isBlank())
                ? request.getFilename() : ("QuickScan" + ext);

        int lineCount = request.getSourceCode() != null ? request.getSourceCode().split("\\r?\\n").length : 0;

        ScanHistory scan = ScanHistory.builder()
                .project(project)
                .status(ScanStatus.RUNNING)
                .scanType(com.sanjay.aisecurity.enums.ScanType.QUICK_SCAN)
                .snippetLanguage(langHint)
                .snippetFilename(filename)
                .snippetLines(lineCount)
                .progressPercentage(0)
                .scanStart(LocalDateTime.now())
                .configurationJson(request.getConfiguration() != null ? request.getConfiguration().toJson() : "{}")
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
                
                // Automatically generate PDF report
                try {
                    reportService.generateReport(scan.getId());
                } catch (Exception e) {
                    log.error("[Quick Scan] Failed to automatically generate PDF report for scan {}", scan.getId(), e);
                }

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
'''

idx = text.rfind('    // =========================================================================')
if idx != -1:
    new_text = text[:idx] + new_method + '\n' + text[idx:]
    with open(path, 'w', encoding='utf-8') as f:
        f.write(new_text)
    print("Method added.")
else:
    print("Could not find insertion point.")
