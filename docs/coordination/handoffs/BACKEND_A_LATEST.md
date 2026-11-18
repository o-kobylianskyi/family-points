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


## B-A-03i — 2026-11-18 multi-PG18 TOCTOU safety tests / operator SOP
- Verified B-A-03h `041fc3b4` Actions `44271220339` overall SUCCESS; prior `9b71ce0` was FAILED, so exact SHA matters.
- `2d9fbb8` / `47112de`: `WorkspaceBackupReconciliationRacePostgresTest` adds three real PG18 race paths: registry commit between filesystem scan and SQL, stale orphan finding followed by registry appearance, concurrent active restore lease creation. All deny deletion, preserve tenant fence.
- QA/operator-only runbook: `docs/coordination/requests/B-A-03I_TOCTOU_RACE_RECONCILIATION_QA_20261118.md` commit `3f47f5a`. This is NOT an approved cleanup operation and has no delete API/scheduler. 24h staleness is a review threshold, not a delete threshold.
- Backend B conditionally ACKed hidden member provenance catalog via `docs/coordination/requests/BACKEND_B_TO_A_MEMBER_SOURCE_CONSUMER_ACK_20261118.md`; Architecture named schema/version approval still pending, so no archive schema change.
- New SHA must get exact-SHA all-four CI evidence. Next B-A-03j: CI fail triage, source cleanup/retention callsite audit, reverse-audit racing registry checks; hold real Restore, HTTP423 and BETA.


## B-A-03j — 2026-11-18 reverse audit registry race guard / physical delete paths
- Previous B-A-03i Actions `44273553572` exact SHA `5f91d269` overall SUCCESS. Later B-A-03j CI must be verified independently.
- `7795b463`: new `WorkspaceBackupRegistryPresenceRacePostgresTest`, separate PG18 JDBC reader/writer, concurrent registry SHA update during ciphertext verification; unrelated family unchanged; no delete.
- `4ded05eb`: after successful stored ciphertext verification, reverse audit re-reads exact workspace/snapshot registry fields and classifies a changed/missing row `REGISTRY_CHANGED_DURING_AUDIT`. `1e9b509` unit asserts drift; `a4ac54e` PG18 expectation updated. Observational only: a later change can still race, never authorize delete.
- Reviewed production physical delete callsites in staging, coordinator, explicit retention and local store. Critical non-atomic filesystem deletion vs SQL rollback retained as P0 for operator workflow. Auditors never issue delete. Exact paths/test packet: `docs/coordination/requests/B-A-03J_REVERSE_AUDIT_RACE_AND_RETENTION_QA_20261118.md` commit `fce7bef`.
- Backend B conditional MEMBER source catalog ACK present; named Architecture wire schema approval still absent. Never add speculative `FAMILY_MEMBERS` to v1 or enable production Restore. HTTP423/BETA HOLD.
- Next B-A-03k: exact-SHA CI triage, disposable physical retention/rollback mismatch tests, then vNext member source only after Architecture ACK.


## Current chat continuation safety — 2026-11-18
- Added `docs/coordination/chat-capacity/BACKEND_A.md` @`ab8bef31`: observable chat capacity unknown, no invented utilization percentages or automatic warnings.
- Exact B-A-03j source/handoff commit `f30b88f`, CI `44274376504` queued at checkpoint. Full four-component PASS must be independently checked before acceptance. Next safe B-A-03k as above.
- New chat ready opening: "Приймай FamilyPoints Backend A. Прочитай AGENTS.md, docs/coordination/handoffs/BACKEND_A_LATEST.md, поточний main SHA і QA B-A-03J; продовжуй B-A-03k великими пакетами, лише producer/backup/retention safety, ніякого Restore Execute." 


## B-A-03k — 2026-11-18 PG18 retention unlink / SQL rollback hazard
- Verified previous B-A-03j SHA `bbc5d866aa4c38d46445fe393f8bca8b3a9a101e` GitHub Actions `44274509971` overall SUCCESS before this batch.
- New `WorkspaceBackupRetentionRollbackGapPostgresTest` @`3ca4231`: actual PostgreSQL 18 + synthetic workspace/backup + real encrypted local file; `expireEligible` deletes disk file, forced outer transaction rollback leaves registry `BACKING_UP`; reverse audit detects invalid/missing ciphertext, other workspace untouched. Independent unlink failure negative checks SQL never claims EXPIRED when unlink fails.
- QA and architectural remediation request `docs/coordination/requests/B-A-03K_RETENTION_SQL_ROLLBACK_GAP_QA_20261118.md` @`bf812d0`: non-atomic filesystem/SQL operation is confirmed design risk, **no production retention refactor or deletion activation**. Recommend separately approved quarantine/intent/alternate-copy state machine with idempotent recovery and lease checks before releasing cleanup.
- New exact-SHA CI/Pg18 test result pending. Backend B conditional original member catalog source ACK only; Architecture version/privacy/FK closure signoff still required. `restorable=false` / `replaySupported=false` / Reset HTTP423 / BETA HOLD unchanged.
- Next B-A-03l: exact-SHA CI verification/fix, then staged retention state machine contract and disposable fault-injection rehearsal, no autonomous production purge.

