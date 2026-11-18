# FamilyPoints — Backend A ownership and first source-contract audit

**Date:** 2026-11-18. **Track:** Backend A — Backup & Portable Archive. **Requested by project owner.**
**Baseline inspected:** main at `74b0b37bc1ebbaa301430c09108e3bc91814c8b9` (check HEAD before each edit). **Status:** SOURCE REVIEW / CONTRACT GAP; not CI verified.

## Ownership boundary
- Backend A owns source-side capture/export; selected FULL/SELECTIVE category closure; protected family-scoped snapshot; stable source metadata and digest; encrypted portable file/manifest/versioning/retention; offline producer-side validation; archival privacy and historical FAMILY_MEMBERS contract after Architecture approval.
- Backend B owns consumer-side restore engine, original→target ID maps, durable authorization, atomic SQL replay/rollback, idempotency/receipts and disposable target tests. Its handoff is `docs/coordination/handoffs/BACKEND_B_LATEST.md`.
- Shared producer↔consumer contract requires explicit Backend B ACK and Architecture approval before any schema/category breaking change. No unilateral modification of Backend B disposable restore executors.
- Legacy `BACKEND_LATEST.md` and `logs/backend.md` remain historical; new work should use distinct `B-A-XX` identifiers.

## B-A-01 — evidence-based source audit (completed review only)
Source files reviewed:
1. `WorkspacePortableBackupExporter.java`: SCOPED_TABLES currently enumerates 23 categories, includes POINT_TYPES, POINT_EXCHANGE_RATES and POINT_EXCHANGE_REQUESTS but **no FAMILY_MEMBERS**. It exports `family-points-export-v1`, family/workspace ID and selected category arrays, validates dataEpoch and source snapshot digest, uses explicit fixed SQL scope, MAX_ROWS 2000 and MAX_BYTES 12 MiB. File header expressly states media blobs need separate handling, and there is no VERIFIED promotion.
2. `ProtectiveBackupImportInspector.java`: fixed matching 23-category allowlist. `checkMemberParent` explicitly marks every present positive member_id as `IDENTITY_REBIND_REQUIRED`, because member provenance is intentionally missing. Inspector is offline-only and cannot make archive restorable.
3. `ProtectiveBackupArtifactCodec.java`: AES-sealed file manifest includes snapshotId, workspaceId, operationId, dataEpoch, sourceDigest, schemaVersion, timestamp, plaintext hash and length. Existing crypto/manifest compatibility must be preserved.
4. Backend B handoff at `74b0b37`: original member catalog/historical membership source provenance is P0 blocker, and current archive does not prove portable identities after complete source DB loss.

**Conclusion:** Adding a literal `FAMILY_MEMBERS` entry to the exporter alone is NOT a safe solution: archive version/allowlist, source digest/snapshot closure, privacy, member account roles and consumer verification must be updated together. Historical membership and source ID ancestry must be signed by archive, not inferred from target ID/name/role.

## Producer↔consumer contract questions (Architecture approval required)
- Define minimal portable member representation: stable source member identifier and family ancestry; distinguish account-global identity from workspace membership; privacy-minimized identity fields; role/status at snapshot; historical/removed member references without revealing secret auth attributes.
- Define deleted/merged member semantics and all member-reference columns (mandatory/optional) across archived categories. Original member_id must not be treated as target id.
- Decide whether `FAMILY_MEMBERS` is a mandatory hidden dependency for every scope with member references, a selectable visible category, or both (scope semantics and import blockers must be explicit).
- Specify archive schema versioning: keep old v1 readable as non-restorable where necessary, introduce new version only after Backend B consumer agreement; AES manifest schemaVersion and JSON format are separate version fields and must not silently diverge.
- Define source atomicity under concurrent writes and dataEpoch changes: selected-scope digest plus all categories should refer to one consistent database snapshot. Current `@Transactional` alone does not prove repeatable read/snapshot isolation.
- Agree explicit size/row limits and media binary handling; never silently truncate or omit data; reject over-limit with stable errors.
- Define QA negative cases: malicious altered encrypted file, cross-tenant member, removed historical member, missing/duplicate parent, target ID collision, same account in multiple families, concurrent change during snapshot, partial category subset, old archive compatibility, missing media content.

