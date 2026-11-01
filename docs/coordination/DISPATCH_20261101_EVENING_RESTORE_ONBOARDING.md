# FamilyPoints dispatch — 2026-11-01 (evening), after native restore progress

**Source inspected:** main 78f822df; Actions run 41349970130 for 706bcf5a QUEUED, no certified green current HEAD. Issues #15 and #18 OPEN.
**Priority:** BKL-002 AND Issue18 before next beta, no real family data deletion, no off-host backup requirement.

## Evidence delta since bf44807e
- Backend commits 90cd9649, f688cd6c, 706bcf5a build reusable, rollback-only native PostgreSQL group restoring with fresh identity maps, catalog/media binding remapping and tests. **This is not production restore execute.**
- Web 652f0ef4 and 78f822df add explicit permission-gated group create in onboarding, duplicate reconciliation and optional group read-failure isolation from import; still no approved portable file HTTP upload/download/execute.
- Android cb7d8922, 224f34d2, f41988fc cover wire-level offline retry/timeouts, durable UUID/body and session+epoch revalidation; device runtime acceptance remains separate.
- QA fixed Testcontainers generic compilation (79857568). Source tests do not equal current-head PASS.
- Rapid commits cancel CI runs; demand stable candidate SHA and completed 4-job CI.

## Dispatch: coherent implementation batches
**Architecture P0:** Sign exact allowed FULL/SELECTIVE reset matrix already product-approved; per-category restore semantics REPLACE vs MERGE, ledger retention/audit, parent/owner/global shared account and fresh Google reauth, key portability. Record decision/blocked cases in ADR. Existing no external storage decision remains.
**Backend P0:** Complete end-to-end family portable archive export/download and upload/full-or-selective restore APPLY (not rollback-only rehearsal). Implement all approved categories, remap FKs, media, ledger defensively, durable receipts, rollback/idempotency, immutable auth/owner, epoch fencing. Then atomic reset execute requiring genuinely recoverable 30-day pre-reset snapshot. Keep execute423 until safety approval. Publish tested HTTP contract rather than proposed APIs.
**Web P0:** Stop expanding readonly metadata polish; finish manager setup group/template/activation flow and source-verified HTTP clients for file backup only after Backend endpoints actually exist. Keep disabled restore/reset until contract and QA.
**Android P1:** Consolidate offline replay coordinator+repository+SQLite tests on exact-head Gradle/CI; preserve UUID and prevent cross-session stale writes. No queued reset/restore. Report instrumentation not run separately.
**QA P0:** Run targeted disposable PG integration on exact SHA, full/selective roundtrip with pre/post FK counts, no cross-family changes and proof backup corruption blocks reset; prove Web onboarding new family->group->catalog selection->import->explicit activation. Device/live testing deferred unless user says otherwise.
**Release P1:** Reserve stable green SHA after writes pause, check all jobs and local 30-day encrypted backup filesystem and key provisioning; do not deploy or purge any real family. No external cloud/object storage project.
**Designer P2:** Four locale FULL/SELECTIVE impact, backup vs recoverable status, file import/restore warnings.

## Acceptance and next
1. Actual full/selective portable restore apply on disposable PostgreSQL, not just internal staging rollback.
2. Verified pre-reset backup + approved atomic FULL/SELECTIVE Reset Execute, with 30-day server retention (downloaded user files not expirable by server).
3. Onboarding disposable family roundtrip and exact-green CI before release gate.

**Coordination via GitHub doc only:** no auto-delivery across chats; each owner must retrieve and ACK with SHA/test/result/blocker. Real reset/upload/deployment not authorized.
