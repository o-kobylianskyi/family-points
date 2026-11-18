# FamilyPoints — Backend B handoff / ownership (2026-11-18)

**Current direction:** Backend B — Restore Engine. **Assigned by project owner in chat on 2026-11-18.** Architecture's recent working split (Backend A — Backup & Portable Archive; Backend B — Restore Engine) is accepted as the intended coordination boundary. The canonical GitHub `logs/architecture.md` and `README.md` still refer to a single Backend at the time of this handoff, so final shared ADR/impact-record routing is **pending Architecture acknowledgment**, not silently asserted.

## Scope owned by Backend B
- Restore/import execution engine for **FULL and SELECTIVE** recovery, scoped transaction and dependency order.
- Source→target **member, currency, rate, task, reward, group, media and ledger FK/ID mapping** at the Restore boundary. Must never infer identity from numeric ID/name/role coincidence.
- Restore-time immutable source graph verification, idempotent replay/exactly-once operation, authorization consumption **in the same DB transaction as data writes**, rollback/no partial writes, restore receipts, recovery from failure, tenant isolation.
- Test-only PostgreSQL 18 rehearsals, negative/security tests, restore-scope API contracts **only after Architecture approval**; QA handoff on pinned exact SHA.
- Integration tests of archive **consumer**: Backend A is producer; B may read sealed V50 archive and request missing source fields/signed original membership via Architecture, but must not unilaterally change archive format.

## Scope owned by Backend A (not Backend B)
- Backup capture/exporter, scope enumeration, snapshot source digest, artifact encoding/encryption, portable archive schema/version V50+, integrity and retention of source archive, archive format migration and approved original `FAMILY_MEMBERS` source catalog.
- Backend B reviews/ACKs archive contract and supplies precise restore-consumer requirements. No conflicting writes to `WorkspacePortableBackupExporter`, `ProtectiveBackupArtifactCodec`, `ProtectiveBackupPortableFile`, `ProtectiveBackupImportInspector` or migration schema without named cross-track agreement.
- Backend A should not independently enable Restore Execute or implement the consumer replay transaction.

## Concrete transition from former unified Backend work
- Existing `docs/coordination/handoffs/BACKEND_LATEST.md` and `docs/coordination/logs/backend.md` remain immutable historical predecessors; append-only only.
- Earlier Backend BKL-002 batches #25–64 span financial source preflight and target restoration. Their **restore-engine, explicit ID-map, rollback/authorization, PostgreSQL rehearsal** tests belong to Backend B.
- Source archive creation and original-source-category schema decisions belong to Backend A. Existing TEST-ONLY `DisposableLedgerSourceGraphVerifier` is **shared contract-sensitive**: Backend B maintains consumer-side verification/tests; any additional original source fields or category changes require Backend A + Architecture review.
- Begin Backend B's separately labeled work with **B-B-01** rather than ambiguous unified package #65. Separate journal entries, tests, QA requests, and status.

## Observed safety state / CI
- On Nov 18, previous exact-SHA runs #44196224270 (493 Maven/2 failures) and #44262590272/#44262614624 (496 Maven/1 failure each) documented finance exception expectations. Later repair/source commits exist, but **current exact-SHA all-four PASS NOT VERIFIED** in this handoff; verify Actions jobs/logs, no PR-only lookup shortcuts.
- Current test-only cross-category work #61–64: `DisposableDurableRestoreApprovalStore` QA-only SQL, `DisposableMemberMappingPreflight`, `DisposableMemberBindingReceiptVerifier`, source verifier and 2-PG18 restore rehearsal. None establishes real owner reauthentication, production durable grants or portable identity after source DB loss.
- **Production RESTORE:** `restorable=false`, `replaySupported=false`; Reset Execute HTTP423; BETA HOLD. Never reset actual families without fresh explicit scoped authorization.

## Critical P0 dependencies on Backend A / Architecture
1. Authenticated, privacy-reviewed portable **original member catalog / historical memberships** with schema/version and immutable ID ancestry. Current V50 lacks `FAMILY_MEMBERS`.
2. Full category closure and clear semantics for `REFUND`, points vs reputation ledger, purchases, media, family-wide vs selected restoration.
3. Valid owner/guardian authentication and explicit source→target identity pairing approval; cross-family/shared-account/soft-deleted member rules.
4. Durable approved Restore receipt/operation and release gate with exact SHA all four jobs and QA. **No speculative production unlock.**

## Backend B next work
- **B-B-01:** independent consumer-side archive/target dependency matrix and contract-gap request to Backend A + Architecture: original members, account/role privacy, all FK categories and historical ledger ancestry.
- **B-B-02:** synthetic two-database restoration crash/rollback proof with exact FK/ID mapping; no live DB.
- **B-B-03:** design and implement production restore only after architecture + Backend A artifact contract approved, with explicit owner authentication and migration/release review.
- Hand all real user/device E2E to QA, Release does deployment; Backend B owns repeatable synthetic integration tests.

