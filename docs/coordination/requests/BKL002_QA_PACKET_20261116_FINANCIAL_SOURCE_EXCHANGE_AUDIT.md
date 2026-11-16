# BKL002-QA-20261116-19 — Refund/Exchange producer audit and two-leg POINT_EXCHANGE financial source proof

**State:** READY_FOR_QA, developer Maven/Docker NOT_RUN. **Owner:** Backend, **independent QA:** Issue #15. Synthetic Testcontainers PostgreSQL 18 only; no production data or Reset/Restore endpoint.

## Actual producer contracts read in production code
- `PointTransactionSourceType` enum supports MANUAL, TASK, TASK_MILESTONE, TASK_PENALTY, LEARNING, ACHIEVEMENT, ACCESS_PURCHASE, REWARD_PURCHASE, POINT_EXCHANGE, REFUND, PENALTY_REVERSAL, SYSTEM. This is **not** evidence that each enum case has restorable finance semantics.
- `PointService.refund(workspaceId,memberId,pointTypeId,amount,sourceType,sourceId,description)` writes a positive transaction with `type=REFUND` but **accepts sourceType/sourceId from its caller**; not enough to declare a universal `REFUND.source_id` FK. REFUND remains blocked in portable Native Restore.
- `PointExchangeService.executeExchange` calls `PointService.spend(...fromPointType, fromAmount, POINT_EXCHANGE, request.getId())` and `PointService.earn(...toPointType, toAmount, POINT_EXCHANGE, request.getId())`. Thus actual source_id points at `point_exchange_requests.id`, with **one negative SPEND from currency** and **one positive EARN to currency** in a successful exchange; the request has member, rate, amounts, status, and currency FKs.

## New bounded code
- `DisposableLedgerSourceGraphVerifier` `f5e8ace`: reject duplicated authenticated `REWARD_PURCHASE` charges pointing at same purchase ID, in addition to existing owner/currency/price/sign/type source checks. `DisposableLedgerSourceGraphVerifierTest` `0696744`: two SPEND rows for one original purchase fail closed.
- `DisposableExchangeLedgerSourceAuditor` `b603b1e` TEST ONLY / **READ ONLY**: source PostgreSQL query for given exchange request, owning family and point_type and rate family closure, status APPROVED, distinct currencies, positive from/to amounts. Exactly **2** point_transactions with source_type POINT_EXCHANGE + source_id=request ID: matching member and two legs, from type `SPEND amount=-from_amount`, to type `EARN amount=to_amount`; duplicate/missing/wrong leg or foreign tenant fails. Returns bounded source audit Pair only; no AES archive/currency remapping or target restore.
- `DisposableExchangeLedgerSourceAuditorPostgresTest` `17e6b15`: synthetic PostgreSQL Flyway migration, 2 currencies, rate, approved request, one + one matching history, positive 10→2 audit and negative other family, wrong credit amount, missing leg and mismatched debit. No production family, no writes performed by auditor (fixture intentionally writes).
- No exchange request/rate category has been added to encrypted portable archive. Therefore **POINT_EXCHANGE still fails closed in portable ledger verifier** despite valid source-DB audit. Source graph verification is not sufficient to authorize target replay or VERIFIED.

## Independent QA (Java21 + Docker)
```powershell
git pull --ff-only origin main
git rev-parse HEAD
.\mvnw.cmd "-Dtest=DisposableExchangeLedgerSourceAuditorPostgresTest,DisposableLedgerSourceGraphVerifierTest,DisposablePointLedgerCrossDatabasePostgresTest,DisposableRewardCategoryCrossDatabasePostgresTest,DisposableRewardRequirementCrossDatabasePostgresTest,DisposableRewardObligationCrossDatabasePostgresTest,ProtectiveBackupRewardHistoryClosureTest" test
git rev-parse HEAD
```

## Required QA cases
1. FIN-19-01 actual source exchange approved, 2 row proof, exact member/from/to amount and currency, original request ID; other family excluded.
2. FIN-19-02 incomplete exchange missing leg / mispriced debit or credit / another owner or extra charge must be rejected (report exact test coverage and open missing cases).
3. FIN-19-03 a duplicate REWARD_PURCHASE debit for one original purchase fails authenticated archive source preflight; source owner/currency/price invariant retained.
4. FIN-19-04 REFUND remains unsupported because its source reference is caller-dependent; no fabricated REFUND semantics.
5. FIN-19-05 exact source SHA, Maven PASS/FAIL/ERROR/SKIP from real run, earlier scoped category/reward/ledger regressions; no all-four CI claim without independently unskipped jobs.
6. FIN-19-06 `ProtectiveBackupImportInspector.restorable=false`, `WorkspaceFinancialRestoreClosureService.replaySupported=false`; Reset Execute HTTP423 and BETA HOLD.

## Next P0
- Verify and export complete exchange-request/rate source graph with tenant integrity before offering portable source ancestry; source-side REQUEST status/rate history policy, target currencies, and FULL ledger reconciliation remain open.
- Establish actual REFUND callers/semantics separately before mapping `source_id`; no generic replay of REFUND, EXCHANGE, TASK or purchase financial rows in production.
