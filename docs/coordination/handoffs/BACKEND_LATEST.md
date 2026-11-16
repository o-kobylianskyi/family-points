# FamilyPoints Backend — rolling continuation handoff

**Last checkpoint:** 2026-11-16, evening CET. **Track:** Backend. **Repository:** o-kobylianskyi/family-points, main. **Purpose:** independently self-contained pickup after chat hard-limit or another agent handoff.

## Release/security policy
- BKL-002 selective/FULL reset/restoration is P0 but **BETA HOLD**. Never enable `reset/execute` (HTTP 423), promote synthetic `VERIFIED`, or mutate real DEV/BETA/PROD family data without architecture, QA, release and owner approval. Always test synthetic disposable PostgreSQL 18.
- Backend works independently under architect's async QA protocol `docs/coordination/requests/BKL002_BACKEND_QA_ASYNC_TEST_PIPELINE_20261116.md`; code/fast tests, QA packets on #15, continue bounded source work. Do not ask user to run every micro-batch.
- No reliable ChatGPT context percentage; use `docs/coordination/chat-capacity/BACKEND.md` and always update handoff. No invented 50/75/85% measurements.

## Latest authoritative independent CI/QA
- Pinned QA accepted 27/27 PASS, zero failures/errors/skips at commit `419efb1fdcab849480e790449c791c86ce004695` for bounded QA packets #01-03 ONLY; newer #04-11 not independently accepted at latest SHA.
- **Important P0 CI regression:** GitHub Actions run `43947907960`, commit `19adfe1b7`, 460 Maven tests / 1 failure / 1 error / 0 skipped. Android/Web/Compose steps PASS, Backend FAIL and exact-SHA full gate FAIL. Architecture issue #15 comment 6928274072.
  1. `WorkspaceRewardObligationArchiveClosurePostgresTest:189` expected one exported reward category but fixture already includes two (`Blocked media` and `Media rewards`). Changed fixture to expect 2 and query independently confirmed source family row count 2: commit `839e9f8`.
  2. `DisposableRewardCategoryCrossDatabasePostgresTest:105` NPE from Java immutable Map.of `containsValue(null)` in `DisposableRewardCategoryRestoreExecutor`. Replaced by safe stream null check; ALSO fixed expected alphabetically sorted inspector category order (`REWARD_CATEGORIES,REWARD_DEFINITION_CATEGORIES,REWARD_DEFINITIONS`) in `e1e4e2b`. Added explicit null-map denial/no-target-mutating regression `9ccfe2e`.
- **These fixes have NOT yet been independently rerun on exact current SHA.** QA retest packet `docs/coordination/requests/BKL002_QA_PACKET_20261116_CI_RED_REPAIR.md` at `d5b1e85`, issue #15 comment `6928389335`, and Backend detailed journal commit `cb9b687` now record the corrections. QA must run two targeted classes, surrounding archive/FK/financial closure tests and full Java four-component CI where applicable. No false green.

## Implemented P0 backup/export changes
- V50..V52 portable encrypted archive, registry+lease+staging+retention. Scoped categories include media/bindings, task/reward definitions, requests, purchases, task execution, point/reputation/group ledger, groups, private templates, reward requirements, reward categories, reward-category composite associations, reward obligations and composite obligation blocked categories/rewards.
- Source exporter `src/main/java/com/olehkobylianskyi/familypoints/service/WorkspacePortableBackupExporter.java` and `ProtectiveBackupImportInspector.java` guard tenant family, parent FK graph, missing categories, authenticated AES, content digest, source historical polymorphic `source_id`, manual ledger history, explicit financial currency/member rebind. All production restored state remains unsafe/blocked.
- `WorkspaceFinancialRestoreClosureService` read-only inventories twelve economic categories, with always `replaySupported=false`; no `VERIFIED` promotion. Previous source/test commits chronologically in `docs/coordination/logs/backend.md`.
- Tests `DisposableCrossDatabasePortableRestorePostgresTest`, `DisposableGroupLedgerCrossDatabasePostgresTest`, `DisposableRewardCategoryCrossDatabasePostgresTest`; specialized executors are in `src/test` ONLY. Actual production full restore not implemented.

