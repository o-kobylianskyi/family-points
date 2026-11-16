# BKL002-QA-20261116-07 — Reward category association graph closure

**Status:** READY_FOR_QA. **Developer Maven/Docker tests:** NOT_RUN; no local JDK/container execution available via GitHub connector. **Producer:** Backend; **owner:** QA. **Priority:** P0 for preventing false recoverability claims. **Scope:** read-only preflight, no restore execution or release.

## Delivered source / test evidence
- Exporter commit c661d9b: selecting REWARD_DEFINITIONS requires zero linked reward_definition_categories for the source family; otherwise `BACKUP_UNEXPORTED_REWARD_CATEGORY_LINKS` before encrypted archive materialization. This association table is not a V50 portable category; silently restoring definitions while discarding category membership would not be faithful.
- Financial closure commit afc969f: includes `REWARD_CATEGORY_LINKS_UNEXPORTED`, scoped through reward_definitions.family_id; reports `RESTORE_DEPENDENCY_NOT_SELECTED` and `RESTORE_CATEGORY_NOT_EXPORTABLE` on existing links; never returns replaySupported=true.
- Integration fixture 56655c4 extends `WorkspaceRewardObligationArchiveClosurePostgresTest`: after independent obligation/requirement negative cases, inserts source reward category and reward-to-category link, expects explicit export block and read-only closure inventory; archived/source data remains intact.
- Existing closure counts updated from nine to ten in 9cdc8f5, ebc1359; mocked foreign-FK test preflight aligned bc7d8d6.

## QA batch (Java 21 + isolated Testcontainers PostgreSQL 18)
```powershell
git pull --ff-only origin main
git rev-parse HEAD
.\mvnw.cmd "-Dtest=WorkspaceRewardObligationArchiveClosurePostgresTest,WorkspacePortableArchiveForeignRelationTest,ProtectiveBackupRewardHistoryClosureTest,WorkspaceFinancialRestoreClosureServiceTest,WorkspaceFinancialRestoreClosurePostgresTest,WorkspacePointLedgerSourceClosurePostgresTest,WorkspaceGroupLedgerPortableExportPostgresTest,DisposableGroupLedgerCrossDatabasePostgresTest" test
git rev-parse HEAD
```

## Acceptance cases
- FIN-CAT-01: reward definition with linked category rejects export even if no requirements or obligations remain. Cause must be `BACKUP_UNEXPORTED_REWARD_CATEGORY_LINKS`.
- FIN-CAT-02: source-family count=1 is visible in Financial Closure; missing association category is explicitly named and no supported native replay declared.
- FIN-CAT-03: unrelated tenant's reward-category links do not affect this workspace's scoped exporter checks.
- FIN-CAT-04: obligation and requirement guards remain independent and fail-closed; previous positive archive paths without dependent category links remain possible.
- FIN-CAT-05: latest source/test SHA pinned, actual Maven test counters PASS/FAIL/ERROR/SKIP and no unrelated records mutated; report defects to Issue #15.

## Limitations and gate
This is protective gating only, not a complete reward category / requirement / obligation / point ledger archive. Follow-up must implement versioned source graph plus native target replay and immutable financial proof. Operator live tests deferred; no family wipe/deploy. VERIFIED not set; Reset Execute HTTP423; BETA HOLD.
