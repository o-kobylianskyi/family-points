# Backend BKL-002 QA #28 — TASK source signed-history provenance

Date: 2026-11-17. **Read-only TEST-only** archived financial source graph verifier. No real family data touched.

## Grounded producer contract
Actual production `TaskService` uses `PointService.earn(..., PointTransactionSourceType.TASK, instanceId)` for positive task reward; missed mandatory task uses `PointService.penalty(..., PointTransactionSourceType.TASK, instanceId)` for negative penalty. `PointService.refund` accepts caller-specified sourceType/sourceId, so generic REFUND replay source ancestry remains UNSUPPORTED. REWARD_PURCHASE existing graph requires archived purchase member/point-type/price and a negative SPEND; POINT_EXCHANGE requires its own two-leg verifier.

## Changes
- `fb7abcef`: TASK rows now demand signed amount/type pairs: positive EARN or negative PENALTY only. Reject 0, SPEND, reversed sign, foreign-member and missing task source.
- `7594bb5a`: authenticated V50 JSON fixtures positive award + missed mandatory penalty; negative sign/type/zero.
- `e8fe486b`: reject duplicate TASK events for the same task instance and event type, not just duplicate ledger ID.
- `abb599db`: fixture rejects repeat EARN and PENALTY for one source; distinct reward and penalty events remain representable.
- Existing source graph MANUAL, REWARD_PURCHASE verification remains fail-closed. REFUND, POINT_EXCHANGE and legacy unknowns are not newly allowed by this verifier.

## Exact-SHA QA execution
```powershell
mvn -Dtest=DisposableLedgerSourceGraphVerifierTest test
mvn -Dtest=DisposableLedgerSourceGraphVerifierTest,DisposableMultiExchangeNativeRestorePostgresTest test
```
Check with Docker / PostgreSQL 18; report exact SHA, test counts/failure/error/skip, first error stack. Run larger historical 12-class #24 suite and four-component CI after targeted success. New source commits are **NOT independently PASS** based on the older full-green 479 Maven tests at `8c397d0`.

## Release safety
No production bean/controller, no live restore. Never mark `VERIFIED` / `replaySupported` true. Reset Execute HTTP423 and BETA HOLD until full owner-approved release gates.
