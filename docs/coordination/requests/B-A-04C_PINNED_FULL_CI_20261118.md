# B-A-04c — pinned full CI execution after Java compile fix and FK inventory

Date: 2026-11-18 · Backend A, no production deploy. This commit intentionally requests `[full-ci]`.

## Required evidence (exact SHA)
Run **all** four components: Java 21/Maven/Testcontainers PostgreSQL 18, Android/Gradle unit/build, Web regression/lint/build and Compose safety. Check each job's actual steps, not its green routing wrapper.

Backend tests of special interest:
```bash
./mvnw -B -ntp -Dtest=DisposablePortableMemberSourceCatalogPostgresTest,DisposablePortableMemberReferenceClosureTest,DisposablePortableMemberEdgeSchemaPostgresTest,DisposablePortableMemberFkInventoryPostgresTest,WorkspaceBackupRetentionRollbackGapPostgresTest test
```
The earlier `32cff0f` full CI `44278301617` correctly failed Java test compilation due undefined `n` at member catalog line 98. Fix `b6c4669`. Additional PostgreSQL negative test now honors V28 workspace role integrity trigger before deliberately injecting synthetic legacy invalid rows into disposable DB. Verify trigger re-enabled and that no production database accessed.

Assess results: PASS/FAIL/ERROR/SKIP and Surefire per-class counts. A backend green job without actual Maven run is NOT acceptance. This is only test-only source graph proof, not vNext archive producer. No Reset Execute, no Restore, no automatic backup deletion, no developer/user credential inclusion. BETA HOLD.

Review changes and decisions: `docs/coordination/requests/B-A-04C_CI_FIX_AND_MEMBER_FK_INVENTORY_QA_20261118.md`; pending Architecture A4-01…09 source member wire/IMP approval.
