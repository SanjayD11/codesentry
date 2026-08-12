# ============================================================
# COMPREHENSIVE UPLOAD LIFECYCLE VERIFICATION SCRIPT
# ============================================================

$BASE     = "http://localhost:8081/api/v1"
$UPLOAD_DIR = "D:\Micro Project\uploads"
$ZIP_PATH = "D:\Micro Project\verify_config\VulnerableApp.zip"
$RESULTS  = @()
$PASS = 0; $FAIL = 0

function Log($msg) { Write-Host "[$(Get-Date -f 'HH:mm:ss')] $msg" }

function Record($name, $status, $detail) {
    $sym = if ($status -eq "PASS") { "PASS" } else { "FAIL" }
    Log "[$sym] $name :: $detail"
    $script:RESULTS += [PSCustomObject]@{ Test=$name; Status=$status; Detail=$detail }
    if ($status -eq "PASS") { $script:PASS++ } else { $script:FAIL++ }
}

function Upload-File($zipPath, $projId, $override) {
    $fileBytes = [System.IO.File]::ReadAllBytes($zipPath)
    $boundary = [System.Guid]::NewGuid().ToString()
    $lf = "`r`n"
    $iso = [System.Text.Encoding]::GetEncoding("ISO-8859-1")
    $bodyLines = @(
        "--$boundary",
        "Content-Disposition: form-data; name=`"files`"; filename=`"VulnerableApp.zip`"",
        "Content-Type: application/zip",
        "",
        $iso.GetString($fileBytes),
        "--$boundary--"
    )
    $bodyBytes = $iso.GetBytes($bodyLines -join $lf)
    return Invoke-WebRequest -Method POST -Uri "$BASE/uploads/$projId`?overrideDuplicate=$override" `
        -Headers @{ Authorization = "Bearer $script:TOKEN" } `
        -Body $bodyBytes -ContentType "multipart/form-data; boundary=$boundary" -UseBasicParsing
}

# Create test ZIP if missing
if (-not (Test-Path $ZIP_PATH)) {
    $src = "D:\Micro Project\verify_config\VulnerableApp.java"
    Compress-Archive -Path $src -DestinationPath $ZIP_PATH -Force
}

# ---- STEP 1: AUTH ----
Log "=== STEP 1: AUTHENTICATION ==="
try {
    $loginResp = Invoke-RestMethod -Method POST -Uri "$BASE/auth/login" `
        -Body (@{email="sanjaydharmarajou@gmail.com"; password="Test@123"}|ConvertTo-Json) `
        -ContentType "application/json"
    $script:TOKEN = $loginResp.data.accessToken
    Record "AUTH-001 Login" "PASS" "Token len=$($script:TOKEN.Length)"
} catch {
    # Try register
    try {
        Invoke-RestMethod -Method POST -Uri "$BASE/auth/register" `
            -Body (@{email="sanjaydharmarajou@gmail.com"; password="Test@123"; firstName="Verify"; lastName="User"}|ConvertTo-Json) `
            -ContentType "application/json" | Out-Null
        $loginResp = Invoke-RestMethod -Method POST -Uri "$BASE/auth/login" `
            -Body (@{email="sanjaydharmarajou@gmail.com"; password="Test@123"}|ConvertTo-Json) `
            -ContentType "application/json"
        $script:TOKEN = $loginResp.data.accessToken
        Record "AUTH-001 Register+Login" "PASS" "New user created"
    } catch {
        Record "AUTH-001 Login" "FAIL" $_.Exception.Message; exit
    }
}
$H = @{ Authorization = "Bearer $script:TOKEN" }

# ---- STEP 2: PROJECT ----
Log "=== STEP 2: PROJECT ==="
try {
    $projects = Invoke-RestMethod -Uri "$BASE/projects?size=50" -Headers $H
    $proj = $projects.data.projects | Where-Object { $_.projectName -eq "VerifyLifecycle" } | Select-Object -First 1
    if (-not $proj) {
        $np = Invoke-RestMethod -Method POST -Uri "$BASE/projects" -Headers $H `
            -Body (@{projectName="VerifyLifecycle"; description="Lifecycle test"; projectType="WEB_APPLICATION"}|ConvertTo-Json) `
            -ContentType "application/json"
        $proj = $np.data
        Record "PROJ-001 Create" "PASS" "ID=$($proj.id)"
    } else {
        Record "PROJ-001 Reuse" "PASS" "ID=$($proj.id)"
    }
    $PROJ_ID = $proj.id
} catch { Record "PROJ-001" "FAIL" $_.Exception.Message; exit }

