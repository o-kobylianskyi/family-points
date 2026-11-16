# BKL002-QA-20261117-21 — Exchange historical rate fix and source→target cross-DB reconciliation

**State:** READY_FOR_QA. **Developer execution:** NOT_RUN (connected GitHub source tool cannot run Maven/Docker). **Track:** Backend P0. QA intake via Issue #15. Java 21, Docker, synthetic PostgreSQL 18 ONLY, no real family Restore or Reset.

## QA finding fixed first — QA-025
- QA source review issue #15 comment `6931899220`: `DisposableExchangeLedgerSourceAuditor` checked both ledger amounts against an approved request but did not prove that the request's amounts conformed to its own **immutable original rate snapshot**. An internally consistent but tampered request + transactions could pass.
- Commit `2d6b35f`: selected original `rate_from_amount/rate_to_amount` from `point_exchange_requests`; positive int inputs; `from_amount % rate_from_amount==0`, `Math.multiplyExact(from_amount/rate_from_amount,rate_to_amount)==to_amount`. Fails `EXCHANGE_SOURCE_HISTORIC_RATE_MISMATCH` or overflow; current mutable `point_exchange_rates.to_amount` must **NOT** override historical snapshots.
- PostgreSQL source regression `c5a973b`: changed snapshot numerator or denominator rejected; mutable current rate 10→5 permitted while original valid request remains 10→2.

## New read-only target proof
- `src/test/java/com/olehkobylianskyi/familypoints/DisposableExchangeTargetReconciliationVerifier.java` `d2540c5`: two independent JDBC source/target connections; calls upgraded source auditor in BOTH DBs; requires **explicit complete injective** source→target member map and two point type ID maps; target family/request ownership and exactly two transactions verified independently; compares immutable original request rate snapshots, from/to amount, point type **codes** and ledger pair economics. Does not infer IDs from matching numerals, does not write records, does not implement source→target native replay.
- `DisposableExchangeTargetReconciliationPostgresTest.java` `bf50052`: two independent PG18 databases Flyway-migrated; source approved exchange 10 POINTS→2 TOKENS, unrelated neighbor in target DB, re-created target family with exchange. Correct remaps reconcile, foreign member/currency/neighbour mappings blocked, wrong target currency code, valid-but-altered rate snapshot and altered economics blocked; missing target credit leg blocked; original source and neighbor untouched. Tests are synthetic: no real balance transfer.
- Full user-facing production `WorkspaceFinancialRestoreClosureService.replaySupported=false` and `ProtectiveBackupImportInspector.restorable=false` unchanged, reset Execute HTTP 423, BETA HOLD.

## Suggested pinned QA run
```powershell
git pull --ff-only origin main
git rev-parse HEAD
.\mvnw.cmd "-Dtest=DisposableExchangeTargetReconciliationPostgresTest,DisposableExchangeLedgerSourceAuditorPostgresTest,ProtectiveBackupExchangeSourceGraphTest,DisposableLedgerSourceGraphVerifierTest,DisposablePointLedgerCrossDatabasePostgresTest,DisposableRewardCategoryCrossDatabasePostgresTest,DisposableRewardRequirementCrossDatabasePostgresTest,DisposableRewardObligationCrossDatabasePostgresTest,WorkspaceFinancialRestoreClosureServiceTest,WorkspaceFinancialRestoreClosurePostgresTest" test
git rev-parse HEAD
```

## QA acceptance / blockers
1. EX21-01 QA-025 source regression: historical snapshot 10:2 with amounts 10:2 accepted regardless of changed mutable current rate; historically inconsistent 10:3 or nondivisible original rate denied.
2. EX21-02 source & target independent PostgreSQL 18, target family and two currencies explicitly mapped, both original and reconstructed exchange requests approved and each 2-leg ledger valid, amount and snapshot/code reconciliation exact.
3. EX21-03 wrong/neighbor member, wrong/neighbor point type IDs, target currency code drift, source/target ratio mismatch, modified target amount and absent target ledger leg all fail closed with source/neighbor unchanged.
4. EX21-04 no database writes by either verifier; test fixtures may write only synthetic rows. No source credentials or real backup bytes logged. No production identity proof, no VERIFIED/restore or point ledger native apply.
5. EX21-05 pin exact pre/post SHA and report complete Maven PASS/FAIL/ERROR/SKIP, environment + stack traces, test omissions. Do not mark the prior red CI fixed without an independent latest-SHA run and full four-component proof.

## Next P0
- Native TEST-ONLY exchange replay would require independently verified member, currency codes/denomination policy, rate, request statuses/identity, ledger source ID graph and group/reputation balance closure, with rollback and an immutable portable snapshot receipt. This packet **does not** deliver native financial replay.
- REFUND source_type is still caller-dependent; cannot assign a universal `source_id` foreign entity without tracing every invocation. Remain fail-closed. Production FULL/SELECTIVE reset execute HTTP 423 and BETA HOLD.
