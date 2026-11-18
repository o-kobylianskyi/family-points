# Backend A — B-A-03l / B-A-04 exact-SHA full CI and independent QA request
**Date:** 2026-11-18. **Gate:** Java21/Maven/PostgreSQL18 + Android + Web + Compose MUST execute, none skipped.

## Changes to validate
- `src/test/java/com/olehkobylianskyi/familypoints/DisposablePortableMemberSourceCatalog.java`
- `src/test/java/com/olehkobylianskyi/familypoints/DisposablePortableMemberSourceCatalogPostgresTest.java`
- `src/test/java/com/olehkobylianskyi/familypoints/WorkspaceBackupRetentionRollbackGapPostgresTest.java`
- Existing `WorkspaceBackupReconciliationRacePostgresTest`, `WorkspaceBackupRegistryPresenceRacePostgresTest`, `WorkspaceBackupRetentionPostgresTest`
- New source contract `docs/coordination/requests/B-A-04_MEMBER_SOURCE_FIELDS_AND_WIRE_GATE_20261118.md`.

## Acceptance
1. Java testCompile succeeds; no illegal generic Testcontainers PostgreSQL container syntax.
2. Actual Maven `clean verify` runs and finishes with 0 failures / 0 errors; capture Surefire reports for member source roster, retention physical-unlink rollback, other postgres race suites, count explicit executed tests.
3. Two-family source only: role and member IDs/provenance, tombstones preserved with no private name/account leakage. Foreign-family role parent rejected and READ_COMMITTED/no transaction denied.
4. Crash after physical unlink: SQL rolls back, original encrypted file stays missing, reverse audit reports discrepancy; independent unlink error fails before `EXPIRED` registry status. No real backup or family modified.
5. Android build+tests, Web regression/lint/build, Compose safety checks all run (not skipped) at the SAME source SHA.
6. No production archive wire/schema, owner permit, import execution, reset enablement or automatic purge.
7. Preserve SHA-specific run ID, executed step conclusions and independent QA receipt; workflow `SUCCESS` with skipped Maven is NOT an acceptance.

## Routing
Architecture: review A4-01…09 and publish named wire approval/IMP.
Backend B: confirm exact consumer fields incl nullable original assignment + historical TaskInstance.
Backend C: owner account provenance and privacy/reauth policy, NOT source credential export.
Backend D: ledger member/point and transaction ancestry.
QA: independently run real PG18 disposable tests and record PASS/FAIL/SKIP.
Release: BETA HOLD, no production Restore and no automatic retention deletion.

The commit publishing this test request intentionally includes **[full-ci]** so .github/workflows/ci.yml schedules all four components, rather than doc-only scope routing. Not a deployment request.
