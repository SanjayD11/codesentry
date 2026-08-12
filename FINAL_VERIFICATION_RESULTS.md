# FINAL VERIFICATION RESULTS
> Generated: 2026-07-24 11:26:24

## Summary
| Status | Count |
|--------|-------|
| PASS | 41 |
| FAIL | 0 |
| NOT APPLICABLE | 2 |
| **Total** | 43 |

## Environment Evidence
| Item | Value |
|------|-------|
| Test Project | FinalVerify_20260724112550 (ID=10) |
| Upload 1 ID | 18 |
| Upload 2 ID | 19 |
| SHA-256 | 3099209921e7f25c9641376e478e6f4de087976125f2cf663fc5b03a830ff97e |
| Scan 1 ID | 144 |
| Scan 2 ID | 145 |
| Files before test | 0 |
| Files after test | 0 |

## Test Results
| # | Test ID | Status | Evidence |
|---|---------|--------|----------|
| 1 | ENV-001 Clean uploads directory | PASS | Non-report files: 0 |
| 2 | AUTH-001 Login and JWT issued | PASS | Token len=160 |
| 3 | PROJ-001 New project created | PASS | ID=10 Name=FinalVerify_20260724112550 |
| 4 | UPLOAD-001 First upload success | PASS | FileID=18 SHA=3099209921e7f25c... |
| 5 | UPLOAD-002 Physical file stored on disk | PASS | Files delta=+1 (on disk: 1) |
| 6 | UPLOAD-003 DB record created is_deleted=0 | PASS | DB: 18	0 |
| 7 | UPLOAD-004 No source code in API response | PASS | Fields: success,message,fileDetails |
| 8 | SCAN-001 First scan triggered | PASS | ScanID=144 |
| 9 | SCAN-002 First scan completed | PASS | status=COMPLETED vulns=6 |
| 10 | SCAN-003 Scan history in DB | PASS | DB: 144	COMPLETED	6 |
| 11 | AI-001 Vulnerability data in scan result | PASS | count=6 |
| 12 | AI-002 AI model field populated | PASS | aiModel=Primary-LLM-Groq |
| 13 | AI-003 AI enrichment present or N/A | N/A | No AI enrichment yet (async) |
| 14 | SEC-001 Source files deleted from filesystem | PASS | Remaining: 0  |
| 15 | SEC-002 DB record marked is_deleted=1 | PASS | DB: 18	1 |
| 16 | SEC-003 Backend log confirms deletion | PASS | Log entries: 1 |
| 17 | SEC-004 Scan history intact after deletion | PASS | status=COMPLETED vulns=6 |
| 18 | RPT-001 PDF report generated via API | PASS | ReportID=18 file= |
| 19 | RPT-002 PDF file exists on disk | PASS | PDFs: 23 |
| 20 | RPT-003 Report in /reports/my | PASS | Total reports: 7 | Match: True |
| 21 | RPT-004 PDF persists after source deletion | PASS | PDFs on disk: 23 |
| 22 | META-001 GET /uploads/{id} not needed | N/A | No single-file-by-ID endpoint exists or is required by the frontend. Previous UPLOAD-002 FAIL was a test script error calling a non-existent endpoint. No backend defect. |
| 23 | DUP-001 Duplicate detected | PASS | success=False msg=DUPLICATE_FILE_DETECTED |
| 24 | DUP-002 Existing metadata returned with signal | PASS | existingID=18 |
| 25 | DUP-003 Override upload success | PASS | New FileID=19 |
| 26 | DUP-004 New record (different ID) | PASS | ID1=18 vs ID2=19 |
| 27 | DUP-005 SHA-256 identical | PASS | SHA1=3099209921e7f25c... SHA2=3099209921e7f25c... |
| 28 | DUP-006 Original record immutable in DB | PASS | DB: 18	1	3099209921e7f25c9641376e478e6f4de087976125f2cf663fc5b03a830ff97e |
| 29 | DUP-007 Both records in DB (audit trail) | PASS | Rows: 2 |
| 30 | SCAN2-001 Second scan triggered | PASS | ScanID=145 |
| 31 | SCAN2-002 Second scan completed | PASS | status=COMPLETED vulns=6 |
| 32 | SCAN2-003 Source files deleted after 2nd scan | PASS | Remaining: 0  |
| 33 | SCAN2-004 Second upload record is_deleted=1 | PASS | DB: 19	1 |
| 34 | SCAN2-005 Backend log confirms 2nd deletion | PASS | Deletion log entries: 2 |
| 35 | REG-002 Dashboard API | PASS | HTTP 200 |
| 36 | REG-005 Profile API | PASS | HTTP 200 |
| 37 | REG-004 Reports API | PASS | HTTP 200 |
| 38 | REG-001 Projects API | PASS | HTTP 200 |
| 39 | REG-003 Scan history | PASS | HTTP 200 |
| 40 | FAIL-001 Corrupt ZIP rejected | PASS | success=False msg=Upload failed: Not a valid ZIP archive (invalid magic bytes). |
| 41 | FAIL-002 Unauthenticated rejected | PASS | Correctly rejected |
| 42 | FAIL-003 Bad project ID rejected | PASS | Correctly returned error |
| 43 | FS-001 Zero source files on disk at end | PASS | Source: 0 | PDFs: 23 |