## Latest completed source-only Backend package #13 (2026-11-16)
- Test-only native REWARD_REQUIREMENTS replay: `DisposableRewardRequirementRestoreExecutor.java` commit `e825afe0`, and two independent PostgreSQL 18 cross-DB test `DisposableRewardRequirementCrossDatabasePostgresTest.java` commit `4088b5e`. Authenticated V50 archive, explicit complete/injective source→target definition, request, task mappings, source parent FK checks, target tenant/epoch/empty history, transactional remap and source/neighbor invariance. NULL/foreign mappings, duplicate replay and tampered file denied; **NOT_RUN** at source-only connector, no production endpoint.
- QA packet #13: `docs/coordination/requests/BKL002_QA_PACKET_20261116_REQUIREMENT_NATIVE_REPLAY.md` commit `15c0c62`; issue #15 comment `6928967238`. Prior CI repair QA #12 also awaiting latest-SHA retest; never infer PASS from previous 27/27.
- **Next bounded P0:** test-only native REWARD_OBLIGATIONS plus blocked category/reward associations; explicit member, purchase, requirement, reward definition, category ID maps, target FK/epoch/rollback/no neighbor writes. Production VERIFIED, financial ledger closure, owner reauth and Reset Execute HTTP423 remain blocked.
- Backend journal update: `491a1aaf`.

## Latest Backend P0 package #14 — 2026-11-16 night
- Added test-only `DisposableRewardObligationRestoreExecutor.java` commits `e4110bb`, `c4ca790` for authenticated reward obligations and blocked-category/reward graph. Explicit complete injective maps for member, purchase, optional requirement, category, reward; source and target family and parent FK; fresh target epoch; purchase owner and historical title/amount/status; atomic insert and counts. `src/test` ONLY, no API.
- Added 2 independent PostgreSQL 18/Flyway cross-DB test `DisposableRewardObligationCrossDatabasePostgresTest.java` commits `dee123c`, `d4f1b21`: one obligation, each blocked target; wrong-family and wrong purchase price rejection, authenticated crafted second SQL-invalid row causes **actual rollback**, duplicate/tamper rejection, source and neighbor unchanged.
- QA #14 file `docs/coordination/requests/BKL002_QA_PACKET_20261116_OBLIGATION_NATIVE_REPLAY.md` commit `4a3fbe7`; issue #15 comment 6930349243, eight-class target command. Developer Maven/Docker NOT_RUN, do not report new PASS until independent exact-SHA QA.
- Previous CI failure #12 had source fixes, no newest exact-SHA green proof in latest issue comments. #13 requirements native replay also pending independent QA. Older QA 27/27 @419efb1 limited to #01–03.
- Next P0: inspect new QA results/CI and correct any actual regressions; further work must prove currency and immutable individual/group ledger source graph, member binding and fully reconstructable financial history before real Restore. Production `VERIFIED` false, `reset/execute` HTTP423, BETA HOLD. Track journal `docs/coordination/logs/backend.md` commit `2b976c7`.

## Latest Backend P0 package #15 — 2026-11-16 night
- Actual QA/CI failure latest: Actions `43953986968` @4088b5e5 Java 461 tests, one failure; `DisposableRewardCategoryCrossDatabasePostgresTest` preflight `CATEGORY_REPLAY_SCOPE_UNSUPPORTED`. Source fix `2539bf9`: Java lexical ordering is `REWARD_CATEGORIES,REWARD_DEFINITIONS,REWARD_DEFINITION_CATEGORIES` (S < underscore), not previous inverted sort. QA current-SHA retest required.
- Test-only `DisposablePointLedgerRestoreExecutor.java` commit `1d9fb14`: AES V50 POINT_LEDGER scoped to MANUAL/no source_id, complete explicit member+currency map, separate externally supplied expected currency codes, target tenant/epoch/no prior ledger, transactional replay and **per member+currency balance** reconciliation. Not production-ready: currency identity is NOT in authenticated archive; nonmanual sources cannot replay.
- `DisposablePointLedgerCrossDatabasePostgresTest.java` `39b699c`: physically independent PG18 source/target, +12/-2=10 exact manual balance, neighbor 99 unchanged, wrong member/wrong currency code/duplicate/TASK-source_id all rejected. Tests NOT_RUN by Backend.
- QA packet #15 `docs/coordination/requests/BKL002_QA_PACKET_20261116_MANUAL_POINT_LEDGER_REPLAY.md` commit `a07ce64`, Issue #15 comment 6930586327; QA requested pinned 10-class run covering prior red and obligations.
- Backend journal `docs/coordination/logs/backend.md` commit `a3ff5e6`. Next implement cryptographically authenticated currency catalog, source-type graph closure and independent current-SHA CI acceptance before moving financial restoration closer to production. FULL/SELECTIVE reset HTTP423 and BETA HOLD unchanged.

