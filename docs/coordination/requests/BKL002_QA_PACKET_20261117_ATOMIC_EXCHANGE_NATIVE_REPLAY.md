# BKL002-QA-20261117-22 — Test-only atomic native exchange request + paired ledger replay

**State:** READY_FOR_QA. **Backend execution:** NOT_RUN (GitHub connector, no Maven/Docker process). **Owner:** Backend P0; independent QA: Issue #15. **Environment:** Java 21 + two isolated PostgreSQL 18 Testcontainers and synthetic families only. **Safety:** NO production runner, HTTP endpoint, VERIFIED state, full or selective reset Execute.

## Source code (test sources ONLY)
- `DisposableExchangeNativeRestoreExecutor.java` commit `8ac8105`; null-safe map hardening `1e50dc5`; fail-closed missing/foreign currency `0c61e95`.
- Input: authenticated AES V50 source portable archive, exact `POINT_EXCHANGE_RATES`, `POINT_EXCHANGE_REQUESTS`, `POINT_TYPES`, `POINT_LEDGER` scopes; bounded **one approved exchange request and precisely two ledger legs**, one rate and two distinct currencies. Reject missing categories, nonpositive/extra source references, reviewer requiring identity rebind, price/ratio overflow, wrong original source_id/member/currency/type/amount, and non-injective/missing source→target member/currency/rate mapping.
- The target is an independent disposable PG family whose member, active currencies and exchange rate have been **pre-created** by the test fixture. No real user identity matching is performed by this executor. In a target SQL transaction, SELECT FOR UPDATE family; require epoch 0 and no prior exchange requests/ledger entries for target family; check member belongs target; require each mapped target point type is active/family-owned and has same sealed source code; require mapped target exchange rate belongs family and links exactly mapped from/to currencies. It need not have today's mutable amount equal the source historical request ratio.
- Insert restored request including **immutable source request snapshot** `rate_from_amount,rate_to_amount,from_amount,to_amount,status,requested_at,processed_at`, using fresh target ID. Insert two source ledger legs with target member, currency, `source_type=POINT_EXCHANGE`, `source_id=newRequest`, preserved signed amount, type, created_at and description; fresh ledger IDs. The source→target ledger ID map is returned. All three inserts occur inside one Spring `TransactionTemplate`.
- Immediately rerun independent `DisposableExchangeLedgerSourceAuditor` against restored target, assert request/leg economics and per-target-member currency net totals `-from_amount` and `+to_amount` before commit; verify immutable foreign source request and neighbor are untouched. No production restore service or `replaySupported=true` transition.
- `DisposableExchangeNativeRestorePostgresTest.java` commit `404bcfb`: physically separate Flyway PostgreSQL 18 DBs; source original 10 POINTS→2 TOKENS with both ledger legs, existing unrelated target DB neighbor, new target family with mapped member/currencies/rate. Export signed source archive from actual PG. Invalid foreign neighbor member/rate/currency and null map fail before target writes; tampered AES fails; replay twice denied. Real **atomic rollback proof**: add test-only PostgreSQL `CHECK ... NOT VALID` rejecting target EARN only, so request and first SPEND insert occur before second credit hits database error; assert zero target request and ledger rows after rollback; remove constraint and replay valid 3-row graph. Source and neighbor remain identical.
- This tests an artificially bounded synthetic single exchange. It does NOT make real-world family financial restoration safe.

## QA command: pin exact source SHA and actual counters
```powershell
git pull --ff-only origin main
git rev-parse HEAD
.\mvnw.cmd "-Dtest=DisposableExchangeNativeRestorePostgresTest,DisposableExchangeTargetReconciliationPostgresTest,DisposableExchangeLedgerSourceAuditorPostgresTest,ProtectiveBackupExchangeSourceGraphTest,DisposableLedgerSourceGraphVerifierTest,DisposablePointLedgerCrossDatabasePostgresTest,DisposableRewardCategoryCrossDatabasePostgresTest,DisposableRewardRequirementCrossDatabasePostgresTest,DisposableRewardObligationCrossDatabasePostgresTest,WorkspaceFinancialRestoreClosureServiceTest,WorkspaceFinancialRestoreClosurePostgresTest" test
git rev-parse HEAD
```

## Required acceptance
1. EX22-01 sealed original rate + request + two legs, correct synthetic target ID mappings and two target currencies with source codes, reconstructed target REQUEST and exactly 2 entries with source_id remapped to NEW request ID; 10→2 balances.
2. EX22-02 missing/foreign target member, currency or rate, duplicate/improper map, old source rate mismatch, reviewer rebind requirement, wrong signed ledger graph and corrupted file fail closed, no destination writes.
3. EX22-03 **forced SQL failure on second EARN** proves rollback of first SPEND and the restored request, not just a preflight exception. No target partial rows; source and neighbor unchanged.
4. EX22-04 replay twice denied; fresh source and target request IDs need not differ or match because they belong to distinct databases; verified identity is by independent connection and complete map, not numeric coincidence.
5. EX22-05 collect pinned exact SHA, Maven PASS/FAIL/ERROR/SKIP counts and real failure stacks from QA. Run prior #20/21 source tests too. Do not assume any current exact-SHA green from source-only reports or old 27/27 acceptance.
6. EX22-06 Production `WorkspaceFinancialRestoreClosureService.replaySupported=false`, `ProtectiveBackupImportInspector.restorable=false`, Reset Execute HTTP423 and **BETA HOLD**. No release authorization from test-only success.

## Remaining blockers
- The isolated executor cannot prove live owner reauth, source↔target member identity, full currency denomination/economy configuration, arbitrary exchange history, member/group/reputation multi-currency balance reconciliation, immutable source ledger provenance for other source_type values, durable encrypted backup retention, protected restoration receipt or atomic multi-category reset. REFUND remains caller-dependent and unsupported.
