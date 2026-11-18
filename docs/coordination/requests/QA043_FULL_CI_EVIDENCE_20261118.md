# QA-043 — independent full CI request and pre-beta safety gate
Date: 2026-11-18
Owner: QA
Status: CI REQUESTED / NOT YET VERIFIED

## Why an explicit full CI is required
Real GitHub Actions run #44264060524 (before QA patch) had Backend Maven FAILURE while Web, Android and Compose succeeded. QA patch eda8b9fea30ada5a7d067b5ec8a366c384a4f3c6 modifies only the second malformed POINT_TYPES ID test expectation from SecurityException to IllegalArgumentException; duplicate-code rejection stays SecurityException.

Later green runs #44265381620, #44265681060, #44265685450 and #44265868544 were scoped documentation-only runs: Maven, Web regression/build, Android Gradle and Release Compose safety steps all SKIPPED; Full CI attestation SKIPPED. They must NOT be interpreted as BETA evidence.

This coordination-only commit includes [full-ci] in its commit message, invoking the explicit four-scope route in .github/workflows/ci.yml. The run validates the exact pushed SHA. It does not authorize deployment, backup, restore, reset or any production write.

## Independent QA acceptance matrix
- Backend: Java 21 Maven clean verify, PostgreSQL Testcontainers, full Surefire test counts, 0 failures, 0 errors, 0 skipped, specifically malformed archive duplicate code and invalid numeric ID.
- Web: i18n check, regression, lint and Vite build all EXECUTED and successful; mocked authenticated Playwright V49 two-tab tests are NOT automatically covered by this workflow unless separately invoked.
- Android: :app:assembleDebug :app:testDebugUnitTest :app:assembleDebugAndroidTest all EXECUTED and successful; physical device tests remain a separate real-world QA phase.
- Compose / Release safety: all non-skipped scripted syntax and synthetic regression steps success; full exact-SHA attestation and push outcome must say SUCCESS with job/commit provenance.
- Onboarding V49 real server workflow / BKL-002 portable Restore are NOT validated by the CI alone. Real writes are blocked pending operator-authorized disposable workspace, approved owner reauthentication and restore protective snapshot acceptance.

## State classifier
PASS: exact SHA, all four required build/test families actually executed, 0 failures/errors, attestation green.
FAIL: a required command ran and failed; record exact run ID, job, failing method and sanitized logs.
SKIPPED: workflow green but required commands were skipped; do NOT count as a full pass.
PENDING: jobs running or evidence unavailable.
BETA remains HOLD; reset Execute HTTP423, restorable=false, replaySupported=false.

## Follow-up
QA will inspect Actions run jobs AND steps on this exact commit, capture Backend test summary and route any failure to Issue #15, Release discrepancy to Issue #20, Web onboarding defect to Issue #18. Operator-assisted tests remain consolidated under QA.