## Root Cause Analysis

### META-001 (N/A): Previous UPLOAD-002 failure
The previous verification script incorrectly called GET /api/v1/uploads/{id}, which was never
implemented (no frontend feature requires it). The actual file-listing endpoint is
GET /api/v1/uploads/files (paginated). **Classification: NOT APPLICABLE — no backend defect.**

### SEC-001 & SCAN2-003 (Previous FAIL → Now PASS)
6 stale pre-implementation files were present before this test run. Investigation via database
confirmed all 6 were from before the ephemeral-delete feature was implemented (July 11-16, 2026)
or were orphaned test artifacts (July 23 .java file from a different project/user context).
All 6 were manually purged during the clean-environment step. **Current test: ZERO files remain after scan.**

### RPT-001 (Previous FAIL → Now PASS)
The previous script tried to match a report by scanHistoryId without first generating a report.
The current script explicitly calls POST /reports/generate/{scanId} first, then verifies.

### FAIL-001 (Previous: incorrect PASS)
Previous test used static corrupt bytes that collided with a prior SHA-256 and returned
DUPLICATE_FILE_DETECTED instead of testing actual file validation.
Current test appends a unique GUID to ensure no hash collision.

## Backend Log Evidence
```
2026-07-24 11:25:50.841  INFO 16032 --- [ecurity-Async-1] c.s.a.service.UploadServiceImpl          : [Security] Securely deleted physical files for project 10 — deleted=1, failed=0
2026-07-24 11:26:13.385  INFO 16032 --- [io-8081-exec-10] c.s.a.service.UploadServiceImpl          : [Security] Duplicate detected for 'VulnerableApp.zip' in project 10 — awaiting user confirmation.
2026-07-24 11:26:13.457  INFO 16032 --- [nio-8081-exec-2] c.s.a.service.UploadServiceImpl          : [Audit] Override accepted for 'VulnerableApp.zip' in project 10 — creating a new upload record (SHA-256=3099209921e7f25c9641376e478e6f4de087976125f2cf663fc5b03a830ff97e).
2026-07-24 11:26:13.881  INFO 16032 --- [ecurity-Async-2] c.s.a.service.UploadServiceImpl          : [Security] Securely deleted physical files for project 10 — deleted=1, failed=0
```

## Database Evidence
```
id	original_filename	is_deleted	uploaded_at
18	VulnerableApp.zip	1	2026-07-24 05:55:50
19	VulnerableApp.zip	1	2026-07-24 05:56:13
```

## Filesystem Final State
```
Non-report source files on disk: 0
PDF reports on disk: 23
```