## Coordination endpoints
- Backend B: `docs/coordination/handoffs/BACKEND_B_LATEST.md`, `docs/coordination/logs/backend-b.md`
- Legacy read-only intake: `docs/coordination/handoffs/BACKEND_LATEST.md`, `docs/coordination/logs/backend.md`
- Architecture: `docs/coordination/logs/architecture.md`, `docs/coordination/IMPACT_REGISTER.md`
- QA: GitHub Issue #15 / exact-SHA and Postgres18 evidence.

## B-B-01 completed — 2026-11-18
- Reviewed exact current `WorkspacePortableBackupExporter.SCOPED_TABLES` and `ProtectiveBackupImportInspector` parent checks: **23 V50 categories**, original `FAMILY_MEMBERS` omitted; inspector emits identity/currency/polymorphic ancestry blockers and `RESTORE_ENGINE_NOT_IMPLEMENTED`, `restorable=false`.
- Matrix `docs/coordination/requests/BACKEND_B_RESTORE_DEPENDENCY_MATRIX_20261118.md` @`99f5f812`: owner/scope per category, selective FK graph, conservative stage order, source/target trust gaps and producer/consumer acceptance. Backend A/Architecture ACK pending; no shared schema or importer changes.
- Next Backend B B-B-02: isolated PG18 target mapping/rollback test harness covering target category order and missing-parent rejection, **without** filling producer field gaps by guessing. Latest CI exact-SHA verified PASS still not asserted. Production HTTP423/BETA HOLD.

## B-B-02 completed in source (2026-11-18), exact-SHA QA pending
- New **TEST-ONLY** two-separate-PG18 graph consumer `DisposableTwoDatabaseRestoreGraphExecutor` @`c2921656`/`ee39aef7`/`36966d27`: source-member roster requires both DBs; exact caller source→target map; preserve tenant isolation; selected member group + two-level work-node graph parent-first; source FK closure; target epoch row lock, empty-target conflict guard; inserts in one target transaction; injected 3-stage failure full rollback.
- New `DisposableTwoDatabaseRestoreGraphPostgresTest` `4c9cf1de`, `129e9648`: missing parent, foreign member, unknown source node/group, duplicate IDs deny; independent target neighbor preserved; source ID→target remapped child.parent_node_id; 3 failure stages retry/duplicate and concurrent same-target 1 success tests.
- Formal QA packet `docs/coordination/requests/BACKEND_B_BB02_TWO_DB_RESTORE_GRAPH_QA_20261118.md` @`5e8f1a39`. New targeted Maven/Postgres NOT_RUN independently. Verified older full four-component runs #44264218493 Maven 506 green and #44265994374 Maven 507 green/0 skipped, but neither covers new class.
- Architecture ABCD clarified: **C** exclusively owns owner-auth and durable security grant/Reset executor, B only consumes approved grant in Restore transaction; **D** financial independent validators; **A** archive source/crypto. Request contract ACK before production API/format.
- Next B-B-03: extend source/target FK graph to financial/currency and task/reward dependency closure using approved source archive; author no speculative real Restore before signed member source contract. Production `restorable/replaySupported=false`, Reset Execute HTTP423/BETA HOLD.

## Backend B B-B-03 — TEST-only source completed 2026-11-18; exact-SHA QA pending
- Restore-consumer `DisposableRestoreFinancialDependencyPreflight` commits `cbc520e9`/`4b733d78` maps explicit source→target member, currency, task definition, reward definition and purchase IDs, checks source TASK_EXECUTIONS and REWARD_PURCHASES parent closure; TASK signed EARN/PENALTY lineage and purchase exact historic SPEND amount/member/currency, manual no source ID, reject duplicate awards and unsupported REFUND/POINT_EXCHANGE. Result includes checked mapped target member×currency expected balances.
- Pure unit fixture `DisposableRestoreFinancialDependencyPreflightTest` `e87240d0`/`93af5b44` validates TASK +5 / purchase −4 / MANUAL +2 => +3 and negatives for source ownership, parents, misprice, duplicated events, mapping, unsupported provenance. No target inserts; no A portable archive format edit; no C authorization or D accounting validator edit.
- QA packet `docs/coordination/requests/BACKEND_B_BB03_TASK_REWARD_FINANCIAL_PREFLIGHT_QA_20261118.md` @`a22bd623`. New exact SHA Maven NOT_RUN independently. Prior 507 PASS full CI #44265994374 predates new code. Need pinned QA targeted and four-component.
- A signed source member provenance, C actual owner grant and D financial REFUND/reputation/exchange closure still required before production. Production restorable/replaySupported false, Reset Execute HTTP423/BETA HOLD.
- Next B-B-04: test-only target ID/FK insertion and parity under PG18, reviewed contracts first for prod.

