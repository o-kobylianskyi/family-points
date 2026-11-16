# BKL002-QA-20261116-10 — Portable reward obligations and blocked-target ancestry

**State:** READY_FOR_QA. **Backend developer execution:** NOT_RUN (source connector cannot execute Java 21/Docker). **Owner:** independent QA/Testing, **priority:** P0, **tracking:** Issue #15. Pin exact pre/post SHA. Use isolated synthetic PostgreSQL 18 only. Do not touch real family, staging, beta or prod data.

## Source implementation
- WorkspacePortableBackupExporter `ab5e29d`: three new encrypted export categories `REWARD_OBLIGATIONS`, `REWARD_OBLIGATION_BLOCKED_CATEGORIES`, `REWARD_OBLIGATION_BLOCKED_REWARDS`, scoped by reward_obligations.family_id. Purchases with obligations must include REWARD_OBLIGATIONS; obligations require REWARD_PURCHASES and optionally REWARD_REQUIREMENTS when nonnull requirement_id. Obligations with blocked categories/rewards require matching blocked target categories; links must include REWARD_OBLIGATIONS and corresponding REWARD_CATEGORIES / REWARD_DEFINITIONS. All source-family FK mismatches fail closed, no writes. Existing user point/reputation ledgers untouched.
- WorkspaceResetSnapshotService `d1322de` and WorkspaceProtectiveBackupService `a6fa821`: selected categories allowed and content hashed under epoch fence; no restore execution.
- ProtectiveBackupImportInspector `1c8ef88`: validates direct obligation family ownership, purchase FK, optional requirement FK, member identity rebind and blocked-category / blocked-reward composite IDs without synthetic `id`. Duplicate composite keys, dangling graph or missing category blocked. restorable=false always.
- WorkspaceFinancialRestoreClosureService `4fd6f0c`: obligation now exportable but nonreplayable; blocked associations independently counted by source family. 12 economic categories in scope. No VERIFIED promotion.
- Tests `37b115c` extend disposable PostgreSQL fixture with purchase+obligation+blocked category+blocked reward encrypted graph (counts 1/1/1), and omission of blocked categories denied. Authenticated inspector negative tests `cde4452` prove composite duplicates and missing parent. Existing category count assertions aligned `3b51f33`, `137a47c`, legacy obligation blocker updated `535b8cf`.

## QA command (Java 21 + Docker + Testcontainers PG18)
```powershell
git pull --ff-only origin main
git rev-parse HEAD
.\mvnw.cmd "-Dtest=WorkspaceRewardObligationArchiveClosurePostgresTest,ProtectiveBackupRewardHistoryClosureTest,WorkspacePortableArchiveForeignRelationTest,WorkspaceFinancialRestoreClosureServiceTest,WorkspaceFinancialRestoreClosurePostgresTest,WorkspacePointLedgerSourceClosurePostgresTest,WorkspaceGroupLedgerPortableExportPostgresTest,DisposableGroupLedgerCrossDatabasePostgresTest" test
git rev-parse HEAD
```

## QA acceptance
- OBL-ARC-01: encrypted complete owner + purchase + rewards + categories + obligation + blocked-category/reward graph, exactly one of each, inspector reports no missing graph but restorable=false.
- OBL-ARC-02: reward purchase with dependent obligation but no obligation selected fails `BACKUP_UNEXPORTED_REWARD_OBLIGATIONS`.
- OBL-ARC-03: obligation selected but PURCHASES/REQUIREMENTS parent missing fails; selecting obligations while omitting populated block categories/rewards fails `BACKUP_REWARD_OBLIGATION_BLOCKS_MISSING`.
- OBL-ARC-04: composite obligation/block FK references must have matching archived parent IDs; duplicate pairs rejected.
- OBL-ARC-05: foreign-family obligation member/purchase/requirement/blocked-category/reward prevents export. Verify source and neighbor unchanged.
- OBL-ARC-06: historical point/reputation ledger and previous archived rewards remain unmodified; source/target data epoch and ID remapping not assumed; strict test counts, FAIL/ERROR/SKIP and run SHA reported.

## Known gaps
- This exports real source content but no native replay/target member remapping. Unsupported payment/ledger source and guarded durable VERIFIED proof remain. It is not a production Restore, Reset, or owner fresh authorization feature. BETA HOLD and HTTP423 Reset Execute remain.
- QA's previous 27/27 PASS @419efb1 applies to old #01–03 only. Later #04–10 must receive independent exact-SHA acceptance. Do not count skipped jobs as PASS.