## Proposed ordered work
- **B-A-02 (next):** prepare precise `FAMILY_MEMBERS` schema/column/foreign-key inventory from Flyway + entity + service; propose privacy-minimized vNext archive contract to Architecture and Backend B. No production member export before approval.
- **B-A-03:** implement approved source category + digest + inspector checks, schema compatibility and encrypted immutable member ancestry. Add unit and two-tenant PostgreSQL integration cases.
- **B-A-04:** complete category-closure and media/financial dependency export tests, inspect partial FULL/SELECTIVE semantics and limit handling.
- **B-A-05:** evaluate transactional export consistency, source digest/epoch races, retention and encryption boundary; regression suite and exact-SHA CI evidence.

## Safety and delivery state
- **Implemented in this package:** ownership handoff + source audit documentation only. **No Java or production mutation. No new tests executed.**
- **Not implemented/accepted:** original FAMILY_MEMBERS archive, vNext contract, producer↔consumer ACK, independent exact-SHA full CI.
- **Release gates unchanged:** Restore Execute production blocked (`restorable=false`, `replaySupported=false`), Reset Execute HTTP 423, BETA HOLD. Synthetic/disposable data only; no real family reset.
- **Coordination:** Backend B handoff, Architecture `logs/architecture.md` and `IMPACT_REGISTER.md`, QA issue #15. Send precise contract proposal before changing shared archive format.


## B-A-02 completed — 2026-11-18 (documentation and source-schema evidence)
- Verified V1 family_members fields and WorkspaceMember tombstone behavior; V21/V22 global account→workspace membership and DB scope trigger; V29 optional legacy account pointer/external identity separation; V31 shared email collision; GroupMembership is separate from account membership.
- Published producer contract proposal: `docs/coordination/requests/B-A-02_PORTABLE_MEMBER_SOURCE_CONTRACT_20261118.md` (commit `1ede3ee`). Includes minimum privacy-safe source node, original FK/role ancestry, soft-delete, account link risk, versioned wire format, legacy compatibility and 12 negative cases.
- NO runtime Java/schema changes or tests; awaiting Architecture and Backend B ACK before source format modifications.
- Next B-A-03: only once approved, implement archive-category/digest/inspector updates with PG18 two-tenant tests; meanwhile independently audit source snapshot isolation/limit behavior without changing restore execution.


## B-A-03a — 2026-11-18 export snapshot isolation safeguard
- Verified exporter uses first digest, per-table rows and final digest while PostgreSQL default READ_COMMITTED may observe multiple MVCC statement snapshots. Family-row lock only fences cooperative writers.
- Source change `a9c9b667`: exporter `@Transactional(isolation = Isolation.REPEATABLE_READ)` when initiating its own Spring-proxied transaction; no archive format/restore changes.
- Test source `b49191ed`: transaction declaration regression, **NOT_RUN** in this track. QA packet `docs/coordination/requests/B-A-03A_SOURCE_SNAPSHOT_ISOLATION_QA_20261118.md` commit `a52a277`.
- **Remaining P0 hazard:** if REQUIRED exporter joins caller's READ_COMMITTED transaction, the annotation does not raise isolation; current direct-instance tests also do not test proxy. Must review actual callsites and add true PG18 concurrent integration before asserting atomic capture.
- B-A-02 FAMILY_MEMBERS wire contract still awaits Architecture / Backend B ACK; do not merge a guessed category or turn on Restore Execute.
- Next: B-A-03b callsite/outer-transaction inventory and fail-closed production isolation boundary, plus controlled PG18 concurrency test.


## B-A-03b — 2026-11-18 transaction join safety
- Identified real callsite: coordinator `registerAndStage` → staging `stage` → exporter `export`; coordinator formerly defaulted READ_COMMITTED, so standalone exporter REPEATABLE_READ annotation was overridden by joining the outer transaction.
- `1520bcb`: annotated coordinator REPEATABLE_READ; `c7f89b8`: fail-closed guard for weak or unknown Spring transaction isolation before registry/file side effects; `9791430`: negative unit tests.
- QA handoff `docs/coordination/requests/B-A-03B_STAGING_ISOLATION_QA_20261118.md` commit `1f61d55`.
- Outstanding: independent PG18 Spring-proxied transaction test and two-connection concurrent source updates, direct staging/export callsites outside coordinator, exact-SHA CI PASS evidence. B-A-02 wire-contract approval still pending.
- Next B-A-03c: real transaction and concurrent isolation regression; B-A-04 after Architecture+Backend B approve member catalog.