- Follow-up `0bd68d4`: physical retention fault-injection PG18 test deletes *only its own synthetic registry row* after assertions so shared static Testcontainers test methods cannot accidentally process another test's older expired fixture. Exact-SHA CI pending.


## B-A-03l CLOSED AS DESIGN / B-A-04 STARTED (2026-11-18)
- Baseline `f10537ae` run `44275795521` overall SUCCESS but Java Maven steps **SKIPPED** (job `134703994678`). B-A-03k retention rollback PG18 tests remain **NOT VERIFIED** by that run. This corrects overly broad previous CI-green claims.
- Retention improvement deliberately stopped at separate architecture-approved intent → quarantine → DB confirm → purge design. No production auto purge, new retention workflow or destructive release. Missing ciphertext after SQL rollback remains a documented P0 hazard; no false "repaired" claim.
- Source member contract advances: `a8b8740` `DisposablePortableMemberSourceCatalog` TEST ONLY, reads minimal scoped `family_roles`/ `family_members` original IDs, roles, type, active, tombstone under REPEATABLE_READ, **no names/login/accounts**. `1e8cbdd` PG18 two-family fixture tests original tombstones, privacy and foreign-role/weak isolation failures. Production exporter/inspector unchanged.
- Source/wire approval request `docs/coordination/requests/B-A-04_MEMBER_SOURCE_FIELDS_AND_WIRE_GATE_20261118.md` `f63f487` requests exact A4-01…09 decisions from Architecture, B, C, D and Release. User accepted product DEC-01…08; Backend B conditional logical consumer ACK; Architecture field/schema version/IMP still missing.
- Backend A independent journal `docs/coordination/logs/backend-a.md` created `19fbb5a` with verification, ownership and next handoff. Explicit `[full-ci]` request to follow; check exact current SHA and job steps, not just overall workflow badge.
- NEXT B-A-04b (after approval): versioned source roster + signed digest/inspector/legacy compatibility; until then test-only full FK ancestry, typed source/role category matrices. No production Restore/Reset; BETA HOLD.


## B-A-04b — 2026-11-18 TEST-ONLY original-member FK graph closure
- Baseline previous explicit `[full-ci]` run `44277446121` at `3cbe37b`: pending Java21/Maven at observed pickup; never claim backend tested until job and Surefire evidence. Web, Android, Compose jobs were green.
- `db9e4eb`: test-only `DisposablePortableMemberReferenceClosure` curated source FK matrix for TASK_EXECUTIONS, TASK_DEFINITIONS, POINT_LEDGER, REPUTATION_LEDGER, REWARD_PURCHASES, POINT_EXCHANGE_REQUESTS, GROUP_MEMBERSHIPS and TASK_DELEGATIONS. Executor and nullable historical original assignee separate, missing optional field explicitly rejected; fail closed on unknown categories, foreign or duplicate member, oversized input. `426fef3` negative fixtures.
- `b795d5a`: PG18 Flyway + information_schema schema drift test checks actual source member-reference columns and nullable status. `4cdb874`: source roster test-only reader now caps 2000 member/role rows, denies duplicate role code and nonintegral IDs. No production archive files modified.
- QA packet `docs/coordination/requests/B-A-04B_MEMBER_REFERENCE_CLOSURE_QA_20261118.md` commit `3392121`; Backend A journal `docs/coordination/logs/backend-a.md` commit `018bb8d`.
- Remaining: signed source vNext A4-01…09 technical ACK by Architecture and field-level D/C/B/Release; full 23-category closure, source TaskInstance snapshot, media/financial polymorphic parent graph; no auto login matching/owner proof. BETA HOLD and Reset HTTP423.
- NEXT B-A-04c: verify current exact-SHA full CI and fix test failures; expand test-only source category closure using real Flyway FK catalog and D/Backend B requirements; implement vNext serialization only after explicit named schema/IMP approval.
- New-chat opening: "Продовжуй FamilyPoints Backend A. Прочитай AGENTS.md, docs/coordination/handoffs/BACKEND_A_LATEST.md, main HEAD, B-A-04B QA та актуальний CI. Роби максимально великими пакетами. Не змінюй wire vNext до Architecture ACK, не розблоковуй Restore."


