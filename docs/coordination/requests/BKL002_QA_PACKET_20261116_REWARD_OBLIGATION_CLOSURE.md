# BKL002-QA-20261116-06 — Reward obligations/requirements archive closure

**Status:** READY_FOR_QA. **Developer Maven:** NOT_RUN (GitHub connector does not provide JDK/Docker runner). **Priority:** P0, Backend producer, QA owner, Issue #15. No real family/test/prod changes.

## Delivered
- Exporter 64d7e8b: REWARD_PURCHASES archive denied if source purchaser-family contains dependent reward_obligations. Error BACKUP_UNEXPORTED_REWARD_OBLIGATIONS.
- Exporter 0e45999: REWARD_DEFINITIONS / REWARD_REQUESTS export denied if source family has reward_requirements. Error BACKUP_UNEXPORTED_REWARD_REQUIREMENTS.
- Financial closure a2fdfac: explicit REWARD_OBLIGATIONS_UNEXPORTED and REWARD_REQUIREMENTS_UNEXPORTED row inventory, dependency/unsupported category blockers; never replaySupported.
- Tests 7c44563 and 5d6de03: PostgreSQL 18 owner, member, currency, reward, purchase, obligation and requirement, fail-closed encrypted exporter; verify original data preserved. Count and mocks aligned in f39f74e, 021e846, d5a0319, 2afaf22.
- Earlier QA reports 27/27 PASS for packet #01-#03 at SHA 419efb1; packets #04-#05 not yet accepted on current SHA. No assumption these new changes passed.

## QA execution (synthetic disposable PostgreSQL only)
```powershell
git pull --ff-only origin main
git rev-parse HEAD
.\mvnw.cmd "-Dtest=WorkspaceRewardObligationArchiveClosurePostgresTest,WorkspacePortableArchiveForeignRelationTest,ProtectiveBackupRewardHistoryClosureTest,WorkspaceFinancialRestoreClosureServiceTest,WorkspaceFinancialRestoreClosurePostgresTest,WorkspacePointLedgerSourceClosurePostgresTest,WorkspaceGroupLedgerPortableExportPostgresTest,DisposableGroupLedgerCrossDatabasePostgresTest" test
```

## Expected cases
- FIN-LIFE-01: purchase with obligation denied before authenticated archive creation; no purchase or obligation mutation.
- FIN-LIFE-02: reward definition with requirement denied rather than silently omitting conditions.
- FIN-LIFE-03: financial closure includes both unsupported categories; any history yields explicit not-selected and not-exportable blockers, replaySupported=false.
- FIN-LIFE-04: existing cross-family purchase/request/task relations remain blocked and source/neighbor tenant unchanged.
- FIN-LIFE-05: absent dependent history does not spuriously block previous legal archive flows; re-run existing suites, report actual test totals and exact pre/post SHA.

## Known gaps / gates
- This is not a native replay, a complete list of all financial dependencies, durable VERIFIED, owner reauth, file upload/download API or FULL/SELECTIVE atomic restore. Further complete FK + polymorphic source mapping required.
- QA picks up explicitly from GitHub and reports PASS/FAIL/BLOCKED_ENV with pinned SHA to Issue #15. File creation does not run another chat.
- Reset Execute HTTP423. BETA HOLD.
