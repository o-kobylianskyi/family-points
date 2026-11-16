# BKL002-QA-20261116-04 — Immutable point-ledger polymorphic source closure

**Status:** READY_FOR_QA; **Developer preflight:** NOT_RUN (GitHub connector is source-only; no Java/Docker runner). **Scope:** source-only import preflight and tenant-scoped export hardening; NO production restore. **Producer:** Backend; **QA:** independent acceptance; **Issue:** #15.

## Changes / source anchors
- `ProtectiveBackupImportInspector.java` commit `3ef71369`: encrypted portable inspector adds explicit blockers for `POINT_LEDGER` and `GROUP_POINT_LEDGER` historical source types other than `MANUAL` and present `source_id`. Adds POINT_LEDGER currency rebind/invalid ID reason. Does NOT infer task, purchase, reward or exchange transaction target IDs. `4da1bfed` bounds untrusted source type length in public blocker text.
- `WorkspacePortableBackupExporter.java` commit `b9c003c0`: denies individual point history export when member family and point-type family mismatch, including null currency lookup.
- `WorkspacePointLedgerSourceClosurePostgresTest.java` commit `d5645de`: disposable PG 18 authentic file with TASK + source_id 4242 plus MANUAL debit, inspector must emit `HISTORICAL_SOURCE_REMAP_REQUIRED:POINT_LEDGER:TASK`, `HISTORICAL_SOURCE_ID_REMAP_REQUIRED:POINT_LEDGER`, `CURRENCY_REBIND_REQUIRED` and member rebind blockers, preserve source sum 10, enforce wrong-workspace and foreign currency export denial.

## Independent QA command
```powershell
git pull --ff-only origin main
git rev-parse HEAD
.\mvnw.cmd "-Dtest=WorkspacePointLedgerSourceClosurePostgresTest,WorkspaceGroupLedgerPortableExportPostgresTest,DisposableGroupLedgerCrossDatabasePostgresTest,WorkspaceFinancialRestoreClosureServiceTest,WorkspaceFinancialRestoreClosurePostgresTest,WorkspacePortableRestorePreviewPostgresTest,DisposableCrossDatabasePortableRestorePostgresTest" test
```

## Acceptance and negative cases
- **FIN-SRC-01**: TASK-origin record with positive source_id but absent referenced task entity *does not* imply restorable. Its archive remains authenticated but `restorable=false`; reason explicitly states missing historical source remap.
- **FIN-SRC-02**: manual operation without source_id does not spuriously trigger historical source remap blocker. POINT_LEDGER still requires member and currency binding.
- **FIN-SRC-03**: foreign family inspection and point currency cross-family relation are rejected before unsafe data is accepted/exported. No balance mutation occurs.
- **FIN-SRC-04**: legacy group ledger binary and source-type negative cases remain fail-closed; no production `VERIFIED` or HTTP restore activation.
- **FIN-SRC-05**: preview/test regressions from earlier Release CI (V50/V52 fixture mismatch and Spring CGLIB) must be checked on pinned SHA separately; no stale assumption of all-green actions. Distinguish source fixture defect vs product defect.

## Limitations
- This does NOT implement native replay for `TASK`, `ACCESS_PURCHASE`, `REFUND`, `POINT_EXCHANGE` etc. Their `source_id` links are currently polymorphic and not covered by a real parent-FK mapping. More source schema/owner/currency reconciliation is mandatory.
- Financial `replaySupported=false`; `restorable=false`; no owner fresh proof, target verified snapshot, durable receipt, safe FULL/SELECTIVE apply or executable BKL-002. Existing users/families untouched.
- QA should pin exact HEAD, return PASS/FAIL/ERROR/SKIP + environment and full failure causes to #15 and coordinator. Writing this file does not automatically trigger another role chat.

**Release:** HTTP 423 Reset Execute; BETA HOLD.
