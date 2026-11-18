# FamilyPoints — Backend Retention (independent lane) ACTIVE QUEUE

**Read first:** [Architect dispatcher](../ARCHITECT_DISPATCH_LATEST.md) on every `++`. This is a **new auxiliary backend lane**, NOT reopening Backend A/C/D. Owner starts this chat manually with `++` when Architecture requests. Operate asynchronously from Backend B and Web, without requesting per-commit approvals.

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
