# FamilyPoints — Backend Retention (independent lane) ACTIVE QUEUE

**Read first:** [Architect dispatcher](../ARCHITECT_DISPATCH_LATEST.md) on every `++`. This is a **new auxiliary backend lane**, NOT reopening Backend A/C/D. Owner starts this chat manually with `++` when Architecture requests. Operate asynchronously from Backend B and Web, without requesting per-commit approvals.

## URGENT CURRENT ACTION — 2026-11-19: RET-CHECK-03 ONE CORRECTION (on next `++`)

**The previous correction is NOT accepted.** QA [Issue #15 #6993294126](https://github.com/o-kobylianskyi/family-points/issues/15#issuecomment-6993294126) independently ran [combined PR #34 CI #44406980588](https://github.com/o-kobylianskyi/family-points/actions/runs/44406980588), SHA `ca5a8fbe3119ea520a7cd773e9b50bef8583fd0a`: **770 Maven tests / 0 failures / 1 error / 0 skipped**. Native B Restore 1/1 and Original Owner 2/2 passed; Web/Android/Compose passed, overall failed due solely to *your* test `WorkspaceBackupRetentionRollbackGapPostgresTest.committedExpiryRetainsBytesAndRepeatedSweepCannotReissueLease:180`: `UPDATE workspace_protective_backups SET storage_key=NULL WHERE snapshot_id=?` violates **`ck_protective_backup_artifact_evidence_complete`** on a row `state=EXPIRED`, `encrypted=true`, `artifact_sha256` populated. Earlier combined PR #33 opposite `assertThrows` failure @line188 showed a different partial evidence transition accepted. **Do not generalize EXPIRED nullable policy from a single row.**

**Assigned Retention-only fix as ONE meaningful batch**:
1. Read the **exact installed V50-V53 Flyway CHECK definition** and `WorkspaceBackupRetentionRollbackGapPostgresTest` at current main and PR34, identify the row state/evidence combination that makes each update legal or illegal. If necessary, inspect PG18 `pg_get_constraintdef` in disposable test and assert deterministic preconditions.
2. Rewrite only the retention test fixture/expectations into *valid deterministic state transitions* consistent with actual CHECK and explicit read-only fail-closed classification. Test wrong key and digest mismatch via permitted updates, and missing/null evidence only where schema truly allows it. **Do not weaken/drop DB constraint**, modify Restore Engine/permit, or misreport physically deleted ciphertext as retained. If the desired invalid-state simulation is impossible under real DDL, delete that *unreachable invalid-state test setup* while preserving equivalent public behavior guard; clearly justify what is and is not reachable.
3. Run once the focused disposable PG18 Testcontainers class set `./mvnw -Dtest=WorkspaceBackupRetentionRollbackGapPostgresTest,WorkspaceBackupRetentionPostgresTest,WorkspaceBackupRegistryPresenceAuditServiceTest test` or use an existing exact-SHA verified runner. Fix deterministic error fully before requesting any new full CI. Report `RET_CHECK_03_READY_FOR_QA` to Issue #15 with SHA, exact SQL CHECK, full targeted counts F/E/S and first stack if still red; no synthetic PASS claims.
4. QA creates *one* corrected combined PR after your handoff. **Do NOT trigger a full CI in this stream or ask Backend B to change your tests.** No follow-on RET enhancements until this test is demonstrably fixed.

**Safety:** no purge, no scheduler, no production Restore/Reset/deployment; `restorable=false`, `replaySupported=false`, HTTP423, BETA HOLD. Existing notes below are historical and superseded where inconsistent.

## Exclusive file boundary
- **Own for this assignment:** `WorkspaceBackupRetentionService.java`, narrowly related retention PG18 tests, optionally `WorkspaceBackupLocalArtifactStore.java` only for a proven necessary idempotent unlink adjustment **after checking B's file ownership**; retention-only docs and recovery tests.
- **Do NOT edit:** v2 archive exporter/inspector/manifest, Original Owner/reauth/RESTORE permit, target native restore/finance, authentication, Web/Android, shared general migrations. If a new migration is genuinely necessary, post a one-shot Architecture BLOCKER/design choice; meanwhile implement the independent non-destructive portions. Backend B owns source archive and atomic Restore.
- **Security contract:** no actual deletion from production filesystem, no scheduler enable, no external REST endpoint exposure; no production rollout. Keep fail-closed until QA.

## Package RET-01 — one large autonomous implementation → one QA handoff

**Verified bug in current product source:** `WorkspaceBackupRetentionService.expireEligible` is `@Transactional` yet calls `store.deleteAuthorizedExpired()` before its SQL `state='EXPIRED'` commit. A SQL failure after physical unlink can roll back registry while encrypted bytes are already lost. Comments about a later cleanup sweep are NOT equivalent to crash-consistent preservation. This is release P0 safety work but *independent* of Backend B original owner/restore implementation.

**Outcome:** Design and implement a robust storage/SQL consistency protocol that **never physically destroys required verified ciphertext before committed and recoverable retention intent**; preserve legal leases, original expiry, digests, tenant/snapshot storage key verification. Prefer durable two-phase state/rename/quarantine + post-commit finalization or another proven safe state machine; account for process kill after every boundary. Do not make a best-effort `afterCommit` callback the only recovery mechanism. Handle concurrent lease-vs-delete and retry/idempotency; no duplicate files or irreversible loss while registry still advertises restorable. Treat tampered/wrong storage keys as BLOCK. Preserve existing pending/verified/nonverified row semantics and recoverability.

**Acceptance tests (disposable PG18 / temporary artifact directory):** SQL rollback after selection, rollback after irreversible candidate, restart after committed intent before unlink, two workers with `SKIP LOCKED`, concurrent acquire/release lease, missing file, mismatched digest/key, repeated sweep, process interruption. Prove that verified backup never falsely remains registered as usable after physical deletion. Tests must not access real artifacts.

**Delivery:** one large integrated code package with atomic commits, targeted tests. At finish mark `RET-01_READY_FOR_QA` in Issue #15 with SHA, exact files, proof of recovery, failure/rollback tests, migration (if any), and caveats. This is *separate from* Backend B `READY_FOR_QA`, so QA may accept RET-01 independently when Architecture requests; don't wait for B.

## Pre-authorized next task RET-02 — no new architect ping required
**After RET-01 is code complete / handoff posted**, proceed directly with **retention reconciliation and operational safety** within SAME exclusive file boundary: retry/journal recovery on startup/manual sweep, metrics/logging without PII or secrets, bounded batch behavior, cross-tenant isolation, dry-run operator report, PG18 tests. If RET-01 QA finds a P0 regression, fix that before RET-02. Do NOT activate physical retention job or schedule deletion. Post separate `RET-02_READY_FOR_QA`; Architecture may then close lane or assign next independent task.

**Operator action:** none for these disposable tests. If a real test is unavoidable, post a separate `OPERATOR ACTION REQUIRED` packet and notify Architecture, do not ask the owner directly in ordinary status.

## RET work ledger — 2026-11-18 (implementation, not QA signoff)

- RET-01 SQL-first bounded expiry source: `36c021679c04718fa4090c0a6aac464e33ebcab6`. No direct unlink in `expireEligible`; scoped ciphertext verification before SQL EXPIRED transition; existing SKIP LOCKED and active-lease exclusions retained. Physical purge NOT implemented, enabled or authorized.
- RET-01 targeted PG18 fixture updates: `a6e72a3`, `c4f9ed8`, `5e82d70`, `03f7839`, `821b7d4`. Covers forced rollback preserving ciphertext, committed expiry with bytes retained, idempotent repeat, lease denial after EXPIRED and reconciliation result. These are source assertions, **not confirmed test executions**.
- RET-02 read-only bounded report source: `ef9f5af24346c4c5dc0e2fbf87b80a7104849df1`. `inspectExpiredArtifacts` classifies storage key/digest/integrity/unavailable while neither mutating SQL nor accessing any live files by itself. No automatic startup job or purge.
- Pending independently executable targeted command: `./mvnw -Dtest=WorkspaceBackupRetentionRollbackGapPostgresTest,WorkspaceBackupRetentionPostgresTest,WorkspaceBackupRegistryPresenceAuditServiceTest test` with disposable Docker/PG18. No local Docker/Maven test execution attested by this stream. Do not mark READY_FOR_QA until an actual compile/test result is inspected or Architecture explicitly accepts source-only QA handoff.
- Remaining design gate: true crash-safe physical purge finalizer and retry journal need separately approved retention lifecycle and recovery proof; a read-only reconciliation report does NOT constitute a completed deletion protocol. No migrations, Restore Engine, owner permit, Web or production configuration touched.

## RET follow-up checkpoint — 2026-11-18 20:08 CET activation
- Read architect latest dispatch and issue #15 current directive [6988504686](https://github.com/o-kobylianskyi/family-points/issues/15#issuecomment-6988504686). No touch to B-INT-02, Web or shared migrations.
- Product source `24e64910604e354e42b77448ac9ad6895752032d`: expired preview now includes keyless EXPIRED rows (read-only `NO_ARTIFACT_KEY`), and SQL expiry fails closed if a keyless row claims a digest or VERIFIED state. Avoid silently expiring corrupted registry evidence.
- PG18 test source `de97739b2a63864649634c3102d1041882b69dce`: absent storage key + digest must reject and preserve original state; `b985bd5961c4252b6066997c5d0fea96bd6db2a4` / `e3585f23c7ab83b257e0220f615070c671593d69`: read-only RET-02 report classifications across key/sha/missing evidence and intact file assertion.
- Verified existing upstream source compiler fix `18afa543d37eae6e6646369dab29c868159ba1d5` to disambiguate JUnit `TransactionTemplate` integer assertions. This fix predates latest stream commits; do not present as our test execution.
- NO actual targeted Maven/PG18 execution possible in this isolated tool runtime: Java 21 exists but repo, Maven wrapper and Docker daemon unavailable; GitHub connector exposes read/commit but no workflow dispatch for new exact SHA. No full CI or targeted PASS asserted.
- **State remains CODE_COMMITTED / QA_EXECUTION_NEEDED**, NOT QA-accepted; explicit bounded physical purge and durable deletion journal still deferred. Future QA runs pinned `-Dtest=WorkspaceBackupRetentionRollbackGapPostgresTest,WorkspaceBackupRetentionPostgresTest,WorkspaceBackupRegistryPresenceAuditServiceTest` in real Docker PG18 environment. Ensure pending QA handles unexpected schema constraints, concurrency, and actual crash/restart checks.

## RET-02 bounded concurrency and workspace isolation — subsequent `++`
- `2c3ca7342233efc4dacf356d2aa928ad72233a91`: introduced `inspectExpiredArtifactsForWorkspace(workspaceId,maxRecords)`, a read-only, tenant-filtered, bounded SQL query reusing the same classification logic. Global `inspectExpiredArtifacts` kept source compatible; no operator endpoint, scheduled job, physical deletion, migration or Restore contract change.
- `6877b8855dae770edd63c35d216d57bda7d67d0e`: disposable PG18 test assertions for correct tenant and rejection of invalid scoped requests.
- `3b530158873bff316154f99fd842bab5e7e7ad54`: added real two-transaction PG18 test fixture with explicit row lock from worker A, SKIP LOCKED worker B zero-count and immediate retry after unlock; verifies no store access and terminal EXPIRED idempotence. **Code committed, test not yet executed; avoid claiming actual concurrent runtime PASS.**
- Remains pending: run exact-SHA Maven/Testcontainers acceptance and fix actual failures; true durable physical purge remains BLOCKED by retention design authorization. Read-only recovery preview supports repeat after process restart but is not an autonomous retry/scheduler or physical deletion journal.

## Retention CI defect correction — 2026-11-18 21:40 CET

- **Actual CI evidence** from Backend B Issue #15 comment 6990884639: workflow [44390867357](https://github.com/o-kobylianskyi/family-points/actions/runs/44390867357), SHA `b5248e6523a71ca5edaabcc48acf0fd84741cfe0`, whole Maven suite 770 tests / 11 failures / 23 errors; exactly two Retention `WorkspaceBackupRetentionRollbackGapPostgresTest` errors due DB CHECK `ck_protective_backup_artifact_evidence_complete` when fixture attempted to null `storage_key` while preserving artifact evidence. This is not an authorized reason to weaken the DB constraint.
- **Fix `a0210961687a15eafe3f725a02962bb79b277a8a`:** malformed stored-row fixture assertions now expect `DataIntegrityViolationException` and prove registered state remains intact. Also rejects attempted `artifact_sha256=NULL`; retains valid-key/wrong-scope and hash-mismatch RET-02 read-only classification. Database constraints unchanged.
- Next gate: exact test execution on a commit that contains this fix; do not transfer the prior failing CI conclusion to new SHA. Disposable PG18 `./mvnw -Dtest=WorkspaceBackupRetentionRollbackGapPostgresTest,WorkspaceBackupRetentionPostgresTest,WorkspaceBackupRegistryPresenceAuditServiceTest test`. Backend B independently handling its own B-INT-02 failures; do not overlap.
- **Status** RET-01/RET-02: PATCH_COMMITTED / RETEST_PENDING; no physical purge, no schema migration, no production action. Durable purge/recovery journal remains unimplemented and separately gated.

## RET-01/RET-02 consolidated code-ready checkpoint — 2026-11-18

**Frozen package target for independent test**: commit `96cfe428df5c6f11a564714ef36f6cc83b008548` or newer with identical Retention source/test blobs. Regression tests include `a021096` PostgreSQL CHECK fixtures, `3b53015` simultaneous `SKIP LOCKED` workers, and `96cfe42` committed-state new-service restart + retry preservation.

**Test execution status**: not attested on corrected target. The only independent reported Retention failure is on earlier run `44390867357` at `b5248e65`; it is not current fix evidence. Do not present package as QA_PASS or fully crash-consistent physical purge. Read-only reconciliation/SQL-first no-unlink safety code is committed, but actual durable physical-deletion journal is explicitly NOT implemented or authorized.

**Single requested QA pass**: disposable PG18 `./mvnw -Dtest=WorkspaceBackupRetentionRollbackGapPostgresTest,WorkspaceBackupRetentionPostgresTest,WorkspaceBackupRegistryPresenceAuditServiceTest test`, exact commit SHA + test classes/counts F/E/S + first failing assertion; if green, Architecture can mark bounded RET-01/RET-02 source behavior accepted for QA. No duplicate full CI, no PROD purge, Restore Engine edits, or automatic scheduler.

## RET-01/02 latest concrete CI correction — 2026-11-18 22:44 CET

- **Actual newer immutable PG18 evidence**: Backend B draft PR #31 [CI #44395020403](https://github.com/o-kobylianskyi/family-points/actions/runs/44395020403) at `bfa2f5dce8f6f02a5d43b1dff4a5d183f62b08aa`, Maven **772 tests, 1 failure, 0 errors, 0 skipped**. Single failing retention assertion `WorkspaceBackupRetentionRollbackGapPostgresTest.committedExpiryRetainsBytesAndRepeatedSweepCannotReissueLease`: expected `DataIntegrityViolationException` on `storage_key=NULL` but PG18 accepted the change for terminal `EXPIRED` state.
- **Fixed only retention test source**: `a8181ba9b3b2980b0f7756384d3e1d431856cf90` now asserts the state-specific behavior: `EXPIRED` nullable `storage_key` is read-only classified `NO_ARTIFACT_KEY`, and `EXPIRED` null digest is read-only classified `BLOCK_DIGEST`; restores correct staged evidence for valid-case checks. Existing `BACKING_UP` guard against keyless partial evidence remains and expects SQL constraint rejection. No schema modifications.
- **Status**: FIX_COMMITTED, NOT EXECUTED at corrected exact SHA. Request focused PG18 Testcontainers rerun (one package, not extra Full CI), then a combined integration candidate/QA acceptance coordinated by Architecture. No purge, no Restore Engine edits, no production activation.

## RET-CHECK-03 source-corrected, focused execution still pending — 2026-11-19

**Exact DDL inspected on `main`:** `V50__workspace_protective_backup_registry.sql` adds the general `ck_protective_backup_verified` invariant and `V51__backup_artifact_staging_evidence.sql` adds:

```sql
CHECK (
  (artifact_sha256 IS NULL AND artifact_bytes IS NULL AND artifact_stored_at IS NULL)
  OR
  (artifact_sha256 ~ '^[0-9a-f]{64}$' AND artifact_bytes > 0
   AND artifact_bytes <= 67140864 AND artifact_stored_at IS NOT NULL
   AND encrypted = TRUE AND storage_key IS NOT NULL)
)
```

This constraint is **not conditional on `state='EXPIRED'`**. With populated byte count/time and SHA, `storage_key=NULL` makes both branches FALSE and is rejected even for EXPIRED; with `artifact_sha256=NULL`, first branch FALSE but second UNKNOWN, and PostgreSQL CHECK accepts UNKNOWN. The prior two contradictory test expectations concerned different columns. Not evidence of nondeterministic PG behavior.

**Single retention-only corrective test commit** `d61c87c795aa166f2871ea59df0233ee93dba928`: `WorkspaceBackupRetentionRollbackGapPostgresTest.committedExpiryRetainsBytesAndRepeatedSweepCannotReissueLease` now asserts exactly this hybrid: key NULL -> SQL `DataIntegrityViolationException` / `BLOCK_STORAGE_KEY` unchanged; digest NULL -> accepted / `BLOCK_DIGEST`; restore original digest -> `RETAINED_VALID`, bytes still on disk and EXPIRED cannot acquire lease. Distinct BACKING_UP incomplete key remains blocked. **Production code/DDL unchanged.**

**Evidence status**: QA PR #34 previous run 770/0F/1E, not a PASS on this test blob. No disposable PG18 Maven/Testcontainers execution available from this stream; don't label `RET_CHECK_03_READY_FOR_QA` or quote targeted tests as passing until actual exact-SHA result is recorded. Target: `./mvnw -Dtest=WorkspaceBackupRetentionRollbackGapPostgresTest,WorkspaceBackupRetentionPostgresTest,WorkspaceBackupRegistryPresenceAuditServiceTest test`. Do not dispatch new Full CI before focused result per Architecture.