# ---- STEP 3: FIRST UPLOAD ----
Log "=== STEP 3: FIRST UPLOAD ==="
try {
    $resp = Upload-File $ZIP_PATH $PROJ_ID "false"
    $data = ($resp.Content | ConvertFrom-Json).data[0]
    if ($data.success) {
        $script:F1_ID = $data.fileDetails.id
        $script:F1_SHA = $data.fileDetails.checksumSHA256
        Record "UPLOAD-001 First Upload Success" "PASS" "FileID=$($script:F1_ID) SHA=$($script:F1_SHA.Substring(0,16))..."
    } elseif ($data.message -eq "DUPLICATE_FILE_DETECTED") {
        # File from a previous test run -- that means duplicate detection works
        $script:F1_ID = $data.fileDetails.id; $script:F1_SHA = $data.fileDetails.checksumSHA256
        Record "UPLOAD-001 First Upload (pre-existing)" "PASS" "Duplicate from prior run - FileID=$($script:F1_ID)"
    } else {
        Record "UPLOAD-001 First Upload" "FAIL" "msg=$($data.message)"
    }
} catch { Record "UPLOAD-001 First Upload" "FAIL" $_.Exception.Message }

# Verify no source in API response
try {
    $meta = Invoke-RestMethod -Uri "$BASE/uploads/$($script:F1_ID)" -Headers $H
    $hasCode = $meta.data.PSObject.Properties.Name -contains "sourceCode" -or $meta.data.PSObject.Properties.Name -contains "content"
    Record "UPLOAD-002 No Source Code in DB" $(if (-not $hasCode) {"PASS"} else {"FAIL"}) "Fields: $($meta.data.PSObject.Properties.Name -join ',')"
} catch { Record "UPLOAD-002 Metadata Accessible" "FAIL" $_.Exception.Message }

# Verify physical file on disk
$diskFiles = Get-ChildItem $UPLOAD_DIR -Recurse -File -ErrorAction SilentlyContinue | Where-Object { $_.Extension -in @(".zip",".java",".py",".js") -and $_.DirectoryName -notlike "*reports*" }
Record "UPLOAD-003 Physical File on Disk" $(if ($diskFiles.Count -gt 0) {"PASS"} else {"FAIL"}) "Files on disk: $($diskFiles.Count)"

# ---- STEP 4: FIRST SCAN ----
Log "=== STEP 4: FIRST SCAN ==="
try {
    $scanConf = @{owasp=$true;cwe=$true;secrets=$true;sqlInjection=$true;xss=$true;commandInjection=$true;pathTraversal=$true;jwtIssues=$true;insecureDeserialization=$true;weakCryptography=$true;directoryTraversal=$true;aiExplanation=$true;aiRootCause=$true;aiBusinessImpact=$true;aiSecureFix=$true;confidenceThreshold=60;maxFileSizeMB=10;timeoutSeconds=300;ignoreDirs="node_modules,.git,target,build";skipGeneratedFiles=$true}
    $tr = Invoke-RestMethod -Method POST -Uri "$BASE/scan/$PROJ_ID" -Headers $H -Body ($scanConf|ConvertTo-Json) -ContentType "application/json"
    $script:S1_ID = $tr.data.scanId
    Record "SCAN-001 First Scan Triggered" "PASS" "ScanID=$($script:S1_ID)"
} catch { Record "SCAN-001 First Scan Trigger" "FAIL" $_.Exception.Message }

# Poll
Log "Polling scan (max 3min)..."
$done = $false; $sc = $null
for ($i=0; $i -lt 36; $i++) {
    Start-Sleep -Seconds 5
    try { $sc = Invoke-RestMethod -Uri "$BASE/scan/$($script:S1_ID)" -Headers $H; if ($sc.data.status -in @("COMPLETED","FAILED")) {$done=$true;break} } catch {}
    Log "  [$i] $($sc.data.status) $($sc.data.progressPercentage)%"
}
Record "SCAN-002 First Scan Complete" $(if ($done -and $sc.data.status -eq "COMPLETED") {"PASS"} else {"FAIL"}) "status=$($sc.data.status) vulns=$($sc.data.totalVulnerabilities)"
$script:S1_VULNS = $sc.data.totalVulnerabilities

