# BKL002-QA-20261116-16 — Authenticated POINT_TYPES currency catalog and manual-ledger closure

**Status:** READY_FOR_QA. **Owner:** Backend; **consumer:** independent QA through Issue #15. **Developer Maven/Docker:** NOT_RUN (GitHub source connector only). **Priority:** P0. Only synthetic disposable PostgreSQL. No real family updates, production restorer, verified reset or release authorization.

## Changes and exact evidence
- Portable source exporter `e6f4bde` now supports `POINT_TYPES`, scoped by point_types.family_id. Source rows include original `id,code,name,active,sort_order` and any other schema fields in the AES-authenticated V50 archive; content is covered by digest+manifest. Registry `95d8b69` and snapshot digest whitelist `ceef29c` allow category selection.
- Offline `ProtectiveBackupImportInspector` `15c8717` validates POINT_TYPES direct family owner, nonblank/unique code, and references `point_type_id` from POINT_LEDGER, GROUP_POINT_LEDGER, REWARD_DEFINITIONS, REWARD_PURCHASES and nullable REWARD_REQUESTS approved_point_type_id. Missing catalog creates explicit `MISSING_CATEGORY:POINT_TYPES`; missing denomination row creates `DANGLING_REFERENCE`. Existing identity and historical polymorphic source blockers remain; `restorable=false` unconditional.
- Test-only `DisposablePointLedgerRestoreExecutor` `09024dce` accepts exact encrypted scope `POINT_LEDGER+POINT_TYPES`, validates source family and archived denomination ID/code, requires complete one-to-one member and currency maps, insists caller-supplied code expectations equal sealed source code and the target family's active point_type code. No numeric-ID guessing. Per-member×currency reconciliation remains; non-MANUAL and non-null source_id still denied, no production API.
- `DisposablePointLedgerCrossDatabasePostgresTest` `a567b4b`: real source catalog + manual +12/-2 ledger in same authenticated file; direct inspector count=1 and no missing/dangling denomination blocker. `9d946ac` proves legacy currency-less archive reports missing category and test-only replay rejects without writes; tampered AES file also denied before writes. Existing foreign-family member, wrong target currency code, double replay, TASK/source_id, source/neighbor ledger balance checks retained.
- QA must rerun prior category sorting CI fix `2539bf9` and newly added obligation replay; newest exact SHA PASS not yet independently observed. Historical QA #01–03 27/27 PASS at `419efb1` does not validate new finance scope. Last known QA red at run 43953986968 / 4088b5e5 had 461 Maven tests and one failure before scope fix.

## Suggested QA batch (JDK21, Docker, 2x PG18, pin SHA)
```powershell
git pull --ff-only origin main
git rev-parse HEAD
.\mvnw.cmd "-Dtest=DisposablePointLedgerCrossDatabasePostgresTest,WorkspacePointLedgerSourceClosurePostgresTest,DisposableGroupLedgerCrossDatabasePostgresTest,DisposableRewardCategoryCrossDatabasePostgresTest,DisposableRewardRequirementCrossDatabasePostgresTest,DisposableRewardObligationCrossDatabasePostgresTest,ProtectiveBackupRewardHistoryClosureTest,WorkspaceRewardObligationArchiveClosurePostgresTest,WorkspaceFinancialRestoreClosureServiceTest,WorkspaceFinancialRestoreClosurePostgresTest" test
git rev-parse HEAD
```

## Acceptance cases
1. CURR-16-01: POINT_TYPES catalog is part of encrypted source archive, exact source owner and currency ID/code present; POINT_LEDGER rows refer to included IDs.
2. CURR-16-02: legacy POINT_LEDGER-only archive emits missing currency category; TEST-only replay rejects before writes; changed ciphertext fails signature/decryption.
3. CURR-16-03: complete immutable MANUAL ledger +12/-2 yields exactly 10 for mapped member×currency, neighbor remains 99, source 10; zero target writes on wrong member/code, duplicate attempt and TASK source_id.
4. CURR-16-04: family mismatch, duplicate code, dangling point_type reference blocked by inspector; untrusted mapping and source archive cannot set VERIFIED.
5. CURR-16-05: 10-class actual Maven counts and pinned pre/post HEAD, failures/errors/skips, prior CI red scope and obligations rollback results returned in Issue #15. Distinguish environment timeout, source failure and stale SHA.

## Important unsolved P0 scope
- This closes **metadata authenticity**, not all historic financial semantics. Source code equality is not enough to prove units/denomination policies if future custom currency config differs. Member identity and historical MANUAL source authenticity still need protected binding; TASK/REWARD_PURCHASE/REFUND/EXCHANGE polymorphic source graph not replayable.
- No general production POINT_TYPES target reconstruction, source finance and group ledger reconciliation, recent owner reauth, physical backup restore proof, 30-day retained AES receipt, verified promotion or atomic FULL/SELECTIVE live reset. `WorkspaceFinancialRestoreClosureService.replaySupported=false`, Reset Execute HTTP423, BETA HOLD.
