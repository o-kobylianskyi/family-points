# FamilyPoints — Backend B (Restore Engine) log

## 2026-11-18 — Owner assignment / direction handoff
- Project owner explicitly assigned this conversation **Backend B**. Architecture working split: Backend A owns **Backup & Portable Archive**; Backend B owns **Restore Engine**. Current older architecture GitHub coordination still lists a single Backend; seek Architecture formal ACK before calling this an approved shared ADR.
- Created [BACKEND_B_LATEST](../handoffs/BACKEND_B_LATEST.md) to preserve exact responsibilities, TEST-only prior BKL-002 #25–64, cross-track dependencies and safety gates. Do not overwrite legacy `logs/backend.md` history.
- Backend B handles target-side FULL/SELECTIVE restore, FK/ID mappings, tenant isolation, atomic rollback/idempotency and authorization consumption, source-vs-target integrity/recovery receipt. Backend A owns exporter, portable source schema/categories, artifact encryption/digest, retention and original source-member catalog. Request shared contract through Architecture before cross-track edits.
- Current V50 lacks portable `FAMILY_MEMBERS`; no signed original identity or verified owner reauth. Production reset Execute HTTP423, restorable/replaySupported false, BETA HOLD. Current exact-SHA four-component CI green NOT verified; do not claim.
- **Next: B-B-01** source→target restore dependency matrix and Backend A/Architecture schema needs; then independent QA synthetic PG18 target replay/rollback.

## 2026-11-18 — B-B-01 V50 restore dependency matrix
- Source-reviewed `WorkspacePortableBackupExporter` and `ProtectiveBackupImportInspector`, all 23 current portable categories, checked explicit original member/currency polymorphic parent blockers and RESTORE_ENGINE_NOT_IMPLEMENTED. Authenticated original FAMILY_MEMBERS still unavailable.
- Committed `docs/coordination/requests/BACKEND_B_RESTORE_DEPENDENCY_MATRIX_20261118.md` @`99f5f812` with per-category owner, restore FK and selective closure graph, safe two-pass conceptual ordering, target conflict and parent omission policies, exact Backend A/Architecture/QA gates.
- Appended formal request to A/Architecture; **NO** exporter/format change or real DB write. Next: B-B-02 PG18 target replay mapping and rollback tests after reviewing current consumer helpers; shared contract ACK remains pending, production restore HTTP423/BETA HOLD.

## 2026-11-18 — B-B-02 two-DB Restore FK mapping and rollback
- Implemented `DisposableTwoDatabaseRestoreGraphExecutor` test-only `c2921656`, `ee39aef7`, `36966d27`: selected source GROUP + two-level WORK_NODES source/target mapping against two independent PG18 DBs, original roster proof while source live, guarded target epoch/foreign IDs, ordered FK reconstruction in one target transaction.
- Testcontainers `4c9cf1de`, `129e9648`: missing/foreign/duplicate parent, 3 failure-injection stages, rollback/retry, duplicate and concurrency exactly once, unrelated target family survival. No live data touched.
- QA document `5e8f1a39`. Independent targeted PASS current SHA NOT_RUN; previous actual full CI #44265994374 at prior QA SHA 507 Maven PASS and four-component green. Production Restore blocked HTTP423.
- ABCD role split consulted: C owner security grant, B target consumer transaction, D finance integrity, A portable source. New helper is deliberately not a C auth implementation.

## 2026-11-18 — B-B-03 task/reward financial restore mapping
- Implemented TEST ONLY `DisposableRestoreFinancialDependencyPreflight` `cbc520e9`, `4b733d78`; maps authenticated source JSON TASK/REWARD/POINT_TYPES/PURCHASE/LEDGER graph to explicit target IDs, fails closed missing parents, wrong member/currency, charged duplicate, unknown REFUND/exchange, task signed event duplicate. Does not write target DB or mint owner grants.
- Unit negative fixtures `e87240d0`, `93af5b44`; QA packet `a22bd623`. New exact-SHA Maven NOT_RUN/UNKNOWN. Full CI #44265994374 previously 507 PASS but predates this.
- Preserve ABCD: A source/crypto, B restore consumer, C real owner authority, D finance semantics. Next B-B-04 synthetic PG18 replay only, production HTTP423 HOLD.

## 2026-11-18 — B-B-04 isolated PG18 target financial graph
- New B-B-04 test-only mapped QA target FK writer `DisposableFinancialTargetRehearsal` @92b56372/31382da5, never writes real finance/history tables. Reconciles B-B-03 expected target balances against inserted mapped ledger sums, checks tenant and epoch, rejects duplicate apply.
- New Testcontainers PG18 @349a2f3d/dbd59033: 4 QA-only FK tables with real family/member/currency parents, +3 test balance, late 3-stage rollback/retry, foreign IDs and mismatched source spend, race, neighbor isolation.
- QA handoff `021467b6`, exact new SHA Maven NOT VERIFIED. Earlier known all-four #44266982944 514 PASS predates this B-B-04. TASK source_id original ID still **not native target TaskInstance remap**. ABCD owner boundaries unchanged; production HTTP423/BETA HOLD.