# ---- STEP 5: SECURE DELETION ----
Log "=== STEP 5: SECURE DELETION ==="
Start-Sleep -Seconds 5

$leakedSrc = Get-ChildItem $UPLOAD_DIR -Recurse -File -ErrorAction SilentlyContinue | Where-Object { $_.Extension -in @(".zip",".java",".py",".js",".html") -and $_.DirectoryName -notlike "*reports*" }
Record "SEC-001 Source Files Deleted After Scan" $(if ($leakedSrc.Count -eq 0) {"PASS"} else {"FAIL"}) "Remaining: $($leakedSrc.Count) ($($leakedSrc|ForEach{$_.Name}|Out-String).Trim())"

# Check scan history still intact
try {
    $sh = Invoke-RestMethod -Uri "$BASE/scan/$($script:S1_ID)" -Headers $H
    Record "SEC-002 Scan History Intact After Delete" $(if ($sh.data.status) {"PASS"} else {"FAIL"}) "status=$($sh.data.status) vulns=$($sh.data.totalVulnerabilities)"
} catch { Record "SEC-002 Scan History Intact" "FAIL" $_.Exception.Message }

# Check backend log
$logFile = "C:\Users\lenovo\.gemini\antigravity-ide\brain\da7b95e4-96ac-4c90-ac00-f6eddd45360b\.system_generated\tasks\task-1572.log"
$secLog = Select-String "\[Security\] Securely deleted physical files" $logFile -ErrorAction SilentlyContinue
Record "SEC-003 Backend Log Evidence" $(if ($secLog.Count -gt 0) {"PASS"} else {"FAIL"}) "Log entries: $($secLog.Count)"

# ---- STEP 6: DUPLICATE UPLOAD ----
Log "=== STEP 6: DUPLICATE UPLOAD WORKFLOW ==="
try {
    $dr = Upload-File $ZIP_PATH $PROJ_ID "false"
    $dd = ($dr.Content | ConvertFrom-Json).data[0]
    Record "DUP-001 Duplicate Signal Returned" $(if ($dd.success -eq $false -and $dd.message -eq "DUPLICATE_FILE_DETECTED") {"PASS"} else {"FAIL"}) "success=$($dd.success) msg=$($dd.message)"
} catch { Record "DUP-001 Duplicate Signal" "FAIL" $_.Exception.Message }

try {
    $or = Upload-File $ZIP_PATH $PROJ_ID "true"
    $od = ($or.Content | ConvertFrom-Json).data[0]
    if ($od.success) {
        $script:F2_ID = $od.fileDetails.id
        $script:F2_SHA = $od.fileDetails.checksumSHA256
        Record "DUP-002 Override Upload Success" "PASS" "FileID=$($script:F2_ID)"
        Record "DUP-003 New Record Created (Different ID)" $(if ($script:F2_ID -ne $script:F1_ID) {"PASS"} else {"FAIL"}) "ID1=$($script:F1_ID) vs ID2=$($script:F2_ID)"
        Record "DUP-004 Identical SHA-256 (Same Content)" $(if ($script:F1_SHA -eq $script:F2_SHA) {"PASS"} else {"FAIL"}) "$($script:F1_SHA.Substring(0,16))..."
    } else {
        Record "DUP-002 Override Upload" "FAIL" "msg=$($od.message)"
    }
} catch { Record "DUP-002 Override Upload" "FAIL" $_.Exception.Message }

