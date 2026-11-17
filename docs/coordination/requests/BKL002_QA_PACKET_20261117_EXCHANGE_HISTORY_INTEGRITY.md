# BKL002-QA-20261117-24 — Exchange history integrity, exact semantic SHA256 and graph cardinality

**Status:** READY_FOR_QA / Developer Maven-Docker NOT_RUN. **Track:** Backend P0, independent QA Issue #15. **Hard safety gate:** `ProtectiveBackupImportInspector.restorable=false`; `WorkspaceFinancialRestoreClosureService.replaySupported=false`; Reset Execute HTTP423 / BETA HOLD. Synthetic isolated PostgreSQL only, NO real families.

## Scope implemented
1. New `src/test/java/com/olehkobylianskyi/familypoints/DisposableExchangeHistoryIntegrityVerifier.java` commit `0d6d1b0`, **read-only/test-only**. Accepts AES V50 signed `POINT_EXCHANGE_RATES`, `POINT_EXCHANGE_REQUESTS`, `POINT_LEDGER`, `POINT_TYPES`; target JDBC with explicit source→target complete, injective member, point-type, rate, request and ledger-ID maps. Requires all source request/ledger IDs unique; all ledger source_id reference archived requests; no arbitrary request or extra ledger categories.
2. For each source request and mapped target row, verifies target owning family and precise economic content: member ID, mapped exchange rate and point-type IDs, APPROVED status, original `rate_from_amount/rate_to_amount/from_amount/to_amount`, original `requested_at/processed_at` (timestamp normalization). For each source ledger row, maps member, currency, original source request ID→target request ID, then checks `source_type=POINT_EXCHANGE`, `SPEND/EARN`, signed amount, `created_at` and description. No equivalence by coincident numerical IDs.
3. Canonical rows keyed by **source** request/ledger IDs, stable normalized semantics hashed with SHA-256 on both source/target representations. Row-level string comparison catches drift **before** hash match. SHA-256 here is an integrity summary of mapped economic history, not a new digital signature nor independent owner authorization. AES archive authentication is provided by existing signed source file.
4. Require **exact target family request count and total point ledger count** matching all archived exchange rows. This rejects extra net-zero entry pairs that aggregate-balance comparisons miss. Then verify aggregate signed sums per target member×currency. All checks are **read-only**; no production restoration.
5. Extended `DisposableMultiExchangeNativeRestorePostgresTest.java` commits `a0ea5d2`, `4d70a97`, `359132d`: after previously authored two PG18 source-to-target replay, verify source/target SHA-256 equality for 2 requests/4 legs and -30 POINTS / +6 TOKENS, flip target ledger description (same balance) → FAIL, move target request timestamp by 1s → FAIL, append two extra MANUAL ledger rows +1 and -1 (same net balance) → FAIL, invalid target ledger mapping → FAIL. Revert each mutation and require restored exact semantic SHA. Existing #23 late final-credit SQL rollback, source/neighbor preserved and duplicate replay tests retained.

## QA run (JDK21, Docker/Testcontainers, exact HEAD)
```powershell
git pull --ff-only origin main
git rev-parse HEAD
.\mvnw.cmd "-Dtest=DisposableMultiExchangeNativeRestorePostgresTest,DisposableExchangeNativeRestorePostgresTest,DisposableExchangeTargetReconciliationPostgresTest,DisposableExchangeLedgerSourceAuditorPostgresTest,ProtectiveBackupExchangeSourceGraphTest,DisposableLedgerSourceGraphVerifierTest,DisposablePointLedgerCrossDatabasePostgresTest,DisposableRewardCategoryCrossDatabasePostgresTest,DisposableRewardRequirementCrossDatabasePostgresTest,DisposableRewardObligationCrossDatabasePostgresTest,WorkspaceFinancialRestoreClosureServiceTest,WorkspaceFinancialRestoreClosurePostgresTest" test
git rev-parse HEAD
```

## Required acceptance
- HIST-24-01 same original and mapped target economic rows, normalized timestamp/order, full explicit injective maps: exactly equal semantic SHA-256 digest; balances -30/+6.
- HIST-24-02 sum-neutral text modification, changed historical timestamp, or a hidden net-zero pair of target rows: all denied, even if total balances still equal.
- HIST-24-03 wrong ledger ID map, missing row, duplicate source IDs and orphan source_id: fail closed. Remaining same-SHA row mappings must be verified by explicit cross-database IDs, not guess.
- HIST-24-04 old #23 forced **last +4 EARN SQL rollback** still demonstrates entire request+ledger transaction rollback (two requests/four legs), not merely preflight. All source and neighbor balances/history unchanged.
- HIST-24-05 QA independently reports exact HEAD before/after, test counts PASS/FAIL/ERROR/SKIP and failure logs. Source review does not imply PASS. Historical 27/27 and unrelated CI artifact gate do not promote Beta.
- HIST-24-06 no new endpoint or production financial restore authorization: `restorable=false`, `replaySupported=false`, Reset Execute HTTP423 / BETA HOLD.

## Exclusions / next P0
This bounded verification still does not guarantee correct restoration of all family members, unselected TASK/REWARD_PURCHASE/REFUND histories, actual currency denomination settings, group/reputation ledgers, member identity reauth, backup persistence/retention or protected restoration receipt. Future work: QA failure triage; then test-only source/target multi-member/multi-currency history reconciliation and cross-category orphan detection before any production restore plan.
