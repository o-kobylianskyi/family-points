# BKL002-QA-20261116-09 — Reward categories and definition-category associations (portable source graph)

**Status:** READY_FOR_QA; developer Maven/Docker NOT_RUN (GitHub connector provides source writes, not Java/Docker runtime). **Priority:** P0. Backend producer, independent QA owner, issue #15. Synthetic Testcontainers PostgreSQL only; no real family, reset or deployment.

## Delivered scope
- Source exporter e6a91fb: new encrypted categories `REWARD_CATEGORIES` (direct family_id) and `REWARD_DEFINITION_CATEGORIES` (through reward_definition_id→reward_definitions.family_id); both are optional V50 archive categories. Existing REWARD_DEFINITIONS export with any category membership requires selection of `REWARD_DEFINITION_CATEGORIES`; export of populated links requires both REWARD_DEFINITIONS and REWARD_CATEGORIES. Wrong-family link `category_id` rejected with `BACKUP_REWARD_CATEGORY_FOREIGN_RELATION`. Missing parent selections rejected with `BACKUP_REWARD_CATEGORY_PARENT_MISSING`.
- Snapshot digest category whitelist c8dca59 and protective metadata registry a8329d6 include both names.
- Offline inspector d2354ba validates reward category family_id and composite association PK `(reward_definition_id,category_id)`, rejects duplicate pairs, requires both parent categories and checks parent IDs through archive graph. It always reports `restorable=false` — successful decryption/inspection is not native replay evidence.
- Financial closure 7ff2178: renamed old `REWARD_CATEGORY_LINKS_UNEXPORTED` to `REWARD_DEFINITION_CATEGORIES`, now exportable but non-replayable with `RESTORE_FINANCIAL_NATIVE_REPLAY_UNIMPLEMENTED` when selected. `REWARD_OBLIGATIONS_UNEXPORTED` remains a blocker.
- PostgreSQL source fixture 61d0d38 plus 834bb27: secure encrypted definition/category/association graph count 1/1/1, missing parent selection denied, live foreign-family category relation denied. Historical missing-category-links assertion aligned 2b43fc2. Authenticated inspector fixtures ead594a test valid composite key, missing parent, duplicate pair, foreign-family category.

## QA run
```powershell
git pull --ff-only origin main
git rev-parse HEAD
.\mvnw.cmd "-Dtest=WorkspaceRewardObligationArchiveClosurePostgresTest,ProtectiveBackupRewardHistoryClosureTest,WorkspacePortableArchiveForeignRelationTest,WorkspaceFinancialRestoreClosureServiceTest,WorkspaceFinancialRestoreClosurePostgresTest,WorkspacePointLedgerSourceClosurePostgresTest,WorkspaceGroupLedgerPortableExportPostgresTest,DisposableGroupLedgerCrossDatabasePostgresTest" test
git rev-parse HEAD
```

## Expected cases
- CAT-ARC-01: authenticated `REWARD_DEFINITIONS+REWARD_CATEGORIES+REWARD_DEFINITION_CATEGORIES` export includes exact row counts and composite relationship; inspector has no missing-parent blocker but remains unrestorable.
- CAT-ARC-02: reward-definition-only source with category links blocked as `BACKUP_UNEXPORTED_REWARD_CATEGORY_LINKS`.
- CAT-ARC-03: association-only export blocked `BACKUP_REWARD_CATEGORY_PARENT_MISSING`. Inspector missing/dangling parent blocked.
- CAT-ARC-04: repeated `(reward_definition_id,category_id)` in authenticated archive rejected; foreign `family_id` or foreign linked category denied.
- CAT-ARC-05: source and neighbor rows unchanged, earlier group-ledger/requirements/obligations negative regressions pass; record exact SHA and PASS/FAIL/ERROR/SKIP, not just BUILD SUCCESS.
- CAT-ARC-06: `VERIFIED`, production native restore/execute and BETA remain disabled. Any skipped tests and CI partial green must not be counted as full acceptance.

## Known follow-up
- Source archive now records categories and links, but does **not** support native association replay, reward obligations and blocked category/reward relations, ownership reauth, snapshot VERIFIED promotion, global category closure, or financial settlement restore.
- QA should consolidate current #04-#09 exact-SHA runs, include failures and environment evidence in issue #15. Prior QA 27/27@419efb1 only covered #01-#03; no assumption of latest-source acceptance.
- Reset Execute HTTP 423, BETA HOLD. No real family modifications.