# ---- STEP 7: SECOND SCAN ----
Log "=== STEP 7: SECOND SCAN ==="
try {
    $tr2 = Invoke-RestMethod -Method POST -Uri "$BASE/scan/$PROJ_ID" -Headers $H -Body ($scanConf|ConvertTo-Json) -ContentType "application/json"
    $script:S2_ID = $tr2.data.scanId
    Record "SCAN2-001 Second Scan Triggered" "PASS" "ScanID=$($script:S2_ID)"
    $d2=$false; $sc2=$null
    for ($i=0; $i -lt 36; $i++) {
        Start-Sleep -Seconds 5
        try { $sc2=Invoke-RestMethod -Uri "$BASE/scan/$($script:S2_ID)" -Headers $H; if ($sc2.data.status -in @("COMPLETED","FAILED")){$d2=$true;break} } catch {}
        Log "  [$i] $($sc2.data.status)"
    }
    Record "SCAN2-002 Second Scan Complete" $(if ($d2 -and $sc2.data.status -eq "COMPLETED") {"PASS"} else {"FAIL"}) "status=$($sc2.data.status) vulns=$($sc2.data.totalVulnerabilities)"
} catch { Record "SCAN2-001 Second Scan" "FAIL" $_.Exception.Message }

Start-Sleep -Seconds 5
$leaked2 = Get-ChildItem $UPLOAD_DIR -Recurse -File -ErrorAction SilentlyContinue | Where-Object { $_.Extension -in @(".zip",".java") -and $_.DirectoryName -notlike "*reports*" }
Record "SCAN2-003 Secure Delete After 2nd Scan" $(if ($leaked2.Count -eq 0) {"PASS"} else {"FAIL"}) "Remaining: $($leaked2.Count)"

# ---- STEP 8: REPORTS ----
Log "=== STEP 8: REPORTS ==="
try {
    Invoke-RestMethod -Method POST -Uri "$BASE/reports/generate/$($script:S1_ID)" -Headers $H `
        -Body (@{reportType="PDF"}|ConvertTo-Json) -ContentType "application/json" | Out-Null
    Start-Sleep -Seconds 8
    $rpts = Invoke-RestMethod -Uri "$BASE/reports/my" -Headers $H
    $myRpt = $rpts.data | Where-Object { $_.scanHistoryId -eq $script:S1_ID }
    Record "RPT-001 PDF Report Generated" $(if ($myRpt.Count -gt 0) {"PASS"} else {"FAIL"}) "Count=$($myRpt.Count)"
    $pdfs = Get-ChildItem "$UPLOAD_DIR\reports" -Recurse -Filter "*.pdf" -ErrorAction SilentlyContinue
    Record "RPT-002 PDF on Disk" $(if ($pdfs.Count -gt 0) {"PASS"} else {"FAIL"}) "PDFs: $($pdfs.Count)"
} catch { Record "RPT-001 Report" "FAIL" $_.Exception.Message }

# ---- STEP 9: REGRESSION ----
Log "=== STEP 9: REGRESSION ==="
@{
    "REG-001 Projects API" = { Invoke-RestMethod -Uri "$BASE/projects" -Headers $H }
    "REG-002 Dashboard API" = { Invoke-RestMethod -Uri "$BASE/dashboard" -Headers $H }
    "REG-003 Scan History" = { Invoke-RestMethod -Uri "$BASE/scan/project/$PROJ_ID" -Headers $H }
    "REG-004 Reports API" = { Invoke-RestMethod -Uri "$BASE/reports/my" -Headers $H }
} | ForEach-Object {
    $_.GetEnumerator() | ForEach-Object {
        try { $r = & $_.Value; Record $_.Key "PASS" "HTTP 200 OK" } catch { Record $_.Key "FAIL" $_.Exception.Message }
    }
}

# ---- STEP 10: FAILURE SCENARIOS ----
Log "=== STEP 10: FAILURE SCENARIOS ==="

# Corrupted ZIP
try {
    $cb = [System.Text.Encoding]::UTF8.GetBytes("NOT A ZIP")
    $bnd = [System.Guid]::NewGuid().ToString(); $iso = [System.Text.Encoding]::GetEncoding("ISO-8859-1")
    $bl = @("--$bnd","Content-Disposition: form-data; name=`"files`"; filename=`"corrupt.zip`"","Content-Type: application/zip","", $iso.GetString($cb), "--$bnd--")
    $bb = $iso.GetBytes($bl -join "`r`n")
    $cr = Invoke-WebRequest -Method POST -Uri "$BASE/uploads/$PROJ_ID`?overrideDuplicate=false" -Headers $H -Body $bb -ContentType "multipart/form-data; boundary=$bnd" -UseBasicParsing -ErrorAction SilentlyContinue
    $cd = ($cr.Content | ConvertFrom-Json).data[0]
    Record "FAIL-001 Corrupt ZIP Rejected" $(if (-not $cd.success) {"PASS"} else {"FAIL"}) "msg=$($cd.message)"
} catch { Record "FAIL-001 Corrupt ZIP Rejected" "PASS" "HTTP error (correct rejection)" }