## B-A-04c — CI defect triage and actual PostgreSQL member-FK audit (2026-11-18)
- Important corrected evidence: `32cff0f` run `44278301617` **FAIL** Java Maven `testCompile` unknown symbol `n` in `DisposablePortableMemberSourceCatalog.java:98`; Web/Android/Compose passed. Fixed `b6c4669`. Do not represent stale docs-only workflow SUCCESS as Java test PASS.
- `45c7ff9` PG18 negative test now respects real V28 cross-family role trigger, then injects synthetic legacy corrupt data with trigger temporarily disabled ONLY in disposable DB, restores it in finally; source reader must fail closed.
- `e58300b` source reference matrix now includes required `MEMBER_EXCEPTION_PERIODS.member_id`; `3d14025` verifies its PG18 FK nullability. `e29f276` / `be84906` new PG18 FK catalog inventory reads `pg_constraint` for all original-member parent edges; prints unreviewed columns and enforces that previously covered categories have no silently uncovered new FK.
- QA packet `docs/coordination/requests/B-A-04C_CI_FIX_AND_MEMBER_FK_INVENTORY_QA_20261118.md` @`b6e9ce7`, journal `docs/coordination/logs/backend-a.md` updated. All new PG tests require exact-SHA Maven execution; queued/in-progress does not equal PASS.
- Architecture A4-01…09 exact format/privacy/role categories/IMP approval still needed; Backend B logical conditional ACK only; no production archive schema change or owner/account export. Restore/Reset HTTP423 and BETA HOLD unchanged.
- Next B-A-04d: verify actual Maven error/test counts at new pinned full CI, repair failures, enumerate outstanding polymorphic/reference category owners, prepare vNext producer only after signed Architecture review.


## B-A-04c full run dispatch (2026-11-18)
- Explicit four-component `[full-ci]` on source SHA `a67f3c388de583d3c6598eae4e88834449781e5d`, run `44281984930` IN_PROGRESS at inspection. This includes compile repair, V28 PG negative and `pg_constraint` real FK inventory; no passing test count yet.
- QA packet `docs/coordination/requests/B-A-04C_PINNED_FULL_CI_20261118.md` includes exact test cases and fail-safe reporting requirements. Return to this run for Java Maven error/actual PG18 execution and all component test steps before claiming accepted. Next task B-A-04d as above.


## B-A-04d — 2026-11-18 source graph MVCC snapshot and CI tracking
- Full 4-component CI `44278301617` SHA `32cff0f` failed Java testCompile, corrected in `b6c4669`. Later Full CI `44281984930` SHA `a67f3c` still showed Java/PostgreSQL **IN_PROGRESS** with Android/Web/Compose success; no full PASS claim.
- New `92e624e` disposable PG18 test `pinnedRepeatableReadRosterNeverMixesConcurrentNewMembers`: initial `REPEATABLE_READ` source member snapshot remains stable after another JDBC connection commits new member; fresh source transaction sees new member. Test-only fixture; production archive unchanged.
- QA packet `docs/coordination/requests/B-A-04D_MVCC_AND_CI_QA_20261118.md` @`e51ebde`, inspect exact non-skipped Java/Maven test report before acceptance.
- Source graph still missing signed Architecture A4-01…09 fields/version/IMP, C/D full semantic review and B exact wire ACK. Next B-A-04e: CI red triage, negative source edges, prepare approved vNext only after decision. No credentials, Restore Execute, reset activation or retention purge; BETA HOLD.


## B-A-04e — 2026-11-18 strict original source identity & exception-period FK closure
- **VERIFIED** previous exact-source Full CI `64f4cdea96b9df2e6e29006e229960c0a83e7f08`, run `44282865822`: Java21/Maven/Testcontainers, Web, Android, Compose and full-ci attestation SUCCESS; actual Maven `Run all backend tests` step SUCCESS, Surefire report artifact `14418511259`. Prior B-A-04d RUNNING note is now superseded.
- `36e3a16c`: test-only source membership validator now strictly admits positive Integer/Long original source IDs, rejects fractional BigDecimal/BigInteger aliasing, duplicate/blank role codes, unsupported member type and >2000 member/role nodes. `4034754d` adversarial unit tests prove fail-closed identity aliasing, catalog limits and exception-period parent.
- `33679c7d`: new Testcontainers PostgreSQL18 `DisposablePortableMemberExceptionClosurePostgresTest` uses *actual* two-family member-exception-period SQL rows and repeatable-read scoped catalog; rejects accidentally included neighbor member without inferring roles or names.
- QA packet `docs/coordination/requests/B-A-04E_STRICT_SOURCE_ID_AND_EXCEPTION_CLOSURE_QA_20261118.md` @`2f8a971f`. No production source exporter/inspector/manifest/REST/SQL migration touched; no global auth identity export.
- Note new member-exception-period ancestry is outside current v1 23-category export; vNext category/format explicit Architecture A4-01…09 + IMP and B/C/D/Release ACK remains OPEN. No guessing original ownership, no target account linking.
- Exact-SHA CI for B-A-04e pending. NEXT B-A-04f: CI/test failures and category-by-category historical FK closure with B/D consumer acceptance; no Restore Execute, HTTP423, BETA HOLD.