## B-A-03c — 2026-11-18 actual PostgreSQL MVCC concurrency regression
- Commit `344d235`: new Testcontainers PG18 `WorkspacePortableBackupConcurrentSnapshotPostgresTest`. One repeatable-read source transaction pins a family scope snapshot/digest while second independent connection updates a member group without family-row fencing; verifies encrypted archive holds old coherent row and original digest, newer DB row committed, neighbor untouched, `restorable=false`.
- QA packet `docs/coordination/requests/B-A-03C_CONCURRENT_SNAPSHOT_QA_20261118.md` commit `c4e8f4c`. Authored test **NOT_RUN** by connector; actual CI PASS must be pinned to its SHA.
- Existing real staging path guard B-A-03b remains; Spring AOP entrypoint integration and direct-stage bypass checks still pending. Member catalog producer→consumer contract still awaits Architecture/Backend B ACK. HTTP423/BETA HOLD unchanged.
- Next B-A-03d: test real Spring transaction proxy and stage entrypoint isolation; then pursue approved source member catalog.


## B-A-03d — 2026-11-18 direct staging/proxy isolation
- `6570877`: direct `WorkspaceBackupArtifactStagingService.stage` now requires an active Spring transaction + synchronization + REPEATABLE_READ before exporter or filesystem writes.
- `a1b0dca`: staging unit tests initialize/clear repeatable-read fixture and explicitly deny READ_COMMITTED, null and absent transaction, with no side effects.
- `94de34f` / `a714796`: PG18 `WorkspaceBackupStagingProxyIsolationPostgresTest` exercises actual `TransactionInterceptor` on coordinator; validates physical DB isolation and rejects weaker outer caller before any registry/staging/storage interaction.
- QA packet `docs/coordination/requests/B-A-03D_STAGING_PROXY_ISOLATION_QA_20261118.md` commit `9782aa2`.
- New CI exact-head acceptance **pending**; never infer PASS from source commits or prior SHA. RESTORE remains disabled, Reset Execute HTTP423, BETA HOLD. Archive FAMILY_MEMBERS source privacy contract awaits Architecture/Backend B ACK.
- Next B-A-03e: resolve any CI regressions, inspect orphan-file reconciliation/direct authorized staging routes, then B-A-04 only after schema approval.


## B-A-03e — 2026-11-18 partial file publication and stage readback compensation
- Fixed physical orphan on local store partial-success path: `4f1afe5` deletes only files linked by the current failing call after publication; preexisting snapshots remain untouched.
- Fixed storage-success/validation-failure staging path: `84a95df` deletes newly stored file after failed readback/SHA/manifest and keeps cleanup failure as suppressed on original exception.
- Added two negative unit tests `2b716c7`; QA packet `docs/coordination/requests/B-A-03E_ORPHAN_COMPENSATION_QA_20261118.md` commit `3c00f4a`.
- No test results are claimed for this exact source SHA until CI completes. Prior B-A-03d workflow `44268359058` reported SUCCESS. Crash/power-loss orphan reconciliation and operational cleanup metrics remain P0 gaps.
- Next B-A-03f: physical file compensation and crash/restart orphan reconciliation rehearsal with disposable files, no live families. B-A-02 member archive still awaiting Architecture + Backend B ACK.


## B-A-03f — 2026-11-18 bounded read-only restart orphan audit
- `a25c15a`: `WorkspaceBackupLocalArtifactStore.inventory` lists bounded per-family entries with no symlink following/secret reads/deletion; `695f18f`: `WorkspaceBackupOrphanAuditService` classifies source files against registry without promoting status or deleting bytes.
- `a6470b8` / `5e0f2c9`: file-inventory crash-temp fixture and registry classifications read-only tests. QA packet `docs/coordination/requests/B-A-03F_RESTART_ORPHAN_AUDIT_QA_20261118.md` commit `a192e80`.
- Pending: exact-SHA CI and physical SHA verification, race/lease-aware operator reconciliation policy. `REGISTERED_FILE_PRESENT` is never equivalent to authenticated, VERIFIED or restorable.
- Next B-A-03g: audit SHA (no plaintext), concurrency tests and operator retention-only deletion approval. B-A-02 FAMILY_MEMBERS still awaits Architecture + Backend B ACK. Restore execute HTTP423/BETA HOLD unchanged.