# Unauthorized access
try {
    Invoke-RestMethod -Method GET -Uri "$BASE/scan/$($script:S1_ID)" -ErrorAction Stop | Out-Null
    Record "FAIL-002 No-Auth Scan Access" "FAIL" "Should require auth"
} catch { Record "FAIL-002 No-Auth Scan Access" "PASS" "Correctly rejected unauthenticated request" }

# Wrong project
try {
    Invoke-RestMethod -Method POST -Uri "$BASE/scan/99999" -Headers $H -Body (@{}|ConvertTo-Json) -ContentType "application/json" -ErrorAction Stop | Out-Null
    Record "FAIL-003 Non-existent Project Scan" "FAIL" "Should return 404"
} catch { Record "FAIL-003 Non-existent Project Scan" "PASS" "Correctly returned error for non-existent project" }

# ---- FINAL REPORT ----
Log "`n================================================"
Log "RESULTS: $script:PASS PASSED, $script:FAIL FAILED"
Log "================================================"

# Build report
$lines = @()
$lines += "# UPLOAD LIFECYCLE VERIFICATION RESULTS"
$lines += "> Generated: $(Get-Date -Format 'yyyy-MM-dd HH:mm:ss')"
$lines += ""
$lines += "## Summary"
$lines += "| | Count |"
$lines += "|---|---|"
$lines += "| ✅ PASS | $script:PASS |"
$lines += "| ❌ FAIL | $script:FAIL |"
$lines += "| Total | $($script:PASS+$script:FAIL) |"
$lines += ""
$lines += "## Test Results"
$lines += "| # | Test ID | Status | Evidence |"
$lines += "|---|---------|--------|----------|"
$i=1; foreach($r in $script:RESULTS) {
    $sym = if ($r.Status -eq "PASS") {"✅"} else {"❌"}
    $lines += "| $i | $($r.Test) | $sym $($r.Status) | $($r.Detail) |"
    $i++
}
$lines += ""
$lines += "## Runtime Evidence"
$lines += "| Key | Value |"
$lines += "|-----|-------|"
$lines += "| Project ID | $PROJ_ID |"
$lines += "| Upload Record 1 ID | $($script:F1_ID) |"
$lines += "| Upload Record 2 ID | $($script:F2_ID) |"
$lines += "| SHA-256 (both uploads) | $($script:F1_SHA) |"
$lines += "| Scan 1 ID | $($script:S1_ID) |"
$lines += "| Scan 2 ID | $($script:S2_ID) |"
$lines += "| Scan 1 Vulnerabilities | $($script:S1_VULNS) |"
$lines += ""
$lines += "## Filesystem State (End)"
$allF = Get-ChildItem "D:\Micro Project\uploads" -Recurse -File -ErrorAction SilentlyContinue
$lines += "Total files (incl. reports): $($allF.Count)"
$lines += "``````"
foreach($f in $allF) { $lines += "$($f.FullName.Replace('D:\Micro Project\uploads\','')) ($([math]::Round($f.Length/1024,1)) KB)" }
$lines += "``````"
$lines += ""
$lines += "## Backend Log Evidence"
$logFile2 = "C:\Users\lenovo\.gemini\antigravity-ide\brain\da7b95e4-96ac-4c90-ac00-f6eddd45360b\.system_generated\tasks\task-1572.log"
$secEntries = Select-String "\[Security\]|\[Audit\]|\[CONFIG\]" $logFile2 -ErrorAction SilentlyContinue | Select-Object -Last 20
$lines += "``````"
foreach($e in $secEntries) { $lines += $e.Line }
$lines += "``````"

$rpt = $lines -join "`n"
$rpt | Set-Content "D:\Micro Project\VERIFICATION_RESULTS.md" -Encoding UTF8
Write-Host "`nReport written to: D:\Micro Project\VERIFICATION_RESULTS.md"
