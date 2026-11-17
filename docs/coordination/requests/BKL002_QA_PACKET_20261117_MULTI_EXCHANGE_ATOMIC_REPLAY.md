# BKL002-QA-20261117-23 — Multi-exchange atomic native replay, strict disposable PostgreSQL

**State:** READY_FOR_QA / Developer Maven+Docker NOT_RUN. **Owner:** Backend P0, **independent QA:** Issue #15. This is a test-only executor and test-only PostgreSQL fixtures, never an API or real-family restoration. Production `restorable=false`, `replaySupported=false`, Reset Execute HTTP423, BETA HOLD.

## Changes
- `src/test/java/com/olehkobylianskyi/familypoints/DisposableMultiExchangeNativeRestoreExecutor.java`, `d56aa74`, supports **1–20 approved historic exchange requests** in one authenticated V50 `POINT_EXCHANGE_RATES+POINT_EXCHANGE_REQUESTS+POINT_LEDGER+POINT_TYPES` archive. It does not invent source IDs; validates unique positive original IDs, source tenant, approved status, original rate numerator/denominator exact divisible amount, no reviewer requiring separate identity remap, FK closure and exactly **two** original ledger legs per request (one debit, one credit; both with same original source_id/member). Non-exchange or unrelated ledger rows, duplicate IDs/legs and missing reference fail closed. Underlying AES/inspection still requires original signed source archive; no live family data.
- Requires explicit **complete and injective** source→target maps for members, both point types, and referenced exchange rates. Target parent family row is locked; `data_epoch=0`, no preexisting target request/ledger history. Each mapped parent must be target-owned; mapped currency must have same authenticated code and be active, mapped rate must own the target point-type pair. No implicit numeric ID correspondence or production account reauth.
- In **one TransactionTemplate** inserts each historic request with fresh ID and original `rate_from_amount/rate_to_amount/from_amount/to_amount/status/timestamps`, inserts both new signed ledger transactions with remapped `source_id=newRequestId`, verifies `DisposableExchangeLedgerSourceAuditor.audit` for each reconstructed pair, then reconciles aggregate net balance **per target member × currency** across all requests before commit. Returns source→target request and ledger maps plus balances.
- `src/test/java/com/olehkobylianskyi/familypoints/DisposableMultiExchangeNativeRestorePostgresTest.java`, `dd44b48`: two independent Flyway PostgreSQL18 instances, source 10 POINTS→2 TOKENS AND 20 POINTS→4 TOKENS, unaffected target-neighbor exchange, fresh target graph. Real encrypted source archive, invalid null/member/currency/rate maps rejected, tampered AES rejected. **A test-only `CHECK ... NOT VALID` rejects only the 2nd request's +4 EARN**: verifies complete rollback of earlier first request's debit+credit and second request+debit, leaving zero target rows. Remove constraint, replay 2 requests+4 legs, verify `POINTS=-30`, `TOKENS=+6`, source/neighbor invariance, duplicate attempt denied.
- Old #22 single-exchange `DisposableExchangeNativeRestoreExecutor` unchanged and must remain green. Production Restore and BKL-002 activation still blocked even if tests pass.

## QA pinned command (Java21 + Docker/Testcontainers)
```powershell
git pull --ff-only origin main
git rev-parse HEAD
.\mvnw.cmd "-Dtest=DisposableMultiExchangeNativeRestorePostgresTest,DisposableExchangeNativeRestorePostgresTest,DisposableExchangeTargetReconciliationPostgresTest,DisposableExchangeLedgerSourceAuditorPostgresTest,ProtectiveBackupExchangeSourceGraphTest,DisposableLedgerSourceGraphVerifierTest,DisposablePointLedgerCrossDatabasePostgresTest,DisposableRewardCategoryCrossDatabasePostgresTest,DisposableRewardRequirementCrossDatabasePostgresTest,DisposableRewardObligationCrossDatabasePostgresTest,WorkspaceFinancialRestoreClosureServiceTest,WorkspaceFinancialRestoreClosurePostgresTest" test
git rev-parse HEAD
```

## Acceptance criteria
1. MULTI-EX-23-01 Two source request IDs, four signed original legs and sealed original rates/currency catalog reconstructed with **fresh target IDs**, all new `source_id` values rebinding to target request IDs.
2. MULTI-EX-23-02 Original per-request historic ratio and amounts preserved; target parent/member ownership and currency-code matching; unknown/foreign/duplicate IDs and bad map fail closed with zero writes.
3. MULTI-EX-23-03 Last +4 credit SQL exception proves rollback of **all previous exchanges**, not only current pair, leaving zero target rows. No source/neighbor drift.
4. MULTI-EX-23-04 After successful full replay aggregate balances exactly POINTS -30 and TOKENS +6 for target member; second replay denied with no duplicated ledger or requests.
5. MULTI-EX-23-05 QA reports actual pinned SHA, Maven PASS/FAIL/ERROR/SKIP counts, stack traces and runtime evidence. Do not infer green from source review, earlier 27/27 or unrelated Release artifact checks.
6. MULTI-EX-23-06 Production `WorkspaceFinancialRestoreClosureService.replaySupported=false`, `ProtectiveBackupImportInspector.restorable=false`, Reset Execute HTTP423 and BETA HOLD unchanged.

## Still P0 blocked
- Real owner account identity, member remap verification, point currency denomination configuration and historical custom policies, complete task/purchase/refund point, group and reputation ledgers and cross-category reconciliation, encrypted retention/receipt + independent restore of whole family, all-four exact-SHA CI acceptance. This is **not** general finance restore and not a production authorization.
