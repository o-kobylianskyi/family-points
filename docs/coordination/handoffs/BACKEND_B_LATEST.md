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