## B-A-03g — 2026-11-18 ciphertext integrity orphan audit
- `ef05216` adds no-decrypt bounded ciphertext SHA/envelope preflight with zeroed transient bytes; `439c4e7` / `265cd3a` add read-only audit classifications for intact/corrupt/unreadable ciphertext.
- `a1d47d1` / `28c389c`: negative registry SHA/malformed evidence and physical tamper tests; no auto deletion. QA packet: `docs/coordination/requests/B-A-03G_CIPHERTEXT_INTEGRITY_QA_20261118.md` commit `4c60f23`.
- B-A-03f CI `44269230342` reported SUCCESS on exact SHA `1c41fc9`. This new packet remains CI unverified until Actions completes.
- B-A-03h next: explicit operator-only, age/lease-aware reconciliation design and restart/race tests; never auto delete encrypted archives based solely on audit. FAMILY_MEMBERS format still awaiting Architecture and Backend B ACK. BETA HOLD / HTTP423 unchanged.


## B-A-03h — 2026-11-18 large package: restart/lease/age guard and reverse audit
- Verified prior B-A-03g exact-SHA `4f332030` workflow `44269850500` SUCCESS. This does not certify this newer packet.
- `99ab374` extends no-follow encrypted archive inventory with last-modified timestamp (old fixture constructor preserved).
- `f03404b`: explicit read-only `WorkspaceBackupReconciliationAssessmentService`. Requires 24h elapsed age before even marking orphan/temp **REVIEW_ONLY**; active restore lease, in-progress/expired registry, future/unknown timestamps, symlinks and unrecognized files all hold; every finding `deleteAuthorized=false`, no HTTP or automatic cleanup.
- `4793875`: deterministic age/lease/tombstone/overflow and no-delete tests. `303a72c`: real Testcontainers PG18 + filesystem post-restart rehearsal with live lease, lease release, BACKING_UP and EXPIRED registry, orphan ciphertext and crash temp, cross-tenant invariants.
- `5f2dcfd`: separate bounded reverse `WorkspaceBackupRegistryPresenceAuditService` to detect DB registry rows whose files are missing/invalid without decrypting; `d3b4e3e` tests missing/corrupt/malformed/oversized scans, read-only safety.
- QA contract `docs/coordination/requests/B-A-03H_RESTART_LEASE_RECONCILIATION_QA_20261118.md`, updated in `b041939`. No file deletion, schema migration, production restore or reset execution.
- **Pending:** current exact-SHA workflow green + PG18 job results, verification of concurrency on new audit, operator-approved physical cleanup procedure, source member archive B-A-02 Architecture+Backend B ACK.
- **Next B-A-03i:** diagnose any test regressions and add concurrency/time-of-check audit negative cases with exact-SHA CI; once signed source format is approved, B-A-04 original member catalog/category closure. BETA HOLD, HTTP423 unchanged.


## B-A-03h CI triage and follow-up (2026-11-18)
- Older run `44270520060` at new PG18 test SHA `303a72c` failed Java testCompile: Testcontainers package `org.testcontainers.postgresql.PostgreSQLContainer` is non-generic. This Backend A fixture incorrectly used `PostgreSQLContainer<?>` / diamond. Fixed locally in repo source commit `9b71ce0` (non-generic class/constructor); another concurrent finance fixture generated similar errors in earlier SHAs and belongs to its owner.
- Handoff baseline `0c59f06` run `44270985978`: Java Maven/PostgreSQL, Android, Web, Compose all SUCCESS; synthetic Full CI exact-SHA evidence job was SKIPPED, not an additional passing job. Earlier failure snapshots do not invalidate later separately verified successes.
- Exact-SHA CI for latest fix `9b71ce0` in progress as of handoff. Do not mark that SHA complete until its own jobs finish and test count is checked.
- The new reverse registry-presence audit is part of this B-A-03h release batch; no auto cleanup, archival version change or Restore unlock.
