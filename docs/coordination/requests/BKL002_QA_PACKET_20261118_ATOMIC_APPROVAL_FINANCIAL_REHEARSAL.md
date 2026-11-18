# BKL-002 Backend QA #57–60 — Atomic PG18 approval and financial rehearsal

2026-11-18. TEST ONLY. No production restore or points ledger changes.

## Independent CI evidence
- Full GitHub Actions run #44196224270 @304819c5: Backend 493 tests/2 failures/0 errors/0 skips from exception expectations in `DisposableLedgerSourceGraphVerifierTest`.
- Run #44262590272 @2353013d: Backend 496 tests/1 failure, expected IllegalArgumentException but duplicate currency code verifier returned SecurityException.
- Run #44262614624 @8aed240a: Backend 496 tests/1 failure, expected SecurityException but invalid zero currency row ID inspector returned IllegalArgumentException.
- Current source after those runs separates duplicate valid currency codes => SecurityException, invalid zero ID => IllegalArgumentException; **actual current SHA PASS not established**. Web/Android/Compose passed at those historical SHA; full gate remains red.

## Commits
- `7d2bcaf6`: TEST-only `DisposableDurableRestoreApprovalStore.consumeWithSyntheticFinancialWrite`. Lock `families.data_epoch FOR UPDATE`, verify synthetic HMAC permit, insert unique operation UUID into QA-only approval table and mapped positive amount into QA-only financial table, within one `TransactionTemplate`. Injected exception after financial insert causes both SQL writes to roll back.
- `c5a3b440`: PG18 Testcontainers cases for injected late rollback/clean retry, second attempt denied across fresh helper instances, wrong original-member mapping/stale target epoch no side effects, concurrent operation UUID 1 success + 1 reject and single financial row.
- Financial rehearsal table is `qa_restore_financial_rehearsal` and is created only in disposable test DB. Existing production `PointService` is NOT called.

## QA exact-SHA run
```powershell
mvn -Dtest=DisposableDurableRestoreApprovalStorePostgresTest,DisposableRestoreApprovalGateTest,DisposableMemberBindingReceiptVerifierTest,DisposableLedgerSourceGraphVerifierTest,DisposableMultiExchangeNativeRestorePostgresTest test
```
Then QA #24 12-class regression and all-four CI with latest release cases, all non-skipped at a pinned SHA; report run URL, totals, first stack, real Docker PG18 outcome.

## Non-claims
Synthetic actor and HMAC key are not owner authentication. Source-member identity after original database loss unresolved (no signed portable FAMILY_MEMBERS V50). PostgreSQL atomicity here proves only a QA reservation + QA financial row, not production multi-category restore. No production Flyway migration, no owner-approved durable restore receipt, no permissions changed.

Production restorable=false, replaySupported=false, Reset Execute HTTP423, BETA HOLD.