## Latest Backend P0 package #16 — 2026-11-16 late
- Source encrypted V50 `POINT_TYPES` category, directly scoped family_id, full point_types rows plus SHA/epoch protected digest: exporter `e6f4bde`, snapshot whitelist `ceef29c`, backup registry `95d8b69`.
- Offline inspector `15c8717`: archived point type direct tenant, nonblank unique currency code, denomination FK checks for point/group ledgers and reward definitions/purchases/requests; missing or dangling POINT_TYPES never silently restorable. New AES fixture `0657099` tests complete/missing/dangling/duplicate code/cross-family.
- Test-only `DisposablePointLedgerRestoreExecutor` `09024dc` now requires encrypted `POINT_LEDGER + POINT_TYPES`, source family+code from sealed metadata; caller mapping expected code AND target point_type active code must match signed source code. Still MANUAL only, no polymorphic source_id, per member×currency sum reconciled. Two-PG test changes `a567b4b`, `9d946ac` cover valid 10 balance, legacy currency-less archive reject, tampered file reject, source/neighbor no writes.
- QA packet #16 `docs/coordination/requests/BKL002_QA_PACKET_20261116_POINT_TYPES_CURRENCY_CLOSURE.md` commit `b662d04`, Issue #15 comments 6930878958 and addendum 6930897477. **New Java tests NOT_RUN** by Backend; exact SHA independent QA needed.
- Backend log `docs/coordination/logs/backend.md` commit `83f15a1`. Last known Java-red issue (run 43953986968, category scope) corrected in `2539bf9` but no verified latest-SHA QA green yet.
- Next P0: check QA/CI evidence first; expand cryptographically anchored source-code financial metadata into non-MANUAL polymorphic source graph only with explicit source→target ledger/object remaps and reconciliation; no production VERIFIED, owner authorization, reset execute (HTTP423), BETA HOLD.

## Latest Backend P0 package #17 — signed financial source graph
- TEST ONLY `src/test/java/com/olehkobylianskyi/familypoints/DisposableLedgerSourceGraphVerifier.java` `5928292`: V50 AES archive read-only source ancestry for POINT_LEDGER/POINT_TYPES; supported MANUAL null-source_id and TASK positive source_id attached to archived TASK_EXECUTIONS of same member; unsupported purchase/refund/exchange source types fail closed. **This does not replay TASK ledger to any target**.
- AES regression fixtures `DisposableLedgerSourceGraphVerifierTest.java` `b6cf1c3` validate valid MANUAL/TASK and orphan/foreign-member/missing TASK, MANUAL invalid source_id and unsupported reward/refund. Developer Maven/Docker NOT_RUN.
- QA packet `docs/coordination/requests/BKL002_QA_PACKET_20261116_LEDGER_SOURCE_GRAPH.md` `947c19c`, issue #15 comment 6931300460; queue seven-class exact-SHA regression with newest currency/replay and earlier red Native Category scenario. Backend detailed log `05d710e`.
- Remaining blocking concerns: TASK source target-ID proof / financial type semantics, purchase/refund/exchange source producers, member and currency protected binding, ledger immutable reconciliation, actual VERIFIED backup and owner reauth; latest SHA CI not independently proven green. **Reset Execute HTTP423 / BETA HOLD.**

## Latest Backend P0 package #18 — purchase ledger source proof
- Read production enum `PointTransactionSourceType`, `RewardService` direct/request purchase handlers and `PointService.spend`: `REWARD_PURCHASE` writes source_id=purchase.getId(), negative amount and SPEND transaction. Other enum values (REFUND/POINT_EXCHANGE/...) not presumed remappable.
- Test-only read-only `DisposableLedgerSourceGraphVerifier` `c99371a` now demands REWARD_PURCHASE ledger ↔ authenticated archived purchase ID/owner/point_type/price/sign/transaction type. TASK proof now needs task instance's TASK_DEFINITIONS family ancestry and bounded amount `79731d7`. Never replay financial rows from these proofs.
- AES fixture `DisposableLedgerSourceGraphVerifierTest` `68bcae0`/Java syntax correction `b609809`/purchase positive+six negatives `87de74a`.
- QA packet `docs/coordination/requests/BKL002_QA_PACKET_20261116_LEDGER_PURCHASE_ANCESTRY.md` `c96f499`, Issue #15 comment 6931656040; seven-class exact-SHA run requested; developer Maven/Docker NOT_RUN. Backend journal `852db09`.
- **Next P0:** check newest exact-SHA QA/CI source failures, inspect Refund/Point Exchange actual producer contracts before source ancestry extension. Then make isolated source→target purchase ID remap verifier and immutable balance reconciliation without production writes. No VERIFIED/Restore reset execution; HTTP423, BETA HOLD.

