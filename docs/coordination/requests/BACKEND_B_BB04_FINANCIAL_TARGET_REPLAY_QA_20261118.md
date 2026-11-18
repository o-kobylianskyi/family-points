# Backend B B-B-04 — target financial FK rehearsal and reconciliation

**2026-11-18. TEST-ONLY.** No production restore, Flyway migration, external approval or real family data change.

## Implemented
- `DisposableFinancialTargetRehearsal.java` @`92b56372`, hardening @`31382da5`: consumes source JSON already authenticated by A and explicitly mapped source ID graph checked by `DisposableRestoreFinancialDependencyPreflight`. Inside ONE target PostgreSQL transaction, lock target family `data_epoch FOR UPDATE`, reject any existing QA restore rows, verify every mapped target member belongs to target family and each mapped point currency exists, is active and matches signed source code; insert mapped TASK_DEFINITIONS / REWARD_DEFINITIONS / REWARD_PURCHASES / POINT_LEDGER into **QA-only tables**. Verify mapped per-member/currency signed balance equals consumer preflight aggregate before commit.
- Separate failure injection immediately after definitions, purchases and ledger rows; each must roll back **all** QA restore tables. Any subsequent duplicate replay is rejected. Purchase source_id mapped to exact target purchase ID.
- `DisposableFinancialTargetRehearsalPostgresTest.java` @`349a2f3d`, per-fixture ID correction `dbd59033`: PG18 Testcontainers + real Flyway for families/members/currencies and **four QA-only tables** with actual Postgres FKs (families, family_members, point_types, qa_restore_rewards). TASK +5 / reward purchase −4 / MANUAL +2 => target +3, mapped purchase.source_id verified, neighbor target family untouched; injected 3-stage failure, fresh object retry, replay denies; source price tampering, foreign target member, foreign/missing currency and stale epoch never write; concurrent same target only one succeeds.

## Precise explicit limitations
- NO real production `task_definitions`, `task_instances`, `reward_definitions`, `reward_purchases` or `point_transactions` are written. These are *QA-only* FK emulations, not proof of native production schema upgrade or full 23-category restore.
- TASK `POINT_LEDGER.source_id` in this bounded rehearsal still holds the original synthetic TASK_EXECUTION ID; **it is not target TaskInstance ID remapping**. Full execution mapping and its schema are a P0 future gate; must not claim a fully native TASK ledger ancestry.
- Source member identity source DB lost cannot be proven using V50 (A contract pending); C owns owner reauthentication / signed grant and B only consumes; D owns REFUND/POINT_EXCHANGE/reputation/group economics. This helper rejects unsupported request-owned purchases/REFUND/EXCHANGE. No owner authorization is inferred.
- Actual approval one-use consumption is not integrated with this new test transaction. Production Reset Execute HTTP423, restorable=false, replaySupported=false, BETA HOLD.

## QA independent exact-SHA instructions
```powershell
mvn -Dtest=DisposableFinancialTargetRehearsalPostgresTest,DisposableRestoreFinancialDependencyPreflightTest,DisposableTwoDatabaseRestoreGraphPostgresTest,DisposableMultiExchangeNativeRestorePostgresTest test
```
Then pinned [full-ci] all four required non-skipped jobs, actual SHA, run IDs, Maven TESTS/FAILURES/ERRORS/SKIPS, first failure stack and Testcontainers PG18 state. Prior independent full green [#44266982944](https://github.com/o-kobylianskyi/family-points/actions/runs/44266982944) at 982ad94 Backend 514/0/0/0 predates this source. **B-B-04 Maven PASS not independently verified**.

## Next Backend B B-B-05
Close native TASK_EXECUTION and REWARD_PURCHASE target-side ID mapping against real columns and source signed inventory in test-only PostgreSQL, then integrate D economic attestations and C grant consumption once architecture and A producer contracts are accepted; no dangerous production enable.
