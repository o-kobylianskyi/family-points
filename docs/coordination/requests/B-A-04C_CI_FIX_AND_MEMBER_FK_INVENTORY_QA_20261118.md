# Backend A B-A-04c — CI compile repair and audited member-FK source inventory

Date: 2026-11-18. Scope: TEST-ONLY member-source ancestry + ABI-safe source verification. Status: source committed, exact-SHA QA/CI pending.

## P0 red CI diagnosis / repaired defect
Full four-component run `44278301617` on `32cff0f` FAILED: Java21 Maven testCompile line 98 in `DisposablePortableMemberSourceCatalog` referenced undefined `n` after prior refactor of numeric positive-ID guard. Web/Android/Compose jobs were SUCCESS, but full gate FAILED. Repaired `return ((Number) raw).longValue()` in `b6c4669`; this compile fix must receive its own non-skipped test execution. Do NOT call earlier source green merely because docs-only runs were green.

## Additional source graph hardening
- `DisposablePortableMemberSourceCatalogPostgresTest`: V28 schema trigger `trg_workspace_member_role` rejects foreign-family roles at SQL INSERT. First assert that production-like trigger denies the write, then use disposable-PG-only trigger disable within try/finally to inject synthetic legacy corrupt row and assert source exporter preflight rejects it. Re-enable trigger after fixture. No real tenant DB used.
- `DisposablePortableMemberReferenceClosure`: add `MEMBER_EXCEPTION_PERIODS.member_id` (required source ancestry) to reviewed subset; `DisposablePortableMemberEdgeSchemaPostgresTest` checks it exists and remains NOT NULL.
- `DisposablePortableMemberFkInventoryPostgresTest`: query actual `pg_constraint` for all FK children of `family_members`; assert key TASK/LEDGER/REWARD/exception-period edges present; classify audited source categories vs unreviewed FK fields. Print uncovered edges as `B-A-04 UNREVIEWED_MEMBER_FKS`; don't silently auto-approve them for signed archive. If a previously reviewed task/ledger/reward category acquires a new member FK, fail test pending contract review.
- This is a **subset** of a full portable source graph. Global user accounts/account-workspace memberships are auth provenance, not safe archive identity metadata. Media polymorphic references and financial source-type backreferences require separate B/D review.

## Exact tests
```bash
./mvnw -B -ntp -Dtest=DisposablePortableMemberSourceCatalogPostgresTest,DisposablePortableMemberReferenceClosureTest,DisposablePortableMemberEdgeSchemaPostgresTest,DisposablePortableMemberFkInventoryPostgresTest,WorkspaceBackupRetentionRollbackGapPostgresTest test
```

## Acceptance / deferrals
- Need real Maven compile and actual PG18 tests with Docker, zero failures/errors/skips for targeted tests, plus independent QA.
- No production exporter, inspector, archive schema/manifest, owner account migration, password/OAuth export, archive auto-cleanup or Restore execution changed.
- Proposed vNext source role/member fields and privacy/hidden category/IMP remain gated by named Architecture A4-01…09 and B/C/D consumer signoff. Existing v1 remains read-only inspectable where supported, non-restorable. HTTP423/BETA HOLD unchanged.
- Next B-A-04d: address exact CI/JUnit failures and investigate `B-A-04 UNREVIEWED_MEMBER_FKS` source table owners, then implement vNext only after approved wire contract.