## Latest Backend P0 package #19 — 2026-11-16 late
- Actual production source: `PointExchangeService.executeExchange` uses PointService.spend and earn for same `point_exchange_requests.id`, source_type POINT_EXCHANGE, precise -fromAmount/SPEND from point currency and +toAmount/EARN to point currency; `PointService.refund(...,sourceType,sourceId,...)` receives caller-specified identity, so generic REFUND mapping remains unsupported.
- Test-only read-only `DisposableExchangeLedgerSourceAuditor` commit `b603b1e`: audit APPROVED exchange request ownership/rate/from-to currencies and exactly 2 ledger legs with one debit and one credit. Disposable Flyway PG18 test `17e6b15` covers valid and other-family/wrong-credit/missing-leg/wrong-debit negatives. No production writes by auditor and no portable exchange archive or target replay.
- Hardened authenticated `REWARD_PURCHASE` source verifier against duplicate debits to same purchase ID `f5e8ace` with AES fixture `0696744`. No refunds or exchange source-type newly authorized in portable verifier.
- QA packet `docs/coordination/requests/BKL002_QA_PACKET_20261116_FINANCIAL_SOURCE_EXCHANGE_AUDIT.md` `7f8c33b`; issue #15 comment 6931778184; seven-class pinned Maven command. **Tests NOT_RUN** by GitHub source connector, QA acceptance pending.
- Backend journal `docs/coordination/logs/backend.md` commit `c88a9a1`. Next P0: QA/CI actual defect triage, add tenant-safe historical point exchange request/rate source catalog to authenticated archive, preserving rate semantics, then explicit remap; do not enable VERIFIED or reset/execute. BETA HOLD HTTP423.

## Latest Backend P0 package #20 — portable authenticated exchange source graph
- `POINT_EXCHANGE_RATES` and `POINT_EXCHANGE_REQUESTS` added to family-scoped source exporter `efaa597`, digest `59eac35`, backup metadata registry `8d1e21b`. If source POINT_LEDGER contains POINT_EXCHANGE, exporter requires both plus POINT_TYPES; request/rate/currency/reviewer foreign family denied.
- `ProtectiveBackupImportInspector` `b99ca55` and `f15bb4e`: two new category FK/tenant/owner validators; immutable per-request rate snapshot multiplier checked independently of mutable current rate; signed ledger exactly one SPEND(-fromAmount) and EARN(+toAmount) referencing same original request ID, approved status and same member. `restorable=false` always; no native target replay.
- Economic closure `1988200` now reports 14 categories including both exchange scopes; test counts updated `f049607`, `86206fa`. PG18 AES export `9fda117` and Jackson ObjectNode structural cases `b6fa9d9`, snapshot mismatch negatives `f7d1db3` all authored **NOT_RUN**.
- Independent QA packet `docs/coordination/requests/BKL002_QA_PACKET_20261116_PORTABLE_EXCHANGE_GRAPH.md` `09984d0`, issue #15 comment 6932004395; ten-class pinned Java/PG18 suite; no current SHA green proof. Full financial replay, REFUND source semantics, protected VERIFIED, owner reauth, reset Execute **HTTP423 / BETA HOLD**.
- Backend detailed journal `4545c10`. Next P0: read QA #20 failure logs first, fix actual regressions; then test-only source→target exchange member/currency/rate and 2-leg ledger remapping with conservation proof; no production financial mutation.

## Next action priority
1. **First check #15 and current Actions jobs** for new results of patched regressions, ensure two previously red tests PASS. If FAIL, fix; do not weaken isolation or source closure assertions.
2. **Next bounded code package:** test-only independent PostgreSQL native replay of `REWARD_REQUIREMENTS` then `REWARD_OBLIGATIONS` with explicit source→target ID maps for purchase, request, definition, task, member, currency, categories; reject unsupported historical dependency/source IDs. Include atomic rollback and cross-tenant proof; no production API.
3. Package new source+tests+SHA under `docs/coordination/requests/BKL002_QA_PACKET_...`, notify Issue #15, update `docs/coordination/logs/backend.md`, this handoff and capacity log.
4. Release must collect exact-SHA four-component non-skipped CI. QA packets #04-11 can be consolidated; user should run only operator-assisted real tests through QA.
5. Review P0 flow for source graph proof, owner fresh reauth, encrypted KMS/file archive and durable VERIFIED, protected target snapshot, 30-day retention and atomic Reset/Restore. Maintain HTTP423/BETA HOLD.

## Coordination contacts
- Issue #15: https://github.com/o-kobylianskyi/family-points/issues/15
- Architecture: docs/coordination/DISPATCH_20261116_P0_BETA_RECOVERY.md and docs/architecture/ADR-FAMILY-RESET-002.md
- QA: docs/coordination/QA_BKL002_ASYNC_CONTROL_20261116.md
- Temporary chat monitoring: docs/coordination/requests/CHAT_CAPACITY_ALL_TRACKS_20261116.md

**Track state:** backend source changes actively merged to main, last targeted fixes pending current-SHA CI proof. Safe implementation continues; no destructive endpoints enabled.
