# UPLOAD LIFECYCLE VERIFICATION RESULTS
> Generated: 2026-07-24 10:16:09

## Summary
| | Count |
|---|---|
| âœ… PASS | 22 |
| âŒ FAIL | 4 |
| Total | 26 |

## Test Results
| # | Test ID | Status | Evidence |
|---|---------|--------|----------|
| 1 | AUTH-001 Login | âœ… PASS | Token len=160 |
| 2 | PROJ-001 Reuse | âœ… PASS | ID=6 |
| 3 | UPLOAD-001 First Upload (pre-existing) | âœ… PASS | Duplicate from prior run - FileID=7 |
| 4 | UPLOAD-002 Metadata Accessible | âŒ FAIL | The remote server returned an error: (404) Not Found. |
| 5 | UPLOAD-003 Physical File on Disk | âœ… PASS | Files on disk: 5 |
| 6 | SCAN-001 First Scan Triggered | âœ… PASS | ScanID=136 |
| 7 | SCAN-002 First Scan Complete | âœ… PASS | status=COMPLETED vulns=0 |
| 8 | SEC-001 Source Files Deleted After Scan | âŒ FAIL | Remaining: 6 (1cc12dd5-b2dd-4fe0-9a27-8fb58a954412.java
24a5ed33-0255-4fb4-8e91-04d5f5c18818.zip
59498fa6-583e-4232-9800-39e29146fd41.html
76145898-347c-46ff-9645-749e85655026.zip
9d9e4788-42e6-43b5-8b36-e06b8e1a7e90.zip
e9fcaf88-36d5-404b-89f2-20d57066d613.zip
.Trim()) |
| 9 | SEC-002 Scan History Intact After Delete | âœ… PASS | status=COMPLETED vulns=0 |
| 10 | SEC-003 Backend Log Evidence | âœ… PASS | Log entries: 2 |
| 11 | DUP-001 Duplicate Signal Returned | âœ… PASS | success=False msg=DUPLICATE_FILE_DETECTED |
| 12 | DUP-002 Override Upload Success | âœ… PASS | FileID=10 |
| 13 | DUP-003 New Record Created (Different ID) | âœ… PASS | ID1=7 vs ID2=10 |
| 14 | DUP-004 Identical SHA-256 (Same Content) | âœ… PASS | 3099209921e7f25c... |
| 15 | SCAN2-001 Second Scan Triggered | âœ… PASS | ScanID=137 |
| 16 | SCAN2-002 Second Scan Complete | âœ… PASS | status=COMPLETED vulns=6 |
| 17 | SCAN2-003 Secure Delete After 2nd Scan | âŒ FAIL | Remaining: 5 |
| 18 | RPT-001 PDF Report Generated | âŒ FAIL | Count= |
| 19 | RPT-002 PDF on Disk | âœ… PASS | PDFs: 19 |
| 20 | REG-002 Dashboard API | âœ… PASS | HTTP 200 OK |
| 21 | REG-004 Reports API | âœ… PASS | HTTP 200 OK |
| 22 | REG-001 Projects API | âœ… PASS | HTTP 200 OK |
| 23 | REG-003 Scan History | âœ… PASS | HTTP 200 OK |
| 24 | FAIL-001 Corrupt ZIP Rejected | âœ… PASS | msg=DUPLICATE_FILE_DETECTED |
| 25 | FAIL-002 No-Auth Scan Access | âœ… PASS | Correctly rejected unauthenticated request |
| 26 | FAIL-003 Non-existent Project Scan | âœ… PASS | Correctly returned error for non-existent project |

## Runtime Evidence
| Key | Value |
|-----|-------|
| Project ID | 6 |
| Upload Record 1 ID | 7 |
| Upload Record 2 ID | 10 |
| SHA-256 (both uploads) | 3099209921e7f25c9641376e478e6f4de087976125f2cf663fc5b03a830ff97e |
| Scan 1 ID | 136 |
| Scan 2 ID | 137 |
| Scan 1 Vulnerabilities | 0 |

## Filesystem State (End)
Total files (incl. reports): 25
```
12\5\1cc12dd5-b2dd-4fe0-9a27-8fb58a954412.java (2.2 KB)
7\3\24a5ed33-0255-4fb4-8e91-04d5f5c18818.zip (3.1 KB)
7\3\59498fa6-583e-4232-9800-39e29146fd41.html (19.1 KB)
7\3\76145898-347c-46ff-9645-749e85655026.zip (2.2 KB)
7\3\9d9e4788-42e6-43b5-8b36-e06b8e1a7e90.zip (2.3 KB)
7\3\e9fcaf88-36d5-404b-89f2-20d57066d613.zip (2.4 KB)
reports\1\1\security-report-scan-1-1783181828237.pdf (9.8 KB)
reports\1\1\security-report-scan-1-1783181837238.pdf (9.8 KB)
reports\13\6\security-report-scan-132-1784867914495.pdf (1.8 KB)
reports\13\6\security-report-scan-134-1784868102488.pdf (1.8 KB)
reports\13\6\security-report-scan-136-1784868361134.pdf (1.8 KB)
reports\2\2\security-report-scan-2-1783255601172.pdf (11.1 KB)
reports\2\2\security-report-scan-3-1783256836426.pdf (10.5 KB)
reports\2\2\security-report-scan-4-1783259123284.pdf (12.7 KB)
reports\2\2\security-report-scan-4-1783259568363.pdf (14.2 KB)
reports\2\2\security-report-scan-6-1783259676032.pdf (11.7 KB)
reports\7\1\security-report-scan-1-1783448091362.pdf (12.8 KB)
reports\7\1\security-report-scan-1-1783452448787.pdf (22.7 KB)
reports\7\1\security-report-scan-3-1783621196683.pdf (9.8 KB)
reports\7\1\security-report-scan-3-1783626305109.pdf (9.8 KB)
reports\7\1\security-report-scan-4-1783743760630.pdf (13.2 KB)
reports\7\3\security-report-scan-19-1783846797036.pdf (11.4 KB)
reports\7\3\security-report-scan-25-1784195176038.pdf (17.2 KB)
reports\7\3\security-report-scan-25-1784197436332.pdf (19.6 KB)
reports\8\2\security-report-scan-2-1783451131498.pdf (13.7 KB)
```

## Backend Log Evidence
```
2026-07-23 15:09:59.207  INFO 21692 --- [ecurity-Async-1] c.s.a.service.UploadServiceImpl          : [Security] Securely deleted physical files for project 6 — deleted=1, failed=0
2026-07-23 15:13:05.333  INFO 21692 --- [nio-8081-exec-4] c.s.a.service.UploadServiceImpl          : [Security] Duplicate detected for 'VulnerableApp.zip' in project 6 — awaiting user confirmation.
2026-07-23 15:13:05.373  INFO 21692 --- [nio-8081-exec-6] c.s.a.service.UploadServiceImpl          : [Audit] Override accepted for 'VulnerableApp.zip' in project 6 — creating a new upload record (SHA-256=3099209921e7f25c9641376e478e6f4de087976125f2cf663fc5b03a830ff97e).
2026-07-23 15:13:05.512  INFO 21692 --- [ecurity-Async-2] c.s.a.service.UploadServiceImpl          : [Security] Securely deleted physical files for project 6 — deleted=1, failed=0
```
