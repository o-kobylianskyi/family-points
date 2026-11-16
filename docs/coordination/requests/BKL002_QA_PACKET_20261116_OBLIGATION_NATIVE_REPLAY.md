# BKL002-QA-20261116-14 — Test-only reward obligation + blocked target native replay

**State:** READY_FOR_QA; **developer Maven/Docker:** NOT_RUN. **Producer:** Backend; **independent QA:** issue #15. **Synthetic disposable PostgreSQL ONLY.** Production Restore, selective/full reset and registry VERIFIED are explicitly NOT enabled. Requires Java 21/Docker/Testcontainers PostgreSQL 18.

## Implementation anchors
- `src/test/java/com/olehkobylianskyi/familypoints/DisposableRewardObligationRestoreExecutor.java` commit `e4110bb`, hardening `c4ca790`: decrypt and validate authenticated V50 archive for REWARD_OBLIGATIONS plus composite REWARD_OBLIGATION_BLOCKED_CATEGORIES and REWARD_OBLIGATION_BLOCKED_REWARDS. Required source parent categories: REWARD_PURCHASES, optional REWARD_REQUIREMENTS, REWARD_CATEGORIES, REWARD_DEFINITIONS. Exact source parent IDs and source family verified before writing.
- Requires **explicit complete injective** maps for source→target member, purchase, requirement, category and reward definition IDs; no inferred ID reuse. Target family row locked FOR UPDATE, epoch=0 and obligation history empty. Every mapped target belongs to target family; purchases' owning members are checked; historical purchase member/title/price/status must also match. Reconstructed obligations preserve title/status/blocking_mode/timestamps and FK graph. Blocked category/reward links inserted within the **same transaction** and source/target counts reconciled.
- This is a `src/test` class only, not a Spring bean or production execution. It assumes target purchases/definitions/categories/members/requirements have already been independently reconstructed. It **does not** independently prove source↔target currency code identity, point/reputation ledger economics, delivery/payment semantics or owner authorization; therefore no full financial restore is approved.
- `DisposableRewardObligationCrossDatabasePostgresTest.java` commit `dee123c`, hardening `d4f1b21`: two physically independent PG18 containers with Flyway V52, AES source family reward+category+purchase+obligation+two blocked targets. Builds fresh target family and untouched neighbor; validates wrong-family member map rejected with no mutations, a changed target purchase amount rejected (even within same family), valid 1/1/1 obligation/blocked-links replay, duplicate replay/tampered file denial, preserved source and neighbor. Uses a separately AES-sealed test archive with a second obligation title exceeding PostgreSQL varchar(255); the first insert must be automatically rolled back on second-row SQL error.

## QA command (pinned SHA before and after)
```powershell
git pull --ff-only origin main
git rev-parse HEAD
.\mvnw.cmd "-Dtest=DisposableRewardObligationCrossDatabasePostgresTest,DisposableRewardRequirementCrossDatabasePostgresTest,DisposableRewardCategoryCrossDatabasePostgresTest,WorkspaceRewardObligationArchiveClosurePostgresTest,ProtectiveBackupRewardHistoryClosureTest,WorkspaceFinancialRestoreClosureServiceTest,WorkspacePortableArchiveForeignRelationTest,DisposableGroupLedgerCrossDatabasePostgresTest" test
git rev-parse HEAD
```

## Required acceptance
- **OBL-NATIVE-01:** two isolated DBs, valid encrypted scoped source archive, complete five explicit maps; target obligation and category/reward blockers each 1 with remapped FKs.
- **OBL-NATIVE-02:** no neighbor/source changes. Wrong target family or mismatched original purchase economics fails closed with zero target obligations.
- **OBL-NATIVE-03:** authenticated crafted two-row archive with second too-long title causes actual DB error and **atomic rollback** to zero target obligations/blocked links.
- **OBL-NATIVE-04:** replay twice and tampered ciphertext both denied; no duplicate or partial graph.
- **OBL-NATIVE-05:** previous CI red repair #12 and requirement replay #13 remain green, no masked errors/skipped tests. QA reports exact git SHA, executed count and PASS/FAIL/ERROR/SKIP with stack traces to #15.
- **OBL-NATIVE-06:** regardless of bounded pass, `ProtectiveBackupImportInspector.restorable=false`, `WorkspaceFinancialRestoreClosureService.replaySupported=false`, reset/execute remains HTTP 423, VERIFIED inaccessible and BETA HOLD.

## Caution
This test-only executor is NOT a complete production recovery path. Individual/group point ledger `source_id` polymorphism, source currency metadata, reward purchase settlement, true member binding and durable receipt/30-day retention still require independent engineering and QA. Historical 27/27 PASS @419efb1 proves packets #01–03 only, not #12–14. Current exact four-component CI attestation is still required.
