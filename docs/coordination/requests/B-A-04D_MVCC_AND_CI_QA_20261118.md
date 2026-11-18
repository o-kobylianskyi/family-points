# B-A-04d — Original member source graph and pinned MVCC CI acceptance

**Date:** 2026-11-18 · **Backend A** · STATUS: CODE COMMITTED; NEW exact-SHA CI pending. Only test files are changed.

## Red CI diagnosis and correction
- Full CI run `44278301617` source `32cff0f` FAILED in actual Java testCompile (undefined `n` in `DisposablePortableMemberSourceCatalog.java:98`). Fixed at `b6c4669`.
- Earlier `[full-ci]` run `44281984930` source `a67f3c` was still **IN_PROGRESS** when inspected: Web, Android and Compose succeeded; Java/Maven/Postgres job was running. Never infer that Java tests succeeded from completed client jobs.
- No Architecture named A4-01…09 wire-schema/IMP ACK located; production exporter/inspector, crypto, schema and Restore are unchanged.

## Additional PG18 source snapshot regression
`DisposablePortableMemberSourceCatalogPostgresTest.pinnedRepeatableReadRosterNeverMixesConcurrentNewMembers`:
- Creates disposable family + role + member, reads source catalogue via Spring `TransactionTemplate` at `ISOLATION_REPEATABLE_READ`.
- A separate `DriverManagerDataSource` commits a second member between the first and second source reads.
- Inside the original transaction the catalogue must remain byte/record equal; a fresh transaction must observe both members.
- No real family, login data, credentials, owner permit, cross-family transfer, restore or production code affected.

## FK inventory and negative tests
- Existing `DisposablePortableMemberFkInventoryPostgresTest` inspects all `pg_constraint` FKs to `family_members`; covered versus uncovered source members are explicit, not silently auto-approved.
- Include source member exception periods, historical executor and original assignee, task definition assignments, delegation, group membership and financial actor references. Global account workspace links are **not** implicitly authorized in portable source format.
- V28 trigger negative fixture requires production DB to reject foreign roles; only disposable PostgreSQL can temporarily disable trigger to simulate old corrupt rows.

## QA command
```bash
./mvnw -B -ntp -Dtest=DisposablePortableMemberSourceCatalogPostgresTest,DisposablePortableMemberReferenceClosureTest,DisposablePortableMemberEdgeSchemaPostgresTest,DisposablePortableMemberFkInventoryPostgresTest,WorkspaceBackupRetentionRollbackGapPostgresTest test
```
Record class-specific PASS/FAIL/ERROR/SKIP, exact SHA and actual `Run all backend tests` step. Note that Android/Web/Compose success is not backend approval.

## B-A-04e next
1. Diagnose actual exact-SHA Java test run (especially PG18 new FK inventory and V28 trigger).
2. Expand member source schema FK closure and conservative legacy categories without guessing original source identity.
3. Await Architecture typed vNext member catalogue ACK + IMP and B/C/D consumer agreement before production change.
4. No automatic physical purge, `restorable=false`, `replaySupported=false`, Reset Execute HTTP423, BETA HOLD.
