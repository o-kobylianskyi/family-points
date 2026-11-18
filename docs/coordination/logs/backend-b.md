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

## 2026-11-18 — B-B-05 TASK ledger execution ID remap
- Source-checked production TaskInstance and repaired B-B-04 test-only task provenance hole: target QA execution inserted with FK to task + owner and new execution ID, TASK ledger now references that ID rather than archived numeric source. Commits `0cbe423a`, `101b5a94`.
- PG18 `5b59ce64` + `4e3f1af3` physical FK test, after-execution failure rollback, absent original execution/definition fail closed, ledger remap and family isolation. Source→target mapping proof does not rely on numeric inequality. QA `261abb8d`; current exact SHA Maven unverified.
- No production executor/REST/schema mutation; C actual auth, A archive family lineage, D finance closure still needed. Remains HTTP423/BETA HOLD.

## 2026-11-18 — B-B-06 large batch historical TaskInstance + red CI ownership
- QA issue #15 identified Backend B fixture global PK failures (572 tests, 1 FAIL + 1 ERROR) #44268695217. Added collision preflight `093f4ca6` and PG18 neighbor collision negative `2a07b90a`; per-family ID allocation already present; require exact new-SHA CI, no guessed green.
- New source historic task validator `fd5dc71c`/`c35c8a5b`, target PG QA execution fields/unique slot `08fcfd83`, `b93222a1`, fixtures `7cbcff8c`, `5a36e161`, pure tests `2f2bdb52`, `d5d894aa`, `8f9068e6`: original-assignee/status/date/slot/claimed/start/finish/excuse/reward/penalty POINT+reputation and real TASK ledger amount/currency parity, reject missing metadata, duplicates, support reputation-only without invented point ledger.
- Conditional B consumer ACK to C action-scoped owner permit `25d2b6cc`, request source workspace identity/versioned manifest/canonical mapping digest, C real owner proof+grant. QA formal packet `00d759ab`.
- Everything TEST ONLY; no real family/history or Reset Execute touched. Current exact-SHA Maven unknown; HTTP423/BETA HOLD.

## 2026-11-18 — B-B-06 producer consumer ACK and explicit full-ci
- Conditional A consumer logical approval `e4f71887` based on A's actual B-A-02 member catalog proposal and Architecture review 02, with tombstone/private original family identity and full TaskInstance history source fields; not wire-format or migration approval.
- C action RESTORE grant consumption contract conditional ACK `25d2b6cc` remains. QA pinned JDK21/PG18 + full four-component request `5e3420de` committed with workflow `[full-ci]` directive; cannot claim green until job/steps, counts and immutable SHA independently inspected.

## 2026-11-18 — B-B-07 canonical Restore Plan + synthetic PG consumption
- Added typed canonical SHA-256 separated digests `11b5e115`/`ba500cac`/`2b30cba2`, JUnit negatives `7155bc09`/`ca53ebfa`/`37eb3e05`; does not trust order of input maps/categories and rejects legacy without original signed `FAMILY_MEMBERS` selection.
- Added B plan-vs-C immutable RESTORE scope comparison and one-target-PG18 QA operation/epoch transaction `e0a844f1`, `c8b07703`, `c0743997` with rollback, race and wrong action/expiry negatives. This is **not authenticated C grant issuance or consumption**; no real finance writes.
- Formal QA packet `d435debf`, new exact-SHA CI not confirmed. A source signed member catalog, C owner proof/permit durable store and D finance attestations still independent blockers. Runtime Reset/Restore HTTP423/BETA HOLD unchanged.

## 2026-11-18 — B-B-08 signed source graph consumer / C-C-10 binary hash interop
- Implemented bounded original FAMILY_MEMBERS/Task/Reward/Purchase/POINT_LEDGER source graph closure @ed12e564, negative test @ce4883ca; separate C-C-09 binary V2 exact scope adapter @caa9768d and tests @879b98b4/@590040d2; C schema VNext-2 marker test-only @120f5a6e/@3a082c12. Old pipe-delimited B hashes explicitly not accepted as C permit. Combined pre-write bounded closure @67429d44 and negative gate @085192ef; marker fixture @c922d9e6.
- Conditional V2 ACK / hard blockers @041bb43c; QA packet @9139c382. No authenticated A category digest or actual C grant/owner, no production Restore. New exact SHA QA NOT_RUN; HTTP423/BETA HOLD.

## 2026-11-18 — consolidated Backend owner assignment replaces A/B/C/D parallel implementation
- Accepted product-owner instruction GitHub Issue #15 #6974473668: B sole primary Backend; C/D closed as separate workers and all remaining original-owner/reauth/one-use permit and FIN-01..16 history/finance duties are now B; A optional bounded support. Architecture scope freeze supersedes prior B-B-09 speculative continuation.
- Read C original owner enrollment/V2 and permit QA and D signed V50+PG18 finance parity/source graph and native-family gate tests; verified absent native production owner writer/permit, A signed portable original roster, full history replay and one-transaction receipt.
- Committed one critical-path ownership handoff `docs/coordination/handoffs/BACKEND_B_INTEGRATION_CRITICAL_PATH_20261118.md` @`25bab346`. No test-only class proliferation or production unsafe implementation in this coordination packet.
- QA-058 confirmed full actual #44275375375 @ab77611b Maven 702/702 PASS including Web Android Compose and exact attestation; newer HEAD must be retested. Next: pin newest SHA QA gate then single source-loss original-owner restore vertical slice.
