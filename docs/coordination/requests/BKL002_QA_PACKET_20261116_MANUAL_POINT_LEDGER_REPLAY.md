# BKL002-QA-20261116-15 — CI red category scope fix + test-only MANUAL POINT_LEDGER replay

**Status:** READY_FOR_QA / NEEDS_RETEST. **Producer:** Backend P0, **consumer:** independent QA, **tracking:** Issue #15. Developer Maven/Docker NOT_RUN (source connector). Java21 + isolated PostgreSQL18 only. No real family/reset/deploy.

## Priority CI P0 regression fixed
Latest independently inspected completed GitHub Actions Maven tests from QA:
- run 43953661904 @ d28048c9: Java **460 tests / 1 failure / 0 errors / 0 skipped**; Compose failed.
- run 43953986968 @ 4088b5e5: Java **461 tests / 1 failure / 0 errors / 0 skipped**; Compose failed.
- Both Java failures: `DisposableRewardCategoryCrossDatabasePostgresTest:107` expected SecurityException but got `IllegalArgumentException: CATEGORY_REPLAY_SCOPE_UNSUPPORTED` BEFORE null-map validation. Runtime category list is TreeSet alphabetical. `REWARD_DEFINITIONS` sorts before `REWARD_DEFINITION_CATEGORIES` (letter S precedes underscore in Java), contrary to previous patch. Commit **2539bf9** corrects exact allowed order back to REWARD_CATEGORIES, REWARD_DEFINITIONS, REWARD_DEFINITION_CATEGORIES. Does NOT drop scope validation or weaken replay protections. QA must rerun pinned test and verify normal replay and explicit null-map SecurityException/no writes.

## New bounded finance proof
- Test-only `src/test/java/com/olehkobylianskyi/familypoints/DisposablePointLedgerRestoreExecutor.java` commit **1d9fb14**: authenticated V50 POINT_LEDGER-only archive, explicit injective member and point_type remaps, independently supplied expected source currency code per point type; check target active currency same code and same family, target member ownership, family row lock and epoch=0, empty target history, transactionally insert immutable MANUAL rows with null source_id only. Reconcile **balance by member AND point_type**, not aggregated across currencies.
- Strictly **reject TASK, REWARD_PURCHASE, POINT_EXCHANGE, REFUND or other non-MANUAL source_type or any source_id**; their polymorphic referenced entities are not proven. A trusted source currency code is NOT in current archive, so this is isolated synthetic proof, not production-ready ledger restore.
- Two independently Flyway-migrated PG18 databases test in `DisposablePointLedgerCrossDatabasePostgresTest.java`, commit **39b699c**: manual +12/-2 => 10 point balance; neighbor +99 unchanged; foreign member map and wrong currency code fail with no target writes; duplicate replay blocked; separate TASK source_id=42 archive blocked, fresh target untouched. No full economic history replay.

## QA command (pinned SHA before and after)
```powershell
git pull --ff-only origin main
git rev-parse HEAD
.\mvnw.cmd "-Dtest=DisposablePointLedgerCrossDatabasePostgresTest,DisposableRewardCategoryCrossDatabasePostgresTest,DisposableRewardRequirementCrossDatabasePostgresTest,DisposableRewardObligationCrossDatabasePostgresTest,WorkspacePointLedgerSourceClosurePostgresTest,WorkspaceGroupLedgerPortableExportPostgresTest,WorkspaceRewardObligationArchiveClosurePostgresTest,ProtectiveBackupRewardHistoryClosureTest,WorkspaceFinancialRestoreClosureServiceTest,WorkspaceFinancialRestoreClosurePostgresTest" test
git rev-parse HEAD
```

## Acceptance
1. CI-SCOPE-01: correctly sorted exact-category scope gets beyond preflight; null target mapping fails `SecurityException` without NPE or `CATEGORY_REPLAY_SCOPE_UNSUPPORTED`; valid category replay reconstructs scoped graph.
2. FIN-MANUAL-01: source→target source/neighbor physically different PG DBs and Flyway V52; amounts +12/-2 and manual historic type, timestamps and source_id invariant; exact target balance=10 for member+currency; no neighbor drift (99).
3. FIN-MANUAL-02: missing/foreign member, mapped point type with mismatched code, duplicate replay, tampered AES, nonmanual TASK/source_id fail closed; no partial writes.
4. FIN-MANUAL-03: no production authentication, currency identity source archive verification, polymorphic history mapping or VERIFIED; `FinancialRestoreClosureService.replaySupported=false`, Reset Execute HTTP423, BETA HOLD.
5. QA posts actual SHA/test counters/failure stack traces; earlier 27/27@419efb1 validates only #01–03, not newest #12–15. All four release components non-skipped required for full CI claim.
