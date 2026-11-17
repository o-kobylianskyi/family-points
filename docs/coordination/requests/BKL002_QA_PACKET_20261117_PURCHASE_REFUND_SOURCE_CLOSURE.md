# BKL-002 Backend QA #29–30 — purchase archive ancestry and REFUND fail-closed boundary

**Date:** 2026-11-17 CET. **Scope:** src/test-only cryptographically authenticated archive preflight. No data reset or production restore is enabled.

## Production contracts inspected
- `RewardService` passes `PointTransactionSourceType.REWARD_PURCHASE` and purchase ID to `PointService.spend`; source archive verifier already demands one negative `SPEND` exactly matching archived `REWARD_PURCHASES.member_id / point_type_id / price_amount`.
- `TaskService` uses positive `TASK/EARN` and negative `TASK/PENALTY` for completion/mandatory-miss events (#28).
- `PointExchangeService` writes `POINT_EXCHANGE` two-leg `SPEND/EARN`; only its specialized test-only auditor/replay can verify this pair.
- `PointService.refund` receives sourceType/sourceId from its caller. There is no proven universal refund ancestry contract, so `REFUND` is NOT newly authorized in the general ledger-source replay verifier.

## Code changes
- `875720e961a154ec1b7a4d3be36b1dd2d5ee2ca1`: iterate ALL authenticated `REWARD_PURCHASES` archived parent rows, **including rows without a debit**. Require positive owner member, denomination present in authenticated same-family `POINT_TYPES`, positive bounded price. Corrupt dangling parent must fail before accepting source-graph proof.
- `4265dc2a98a37d0c78cd8c859ab826e325ffeec6`: AES V50 negatives for owner=0, price=0, price > int and second unused purchase with foreign/missing currency ID 999. Existing matching purchase SPEND positive proof retained.
- `0afe1c5e0d5b4fd8b6f6bd4f1882787a319a8f50`: dedicated negative AES cases for `REFUND`, `POINT_EXCHANGE` SPEND/EARN and unknown legacy source_type under generic source verifier. They must remain denied rather than guessed into purchase or task ancestry.

## Acceptance plan, independent QA
On an immutable exact SHA:
```powershell
mvn -Dtest=DisposableLedgerSourceGraphVerifierTest test
mvn -Dtest=DisposableLedgerSourceGraphVerifierTest,DisposableMultiExchangeNativeRestorePostgresTest test
```
Then run consolidated QA #24 twelve-class regression and full four-component CI (Backend/Web/Android/Compose), checking non-skipped status of each. Record exact SHA, run URL, Maven pass/fail/error/skip, Docker PostgreSQL 18 startup and first stack trace on failure. Previous 479-test green CI @ `8c397d0` predates this code, and cannot be inherited.

## Required safety assertions
- No REFUND source promotion, no mixed POINT_EXCHANGE handling in generic source preflight.
- Purchase price, owner and denomination protected by AES source graph; archive-only bad purchase denied.
- Source/neighbor databases remain untouched by read-only verifier.
- Production `restorable=false`, `replaySupported=false`; reset/execute HTTP423; BETA HOLD until architect, QA, Release and owner approval.