## Backend B B-B-04 — source submitted 2026-11-18, current-SHA QA pending
- TEST ONLY mapped financial target rehearsal `DisposableFinancialTargetRehearsal.java` @92b56372,31382da5. Uses B-B-03 read-only source graph and exact member/point/task/reward/purchase maps, checks target epoch and mapped target member/currency, inserts QA-only task/reward/purchase/point ledger FK tables under one PostgreSQL transaction, verifies net balances, refuses duplicate target. Rejects request-owned purchases in bounded scope; D owns extended finance semantics.
- PG18 test `DisposableFinancialTargetRehearsalPostgresTest` @349a2f3d,dbd59033: family-scoped target mapped ID fixtures, real FK constraints, +5/−4/+2 => +3, 3-stage rollback/fresh retry, stale epoch/wrong buyer/currency/altered historical spend negative, concurrency one success/one rejection, unrelated family unchanged.
- QA packet `docs/coordination/requests/BACKEND_B_BB04_FINANCIAL_TARGET_REPLAY_QA_20261118.md` @021467b6. New SHA tests NOT independently run; earlier full four-component green #44266982944 Backend 514 predates new changes.
- **Important gap:** this stage uses QA-only FK tables, not live `task_instances`/`reward_purchases`/`point_transactions`. TASK ledger source_id not yet remapped to newly allocated target TaskInstance ID; portable original identity, C owner grant, D REFUND/group/reputation closure remain blockers. Production Restore remains HTTP423/BETA HOLD.
- Next B-B-05: native target task execution ID rebinding and historical source semantic closure under approved A/C/D contracts, first in disposable PG.

## Backend B B-B-05 — task execution lineage rebound (2026-11-18)
- Reviewed production `TaskInstance`: distinct `task_instances.id` and `task_definition_id` plus member FK, scheduled date/slot key unique. TASK ledger `source_id` must refer to target task **execution** rather than task definition or old numeric ID.
- Extended `DisposableFinancialTargetRehearsal` `0cbe423a` to insert QA-only task execution parent rows in same target transaction, return immutable original→target execution ID map and rebind TASK ledger `source_id`. `101b5a94` reconciles every polymorphic TASK / REWARD_PURCHASE target parent, source row and same target member/currency/family. No unsupported source auto-inference.
- PG18 test `5b59ce64`: new QA-only task execution physical FK table, verify ledger points to new mapped execution and task/member FK, absent source execution/definition rejects with no writes, 4-stage crash rollback, safe retry and prior concurrent/tenant tests. `4e3f1af3` removes invalid assumption target ID must numerically differ from source.
- QA packet `docs/coordination/requests/BACKEND_B_BB05_NATIVE_TASK_EXECUTION_REBIND_QA_20261118.md` @`261abb8d`. **Latest new changes NOT independently Maven PASS**; old 514 green predated these. Run pinned B-B-03/04/05+PG18 suite and all-four non-skipped [full-ci].
- **Bounded test only**: QA-only FK tables, no native production `task_instances` insertion, no complete historical TaskInstance snapshot fields/target unique collision handling, no owner/auth grant or source member provenance. Backend A, C, D contracts pending; HTTP423/BETA HOLD.
- Next B-B-06: historical TaskInstance status/date/slot/reward/penalty snapshot and assignment mapping, same-family unique constraint, source→target parity with D validated finance under architecture ACK.

