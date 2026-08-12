path = 'D:/Micro Project/src/main/java/com/sanjay/aisecurity/service/ScanServiceImpl.java'
with open(path, 'r', encoding='utf-8') as f:
    text = f.read()

start_sig = 'public void executeScanAsync(Long scanId, Long projectId, ScanConfigurationDto configuration) {'
start_idx = text.find(start_sig)

if start_idx != -1:
    brace_count = 0
    in_method = False
    end_idx = -1
    for i in range(start_idx, len(text)):
        if text[i] == '{':
            brace_count += 1
            in_method = True
        elif text[i] == '}':
            brace_count -= 1
        
        if in_method and brace_count == 0:
            end_idx = i + 1
            break

    if end_idx != -1:
        method_body = text[start_idx:end_idx]
        
        # Now find the try block
        try_idx = method_body.find('try {')
        catch_idx = method_body.rfind('} catch (Exception e) {')
        
        if try_idx != -1 and catch_idx != -1:
            prefix = method_body[:try_idx + 5]
            body = method_body[try_idx + 5:catch_idx]
            suffix = method_body[catch_idx:]
            
            split_point = body.find('long tScanStart = System.currentTimeMillis();')
            if split_point != -1:
                discovery_part = body[:split_point]
                pipeline_part = body[split_point:]
                
                idx_ai = pipeline_part.find('aiEnrichmentService.enrichScanSummaryAsync(')
                idx_ai_end = pipeline_part.find(';', idx_ai)
                enrich_call = pipeline_part[idx_ai:idx_ai_end+1]
                
                new_enrich_call = '''if (isQuickScan) {
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
            }'''
                pipeline_part = pipeline_part.replace(enrich_call, new_enrich_call)
                
                new_executeScanAsync = prefix + discovery_part + '''
            executeScanPipeline(scanId, projectId, discoveredFiles, totalFiles, tStart, tDiscoveryStart, tDiscoveryEnd, config, scanHistory, scanTaskExecutor, false);
        ''' + suffix
                
                new_pipeline_method = '''
    private ScanSummaryResponse executeScanPipeline(Long scanId, Long projectId, List<com.sanjay.aisecurity.analyzer.pipeline.DiscoveredFile> discoveredFiles, 
                                     int totalFiles, long tStart, long tDiscoveryStart, long tDiscoveryEnd, 
                                     ScanConfigurationDto config, ScanHistory scanHistory, 
                                     java.util.concurrent.ExecutorService scanTaskExecutor, boolean isQuickScan) {
        ''' + pipeline_part + '''
        if (isQuickScan) {
            return getScanResult(scanId);
        }
        return null;
    }
'''
                new_text = text[:start_idx] + new_executeScanAsync + "\n" + new_pipeline_method + text[end_idx:]
                with open(path, 'w', encoding='utf-8') as f:
                    f.write(new_text)
                print('Refactored successfully!')
            else:
                print('Split point not found')
        else:
            print('try or catch not found')
    else:
        print('end brace not found')
else:
    print('start_sig not found')
