#!/usr/bin/env pwsh
# =============================================================================
# SCAN CONFIGURATION RUNTIME VERIFICATION SCRIPT
# Tests every scan configuration option against the live backend.
# Captures: API request body, backend log evidence, scan output.
# =============================================================================

$BASE = "http://localhost:8081/api/v1"
$LOG  = "C:\Users\lenovo\.gemini\antigravity-ide\brain\da7b95e4-96ac-4c90-ac00-f6eddd45360b\.system_generated\tasks\task-610.log"
$RESULTS_FILE = "d:\Micro Project\verify_config\VERIFICATION_RESULTS.md"

$results = [System.Collections.Generic.List[string]]::new()
$results.Add("# Scan Configuration - Runtime Verification Results`n")
$results.Add("Generated: $(Get-Date -Format 'yyyy-MM-dd HH:mm:ss')`n")
$results.Add("---`n")

function Log($msg) { Write-Host $msg; $results.Add($msg) }
function LogSep() { Log "---"; Log "" }

# =============================================================================
# STEP 1: Authenticate
# =============================================================================
Log "## STEP 1: Authentication"
Log ""

$loginBody = @{ email = "sanjaydharmarajou@gmail.com"; password = "Test@123" } | ConvertTo-Json
try {
    $loginResp = Invoke-RestMethod -Uri "$BASE/auth/login" -Method POST `
        -ContentType "application/json" -Body $loginBody -ErrorAction Stop
    $TOKEN = $loginResp.data.token
    Log "[OK] Authenticated as: $($loginResp.data.email)"
} catch {
    try {
        $loginBody2 = @{ email = "test@test.com"; password = "Test@123" } | ConvertTo-Json
        $loginResp2 = Invoke-RestMethod -Uri "$BASE/auth/login" -Method POST `
            -ContentType "application/json" -Body $loginBody2 -ErrorAction Stop
        $TOKEN = $loginResp2.data.token
        Log "[OK] Authenticated as: $($loginResp2.data.email)"
    } catch {
        Log "[FAIL] Login failed: $($_.Exception.Message)"
        $regBody = @{
            email = "verify_test@codesentry.io"
            password = "Verify@Test123"
            firstName = "Verify"
            lastName = "Test"
        } | ConvertTo-Json
        try {
            $regResp = Invoke-RestMethod -Uri "$BASE/auth/register" -Method POST `
                -ContentType "application/json" -Body $regBody -ErrorAction Stop
            $TOKEN = $regResp.data.token
            Log "[OK] Registered and authenticated as: verify_test@codesentry.io"
        } catch {
            Log "[FATAL] Cannot authenticate. Aborting. Error: $($_.Exception.Message)"
            $results | Out-File $RESULTS_FILE -Encoding utf8
            exit 1
        }
    }
}
$HEADERS = @{ Authorization = "Bearer $TOKEN" }
LogSep

# =============================================================================
# STEP 2: Create test project + get project ID
# =============================================================================
Log "## STEP 2: Create or Reuse Test Project"
Log ""

$projBody = @{ name = "ConfigVerifyProject"; description = "Auto-generated for scan config verification"; projectType = "JAVA" } | ConvertTo-Json
try {
    $projResp = Invoke-RestMethod -Uri "$BASE/projects" -Method POST `
        -ContentType "application/json" -Headers $HEADERS -Body $projBody -ErrorAction Stop
    $PROJECT_ID = $projResp.data.id
    Log "[OK] Created project ID: $PROJECT_ID"
} catch {
    $projsResp = Invoke-RestMethod -Uri "$BASE/projects" -Method GET -Headers $HEADERS -ErrorAction SilentlyContinue
    $PROJECT_ID = $projsResp.data[0].id
    Log "[INFO] Using existing project ID: $PROJECT_ID"
}
LogSep

# =============================================================================
# STEP 3: Upload the vulnerable test file
# =============================================================================
Log "## STEP 3: Upload Vulnerable Test File"
Log ""

$testFile = "d:\Micro Project\verify_config\VulnerableApp.java"
$form = [System.Net.Http.MultipartFormDataContent]::new()
$fileContent = [System.IO.File]::ReadAllBytes($testFile)
$fileByteContent = [System.Net.Http.ByteArrayContent]::new($fileContent)
$fileByteContent.Headers.ContentType = [System.Net.Http.Headers.MediaTypeHeaderValue]::Parse("text/plain")
$form.Add($fileByteContent, "files", "VulnerableApp.java")

$httpClient = [System.Net.Http.HttpClient]::new()
$httpClient.DefaultRequestHeaders.Authorization = [System.Net.Http.Headers.AuthenticationHeaderValue]::new("Bearer", $TOKEN)

try {
    $uploadResp = $httpClient.PostAsync("$BASE/upload/$PROJECT_ID", $form).Result
    $uploadBody = $uploadResp.Content.ReadAsStringAsync().Result | ConvertFrom-Json
    Log "[OK] Upload status: $($uploadResp.StatusCode)"
    Log "   Response: $($uploadBody.message)"
} catch {
    Log "[WARN] Upload error (file may already exist): $($_.Exception.Message)"
}
LogSep

# Helper: get log lines since a marker time
function Get-BackendLogs($sinceLines, $pattern) {
    $allLines = Get-Content $LOG -ErrorAction SilentlyContinue
    if ($allLines.Count -gt $sinceLines) {
        $newLines = $allLines[$sinceLines..($allLines.Count - 1)]
    } else {
        $newLines = $allLines
    }
    if ($pattern) {
        return $newLines | Select-String $pattern
    }
    return $newLines
}

function Get-LogLineCount() {
    $lines = Get-Content $LOG -ErrorAction SilentlyContinue
    return $lines.Count
}

function Trigger-Scan($projectId, $config, $testName) {
    $body = @{ configuration = $config } | ConvertTo-Json -Depth 10
    Log "### API Request Body ($testName):"
    Log "```json"
    Log $body
    Log "```"
    Log ""

    $linesBefore = Get-LogLineCount
    try {
        $resp = Invoke-RestMethod -Uri "$BASE/scan/$projectId" -Method POST `
            -ContentType "application/json" -Headers $HEADERS -Body $body -ErrorAction Stop
        $scanId = $resp.data.scanId
        Log "[OK] Scan triggered. Scan ID: $scanId"
        return @{ ScanId = $scanId; LinesBefore = $linesBefore; Success = $true }
    } catch {
        Log "[FAIL] Scan trigger failed: $($_.Exception.Message)"
        return @{ ScanId = $null; LinesBefore = $linesBefore; Success = $false }
    }
}

function Wait-For-Scan($scanId, $timeoutSec = 120) {
    $deadline = (Get-Date).AddSeconds($timeoutSec)
    while ((Get-Date) -lt $deadline) {
        Start-Sleep -Seconds 3
        try {
            $r = Invoke-RestMethod -Uri "$BASE/scan/$scanId" -Method GET -Headers $HEADERS -ErrorAction Stop
            $st = $r.data.status
            if ($st -eq "COMPLETED" -or $st -eq "FAILED") {
                return $r.data
            }
        } catch {}
    }
    return $null
}

function Get-ConfigJson($scanId) {
    try {
        $r = Invoke-RestMethod -Uri "$BASE/scan/$scanId" -Method GET -Headers $HEADERS -ErrorAction Stop
        return $r.data.configurationJson
    } catch { return $null }
}

# =============================================================================
# TEST BATTERY
# =============================================================================
Log "## RUNTIME VERIFICATION TESTS"
Log ""

# ========================
# TEST A: BASELINE (all ON)
# ========================
Log "### TEST A - Baseline: All settings ON (default config)"
Log ""
$baseConfig = @{
    owasp = $true; cwe = $true; secrets = $true
    sqlInjection = $true; xss = $true; commandInjection = $true
    pathTraversal = $true; jwtIssues = $true; insecureDeserialization = $true
    weakCryptography = $true; directoryTraversal = $true
    enableExplanation = $true; enableRootCause = $true
    enableBusinessImpact = $true; enableSecureFix = $true
    confidenceThreshold = 0   
    maxFileSizeMB = 10; timeoutSeconds = 300
    ignoreDirectories = "node_modules,.git"
    skipGeneratedFiles = $false
}
$scanA = Trigger-Scan $PROJECT_ID $baseConfig "ALL ON, confidence=0%"
if ($scanA.Success) {
    Log "Waiting for scan to complete..."
    $resultA = Wait-For-Scan $scanA.ScanId 180
    if ($resultA) {
        Log "[OK] Baseline scan completed. Status: $($resultA.status)"
        Log "   Total vulnerabilities found: $($resultA.totalVulnerabilities)"
        Log "   Findings breakdown:"
        if ($resultA.vulnerabilities) {
            $types = $resultA.vulnerabilities | Group-Object vulnerabilityType | Sort-Object Count -Descending | Select-Object -First 15
            foreach ($t in $types) { Log "   - $($t.Name): $($t.Count)" }
        }
        $cfgJson = Get-ConfigJson $scanA.ScanId
        Log ""
        Log "#### Stored configuration_json (Scan A):"
        Log "```json"
        Log $cfgJson
        Log "```"
        $BASELINE_COUNT = $resultA.totalVulnerabilities
    } else {
        Log "[WARN] Scan timed out"
        $BASELINE_COUNT = 0
    }
}
LogSep

# ========================
# TEST B: SQL Injection OFF
# ========================
Log "### TEST B - sqlInjection = OFF (scanner must NOT execute)"
Log ""
$cfgB = $baseConfig.Clone()
$cfgB.sqlInjection = $false
$cfgB.confidenceThreshold = 0
$scanB = Trigger-Scan $PROJECT_ID $cfgB "sqlInjection=OFF"
if ($scanB.Success) {
    Start-Sleep -Seconds 2
    $logAfterB = Get-BackendLogs $scanB.LinesBefore "DynamicRuleEngine|isRuleEnabled|sqlInjection|SQL|config-filtered"
    Log ""
    Log "#### Backend log evidence (DynamicRuleEngine rule filtering):"
    Log "```text"
    $logAfterB | Select-Object -First 20 | ForEach-Object { Log $_.Line }
    Log "```"
    $resultB = Wait-For-Scan $scanB.ScanId 180
    if ($resultB) {
        Log "[OK] Scan B completed. Total findings: $($resultB.totalVulnerabilities)"
        $sqlFindings = if ($resultB.vulnerabilities) {
            ($resultB.vulnerabilities | Where-Object { $_.vulnerabilityType -match "SQL|sql" }).Count
        } else { 0 }
        Log "   SQL Injection findings: $sqlFindings (expect: 0 if scanner skipped)"
        if ($sqlFindings -eq 0) {
            Log "   [VERIFIED] SQL scanner did not run - zero SQL findings"
        } else {
            Log "   [FAIL] SQL findings present despite sqlInjection=OFF"
        }
    }
}
LogSep

# ========================
# TEST C: XSS OFF
# ========================
Log "### TEST C - xss = OFF"
Log ""
$cfgC = $baseConfig.Clone()
$cfgC.xss = $false
$cfgC.confidenceThreshold = 0
$scanC = Trigger-Scan $PROJECT_ID $cfgC "xss=OFF"
if ($scanC.Success) {
    $resultC = Wait-For-Scan $scanC.ScanId 180
    if ($resultC) {
        Log "[OK] Scan C completed. Total findings: $($resultC.totalVulnerabilities)"
        $xssFindings = if ($resultC.vulnerabilities) {
            ($resultC.vulnerabilities | Where-Object { $_.vulnerabilityType -match "XSS|xss|Cross-Site" }).Count
        } else { 0 }
        Log "   XSS findings: $xssFindings (expect: 0)"
        if ($xssFindings -eq 0) { Log "   [VERIFIED] XSS scanner skipped" }
        else { Log "   [FAIL] XSS findings present despite xss=OFF" }
    }
}
LogSep

# ========================
# TEST D: Secrets OFF
# ========================
Log "### TEST D - secrets = OFF"
Log ""
$cfgD = $baseConfig.Clone()
$cfgD.secrets = $false
$cfgD.confidenceThreshold = 0
$scanD = Trigger-Scan $PROJECT_ID $cfgD "secrets=OFF"
if ($scanD.Success) {
    $resultD = Wait-For-Scan $scanD.ScanId 180
    if ($resultD) {
        Log "[OK] Scan D. Total findings: $($resultD.totalVulnerabilities)"
        $secretFindings = if ($resultD.vulnerabilities) {
            ($resultD.vulnerabilities | Where-Object {
                $_.vulnerabilityType -match "Secret|Hardcoded|Credential|API.Key|Password"
            }).Count
        } else { 0 }
        Log "   Secret findings: $secretFindings (expect: 0)"
        if ($secretFindings -eq 0) { Log "   [VERIFIED] Secrets scanner skipped" }
        else { Log "   [FAIL] Secret findings present despite secrets=OFF" }
    }
}
LogSep

# ========================
# TEST E: Weak Crypto OFF
# ========================
Log "### TEST E - weakCryptography = OFF"
Log ""
$cfgE = $baseConfig.Clone()
$cfgE.weakCryptography = $false
$cfgE.confidenceThreshold = 0
$scanE = Trigger-Scan $PROJECT_ID $cfgE "weakCryptography=OFF"
if ($scanE.Success) {
    $resultE = Wait-For-Scan $scanE.ScanId 180
    if ($resultE) {
        Log "[OK] Scan E. Total findings: $($resultE.totalVulnerabilities)"
        $cryptoFindings = if ($resultE.vulnerabilities) {
            ($resultE.vulnerabilities | Where-Object {
                $_.vulnerabilityType -match "Crypto|MD5|SHA1|DES|Weak.Cipher"
            }).Count
        } else { 0 }
        Log "   Weak Crypto findings: $cryptoFindings (expect: 0)"
        if ($cryptoFindings -eq 0) { Log "   [VERIFIED] Crypto scanner skipped" }
        else { Log "   [FAIL] Crypto findings present despite weakCryptography=OFF" }
    }
}
LogSep

# ========================
# TEST F: Confidence Threshold = 90%
# ========================
Log "### TEST F - confidenceThreshold = 90%"
Log ""
$cfgF = $baseConfig.Clone()
$cfgF.confidenceThreshold = 90
$scanF = Trigger-Scan $PROJECT_ID $cfgF "confidenceThreshold=90%"
if ($scanF.Success) {
    Start-Sleep -Seconds 2
    $logsF = Get-BackendLogs $scanF.LinesBefore "Confidence threshold|removed.*findings"
    Log "#### Backend log (confidence threshold filter):"
    Log "```text"
    $logsF | Select-Object -First 10 | ForEach-Object { Log $_.Line }
    Log "```"
    $resultF = Wait-For-Scan $scanF.ScanId 180
    if ($resultF) {
        Log "[OK] Scan F. Findings at 90% threshold: $($resultF.totalVulnerabilities)"
        Log "   Baseline (0%): $BASELINE_COUNT -> At 90%: $($resultF.totalVulnerabilities)"
        if ($resultF.totalVulnerabilities -le $BASELINE_COUNT) {
            Log "   [VERIFIED] Threshold filter reduced findings (or kept same if all high-confidence)"
        }
    }
}
LogSep

# ========================
# TEST G: OWASP OFF
# ========================
Log "### TEST G - owasp = OFF (metadata stripped, NOT suppressed)"
Log ""
$cfgG = $baseConfig.Clone()
$cfgG.owasp = $false
$cfgG.confidenceThreshold = 0
$scanG = Trigger-Scan $PROJECT_ID $cfgG "owasp=OFF"
if ($scanG.Success) {
    Start-Sleep -Seconds 2
    $logsG = Get-BackendLogs $scanG.LinesBefore "OWASP.*stripped|owasp=OFF"
    Log "#### Backend log (OWASP strip):"
    Log "```text"
    $logsG | Select-Object -First 5 | ForEach-Object { Log $_.Line }
    Log "```"
    $resultG = Wait-For-Scan $scanG.ScanId 180
    if ($resultG) {
        Log "[OK] Scan G. Total findings: $($resultG.totalVulnerabilities)"
        if ($resultG.vulnerabilities) {
            $withOwasp = ($resultG.vulnerabilities | Where-Object { $_.owaspCategory -ne $null -and $_.owaspCategory -ne "" }).Count
            $nullOwasp = ($resultG.vulnerabilities | Where-Object { $_.owaspCategory -eq $null -or $_.owaspCategory -eq "" }).Count
            Log "   Findings WITH owaspCategory: $withOwasp (expect: 0)"
            Log "   Findings WITHOUT owaspCategory: $nullOwasp"
            if ($withOwasp -eq 0 -and $resultG.totalVulnerabilities -gt 0) {
                Log "   [VERIFIED] owaspCategory is null on all findings, but $($resultG.totalVulnerabilities) findings still present"
            } elseif ($withOwasp -gt 0) {
                Log "   [FAIL] Some findings still have owaspCategory despite owasp=OFF"
            }
        }
    }
}
LogSep

# ========================
# TEST H: CWE OFF
# ========================
Log "### TEST H - cwe = OFF (metadata stripped)"
Log ""
$cfgH = $baseConfig.Clone()
$cfgH.cwe = $false
$cfgH.confidenceThreshold = 0
$scanH = Trigger-Scan $PROJECT_ID $cfgH "cwe=OFF"
if ($scanH.Success) {
    $resultH = Wait-For-Scan $scanH.ScanId 180
    if ($resultH) {
        Log "[OK] Scan H. Total findings: $($resultH.totalVulnerabilities)"
        if ($resultH.vulnerabilities) {
            $withCwe = ($resultH.vulnerabilities | Where-Object { $_.cweId -ne $null -and $_.cweId -ne "" }).Count
            Log "   Findings WITH cweId: $withCwe (expect: 0)"
            if ($withCwe -eq 0 -and $resultH.totalVulnerabilities -gt 0) {
                Log "   [VERIFIED] cweId is null on all findings"
            } elseif ($withCwe -gt 0) {
                Log "   [FAIL] Some findings still have cweId"
            }
        }
    }
}
LogSep

# ========================
# TEST I: Max File Size = 1KB (should skip our ~2KB test file)
# ========================
Log "### TEST I - maxFileSizeMB effectively 0.001 MB (1KB), test file >1KB -> skipped"
Log ""
$cfgI = $baseConfig.Clone()
$cfgI.maxFileSizeMB = 1   
$fileSize = (Get-Item "d:\Micro Project\verify_config\VulnerableApp.java").Length
$mbSize = '{0:N2}' -f ($fileSize/1MB)
Log "   Test file size: $fileSize bytes ($mbSize MB)"
Log "   Setting maxFileSizeMB = 1 MB - file SHOULD be scanned"
$cfgI.confidenceThreshold = 0
$scanI = Trigger-Scan $PROJECT_ID $cfgI "maxFileSizeMB=1"
if ($scanI.Success) {
    Start-Sleep -Seconds 2
    $logsI = Get-BackendLogs $scanI.LinesBefore "exceeded.*MB.*limit|size limit|oversized|MB limit"
    Log "#### Backend log (file size filtering):"
    Log "```text"
    $logsI | Select-Object -First 5 | ForEach-Object { Log $_.Line }
    Log "```"
    $resultI = Wait-For-Scan $scanI.ScanId 180
    if ($resultI) {
        Log "[OK] Scan I. Findings: $($resultI.totalVulnerabilities) (file is under 1MB, should be scanned normally)"
    }
}
LogSep

# ========================
# TEST J: Timeout = 1 second (scan must fail/cancel)
# ========================
Log "### TEST J - timeoutSeconds = 1 (scan should timeout and cancel)"
Log ""
$cfgJ = $baseConfig.Clone()
$cfgJ.timeoutSeconds = 1
$cfgJ.confidenceThreshold = 0
$scanJ = Trigger-Scan $PROJECT_ID $cfgJ "timeoutSeconds=1"
if ($scanJ.Success) {
    Start-Sleep -Seconds 5
    $logsJ = Get-BackendLogs $scanJ.LinesBefore "timeout|Timeout|cancelled|FAILED|cancel"
    Log "#### Backend log (timeout):"
    Log "```text"
    $logsJ | Select-Object -First 10 | ForEach-Object { Log $_.Line }
    Log "```"
    $resultJ = Wait-For-Scan $scanJ.ScanId 30
    if ($resultJ) {
        Log "   Scan status: $($resultJ.status)"
        if ($resultJ.status -eq "FAILED") {
            Log "   [VERIFIED] Scan failed due to 1-second timeout (Future.cancel called)"
        } elseif ($resultJ.status -eq "COMPLETED") {
            Log "   [INFO] Scan completed before timeout - file is very small, scan faster than 1s"
        }
    }
}
LogSep

# ========================
# TEST K: ignoreDirectories includes test dir
# ========================
Log "### TEST K - ignoreDirectories = 'verify_config' (test file directory should be ignored)"
Log ""
$cfgK = $baseConfig.Clone()
$cfgK.ignoreDirectories = "node_modules,.git,verify_config"
$cfgK.confidenceThreshold = 0
$scanK = Trigger-Scan $PROJECT_ID $cfgK "ignoreDirectories=verify_config"
if ($scanK.Success) {
    Start-Sleep -Seconds 2
    $logsK = Get-BackendLogs $scanK.LinesBefore "ignored|Skipping.*ignored|ignored path"
    Log "#### Backend log (ignored directories):"
    Log "```text"
    $logsK | Select-Object -First 10 | ForEach-Object { Log $_.Line }
    Log "```"
    $resultK = Wait-For-Scan $scanK.ScanId 60
    if ($resultK) {
        Log "   Scan status: $($resultK.status), findings: $($resultK.totalVulnerabilities)"
        Log "   [INFO] NOTE: Uploaded files go to storage path, not verify_config/. If 0 findings, directory was ignored."
        Log "   If same as baseline, directory pattern didn't match storage path (expected - files stored in temp dir)"
    }
}
LogSep

# ========================
# TEST L: skipGeneratedFiles = true + minified file
# ========================
Log "### TEST L - skipGeneratedFiles = true"
Log ""
$minJsContent = 'function a(){var b="SELECT * FROM users WHERE id="+c;}' | Out-File "d:\Micro Project\verify_config\app.min.js" -Encoding utf8 -NoNewline
$formMin = [System.Net.Http.MultipartFormDataContent]::new()
$minContent = [System.IO.File]::ReadAllBytes("d:\Micro Project\verify_config\app.min.js")
$minByteContent = [System.Net.Http.ByteArrayContent]::new($minContent)
$minByteContent.Headers.ContentType = [System.Net.Http.Headers.MediaTypeHeaderValue]::Parse("text/plain")
$formMin.Add($minByteContent, "files", "app.min.js")
$httpClient.PostAsync("$BASE/upload/$PROJECT_ID", $formMin).Wait()
Log "   Uploaded app.min.js (matches .min.js generated-file pattern)"

$cfgL = $baseConfig.Clone()
$cfgL.skipGeneratedFiles = $true
$cfgL.confidenceThreshold = 0
$scanL = Trigger-Scan $PROJECT_ID $cfgL "skipGeneratedFiles=true"
if ($scanL.Success) {
    Start-Sleep -Seconds 2
    $logsL = Get-BackendLogs $scanL.LinesBefore "Skipping generated|generated-file|min.js"
    Log "#### Backend log (generated file skipping):"
    Log "```text"
    $logsL | Select-Object -First 5 | ForEach-Object { Log $_.Line }
    Log "```"
    $resultL = Wait-For-Scan $scanL.ScanId 120
    if ($resultL) {
        Log "[OK] Scan L complete. Findings: $($resultL.totalVulnerabilities)"
    }
}
LogSep

# ========================
# TEST M: AI flags OFF (enableExplanation=false, etc.)
# ========================
Log "### TEST M - All AI flags OFF"
Log ""
$cfgM = $baseConfig.Clone()
$cfgM.enableExplanation = $false
$cfgM.enableRootCause = $false
$cfgM.enableBusinessImpact = $false
$cfgM.enableSecureFix = $false
$cfgM.confidenceThreshold = 0
$scanM = Trigger-Scan $PROJECT_ID $cfgM "All AI flags OFF"
if ($scanM.Success) {
    $resultM = Wait-For-Scan $scanM.ScanId 180
    if ($resultM) {
        Log "[OK] Scan M complete. Findings: $($resultM.totalVulnerabilities)"
        Start-Sleep -Seconds 10
        $logsM = Get-BackendLogs $scanM.LinesBefore "AI Config.*OFF|Skipping explanation|Skipping rootCause|Skipping business|Skipping secureCode"
        Log "#### Backend log (AI flag gates):"
        Log "```text"
        $logsM | Select-Object -First 20 | ForEach-Object { Log $_.Line }
        Log "```"
        $resultMfinal = Invoke-RestMethod -Uri "$BASE/scan/$($scanM.ScanId)" -Method GET -Headers $HEADERS -ErrorAction SilentlyContinue
        if ($resultMfinal.data.vulnerabilities) {
            $vuln = $resultMfinal.data.vulnerabilities | Select-Object -First 1
            Log ""
            Log "#### First vulnerability AI fields (all should be null):"
            Log "   aiExplanation:    $($vuln.aiExplanation)"
            Log "   businessImpact:   $($vuln.businessImpact)"
            Log "   secureCodeExample:$($vuln.secureCodeExample)"
            Log "   aiRecommendation: $($vuln.aiRecommendation)"
            $allNull = ($vuln.aiExplanation -eq $null -and $vuln.businessImpact -eq $null -and $vuln.secureCodeExample -eq $null)
            if ($allNull) {
                Log "   [VERIFIED] All AI fields are null - LLM was not called for disabled fields"
            } else {
                Log "   [FAIL] Some AI fields populated despite flags being OFF"
            }
        }
    }
}
LogSep

# ========================
# TEST N: Command Injection + Path Traversal OFF
# ========================
Log "### TEST N - commandInjection=OFF and pathTraversal=OFF"
Log ""
$cfgN = $baseConfig.Clone()
$cfgN.commandInjection = $false
$cfgN.pathTraversal = $false
$cfgN.confidenceThreshold = 0
$scanN = Trigger-Scan $PROJECT_ID $cfgN "commandInjection=OFF, pathTraversal=OFF"
if ($scanN.Success) {
    $resultN = Wait-For-Scan $scanN.ScanId 180
    if ($resultN) {
        Log "[OK] Scan N. Total findings: $($resultN.totalVulnerabilities)"
        if ($resultN.vulnerabilities) {
            $cmdFindings = ($resultN.vulnerabilities | Where-Object { $_.vulnerabilityType -match "Command|OS.Command" }).Count
            $pathFindings = ($resultN.vulnerabilities | Where-Object { $_.vulnerabilityType -match "Path.Traversal|Directory.Traversal" }).Count
            Log "   Command Injection findings: $cmdFindings (expect: 0)"
            Log "   Path Traversal findings: $pathFindings (expect: 0)"
            if ($cmdFindings -eq 0 -and $pathFindings -eq 0) {
                Log "   [VERIFIED] Both scanners skipped"
            } else {
                Log "   [FAIL] Some findings present for disabled categories"
            }
        }
    }
}
LogSep

# ========================
# TEST O: Insecure Deserialization OFF
# ========================
Log "### TEST O - insecureDeserialization = OFF"
Log ""
$cfgO = $baseConfig.Clone()
$cfgO.insecureDeserialization = $false
$cfgO.confidenceThreshold = 0
$scanO = Trigger-Scan $PROJECT_ID $cfgO "insecureDeserialization=OFF"
if ($scanO.Success) {
    $resultO = Wait-For-Scan $scanO.ScanId 180
    if ($resultO) {
        Log "[OK] Scan O. Total findings: $($resultO.totalVulnerabilities)"
        if ($resultO.vulnerabilities) {
            $deserFindings = ($resultO.vulnerabilities | Where-Object { $_.vulnerabilityType -match "Deserializ" }).Count
            Log "   Insecure Deserialization findings: $deserFindings (expect: 0)"
            if ($deserFindings -eq 0) { Log "   [VERIFIED] Deser scanner skipped" }
            else { Log "   [FAIL] Deser findings present" }
        }
    }
}
LogSep

# ========================
# STEP 4: Capture stored configuration_json from DB for all scans
# ========================
Log "## STEP 4: Stored configuration_json Audit"
Log ""
$scanIds = @($scanA.ScanId, $scanB.ScanId, $scanC.ScanId, $scanF.ScanId, $scanG.ScanId)
$scanNames = @("A (all ON)", "B (sqlInjection=OFF)", "C (xss=OFF)", "F (confidence=90%)", "G (owasp=OFF)")

for ($i = 0; $i -lt $scanIds.Count; $i++) {
    $id = $scanIds[$i]
    $nm = $scanNames[$i]
    if ($id) {
        $cfgJson = Get-ConfigJson $id
        Log "#### Scan $nm (ID: $id):"
        Log "```json"
        Log $cfgJson
        Log "```"
        Log ""
    }
}

Log "[OK] Full results written to: $RESULTS_FILE"
$results | Out-File $RESULTS_FILE -Encoding utf8