## Backend B B-B-06 large package — 2026-11-18 / TEST ONLY
- Reviewed real `TaskInstance` & `TaskInstanceStatus` / `TaskExcuseReason` enums, slot/date unique key and previous red CI QA-050 (#44268695217, Maven 572/1 failure/1 error). Never weakened DuplicateKeyException expectation.
- New `DisposableTaskInstanceSnapshotValidator` `fd5dc71c`/`c35c8a5b`: strict historical task member/definition/original assignee mapping, dates/times, status/excuse, claimedAt, title/description/mandatory, due time, slot, reward+penalty POINTS/reputation snapshots, reward/penalty processed flags, and exact TASK ledger economic provenance. No guessing source fields if missing, accepts reputation-only processed reward without invented POINTS.
- `DisposableFinancialTargetRehearsal` `08fcfd83`/`b93222a1`: duplicate source tuple detection, no target writes if missing/forged history; QA-only PG18 task execution insert with original snapshot fields/FKs/date+slot, mapped member and currency and task ledger source ID, one target transaction. `093f4ca6` rejects foreign QA global ID collisions before inserts.
- Tests `DisposableTaskInstanceSnapshotValidatorTest` `2f2bdb52`/`d5d894aa`/`8f9068e6`; PG18 `7cbcff8c`/`5a36e161`/`2a07b90a`: strict missing/forged snapshot, status/time/assignee/currency, duplicate slot, actual unique PG constraint, 4-stage rollback/retry/race, neighbor collision, reputation-only. **Exact current SHA Maven/PG18 PASS not independently verified.**
- Conditional ACK to **C** RESTORE permit contract `docs/coordination/requests/BACKEND_B_TO_C_RESTORE_PERMIT_CONSUMER_ACK_20261118.md` @`25d2b6cc`: C issues real action RESTORE owner scope only, B locks family then verified permit, consumes and restores/bumps epoch/results within same DB transaction; requires versioned source workspace/manifest and stable digest before final approval.
- Formal QA packet `docs/coordination/requests/BACKEND_B_BB06_HISTORY_CONFLICT_AND_PERMIT_QA_20261118.md` @`00d759ab`, commit annotated [full-ci] for actual non-skipped full four-component check. **Cannot call a commit a CI PASS.** Older QA red #44268695217 known, newer main requires fresh independent evidence.
- No production Restore/Flyway/owner auth/real finance tables touched. Production restorable/replaySupported=false, Reset Execute HTTP423, BETA HOLD. Next B-B-07: typed source/target restore plan and identity digests, C-ACK/A artifact/D finance gated.

## B-B-06 additional producer consumer ACK / CI request — 2026-11-18
- Backend B conditionally ACKed Backend A logical hidden original `FAMILY_MEMBERS` vNext catalog only as source reference graph (not owner/account authorization) in `docs/coordination/requests/BACKEND_B_TO_A_MEMBER_SOURCE_CONSUMER_ACK_20261118.md` @`e4f71887`: mandatory tombstones, original role+family, source member FK closure, full historical TaskInstance fields, separate group/account membership and no raw credentials/emails.
- Backend C scope consumer conditional ACK `25d2b6cc` pending Architecture named schema/IMP; C owns owner reauth/one-use grant, B only verifies and consumes RESTORE grant atomically with target replay. Source identity version and sorted mapping digest required.
- Explicit B-B-06 QA 5-class targeted and actual all-four full-CI request `docs/coordination/requests/BACKEND_B_BB06_PINNED_CI_RUN_20261118.md` @`5e3420de`, commit `[full-ci]`. **Run ID and current exact-SHA pass NOT independently verified by Backend B.**
- Current code remains TEST ONLY (QA FK tables), no production import, no actual authenticated original family members after loss, no real source finance all-categories. HTTP423/BETA HOLD.

## Backend B B-B-07 large package — 2026-11-18 (SOURCE READY / QA NOT VERIFIED)
- New `DisposableRestorePlanFingerprint` `11b5e115`/`ba500cac`/`2b30cba2`: typed sourceWorkspace/targetWorkspace/version/snapshot/manifest digest/source+target epochs/sorted original→target member mappings/selected category counts with canonical SHA-256 domain-separated member, selection and full plan digests. Reject missing member identity catalog, legacy V50, omitted required category parent, invalid/duplicate/unknown categories, ambiguous mapping and target epoch overflow. This is caller-supplied test plan, not signed A evidence.
- Pure JUnit `DisposableRestorePlanFingerprintTest` `7155bc09`/`ca53ebfa`/`37eb3e05`: permutation-stable digest, modified scope/map/category/epochs changes digest, C descriptor RESTORE-only matching, wrong operation, RESET, expiry, missing roster and parent closure negatives.
- New TEST ONLY `DisposableRestorePlanTransactionGuard` `e0a844f1` and PG18 Testcontainers `c8b07703`/`c0743997`: lock target family data_epoch, compare caller-supplied immutable C scope descriptor, reserve unique synthetic operation, write QA event, bump epoch in same transaction. Retry only after injected rollback; duplicate call/stale epoch denied, competing 2 callers at most one commits and unrelated family preserved.
- QA `docs/coordination/requests/BACKEND_B_BB07_CANONICAL_PLAN_AND_PERMIT_QA_20261118.md` @`d435debf`. **No current exact SHA JUnit/PG18 PASS independently observed**. Run six targeted tests plus all-four full CI/Release 98+ on exact SHA, triage real failures.
- **Important C interoperability gap:** C's current immutable descriptor lacks explicit sourceWorkspaceId/schemaVersion/manifestRootDigest fields and is NOT a signed owner grant; B canonical plan hash cannot replace real issuer proof. Source member identity vNext A not yet approved, D finance/other category closure incomplete, B synthetic plan transaction not same as financial rehearsal transaction. No production restoration.
- Next B-B-08: per-row dependency closure + signed original member/finance provenance fixture and C permit atomic consumption after Architecture IMP; Restore HTTP423/BETA HOLD.
