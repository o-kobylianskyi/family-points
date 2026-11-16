# BKL002-QA-20261116-05 — Task execution / reward history FK closure

**Status:** READY_FOR_QA. **Backend developer tests:** NOT_RUN (GitHub connector source writes; no Maven/Docker runner). **Scope:** fail-closed export and authenticated offline inspector only, NO production Restore or VERIFIED. **Source baseline:** `fac5cb250`, `94fdb65b`, `c7965809`; tests `71fcdcac`, `fd7829a4`. QA pin exact current main SHA; #15.

## Delivered behavior
- `WorkspacePortableBackupExporter` rejects TASK_EXECUTIONS if the task definition is missing or has a foreign `family_id` relative to its executing member, even though DB FK alone may allow it.
- REWARD_PURCHASES export rejects foreign/missing point type and optional linked reward definition or request (when present). REWARD_REQUESTS export rejects foreign/missing requester, linked definition (when present) or approved point type (when present). Joins are scoped by the selected family and fixed server SQL.
- `ProtectiveBackupImportInspector` reports explicit source-archive blockers for reward purchase request dependency, member identity rebind, purchased/approved currency rebind, and invalid currency IDs. Reward definition FK on request/purchase is legitimately nullable; missing/null must NOT trigger false `INVALID_REFERENCE` or `MISSING_CATEGORY`, while a provided dangling FK must block.
- No source history is rewritten. An encrypted file and import preflight are not independent replay proof; `restorable=false` remains.

## QA command (synthetic fixtures only)
```powershell
git pull --ff-only origin main
git rev-parse HEAD
.\mvnw.cmd "-Dtest=WorkspacePortableArchiveForeignRelationTest,ProtectiveBackupRewardHistoryClosureTest,WorkspacePointLedgerSourceClosurePostgresTest,WorkspaceGroupLedgerPortableExportPostgresTest,DisposableGroupLedgerCrossDatabasePostgresTest,WorkspaceFinancialRestoreClosureServiceTest,WorkspacePortableRestorePreviewPostgresTest" test
```

## Expected cases
- **FIN-FK-01** source TASK execution references foreign-family TaskDefinition -> export fails before encrypted archive write; no mutation.
- **FIN-FK-02** source RewardPurchase references foreign-family RewardDefinition, RewardRequest or currency -> export fails.
- **FIN-FK-03** source RewardRequest references foreign member, definition or approved point type -> export fails.
- **FIN-FK-04** request-origin purchase with `reward_definition_id=null`, valid request present -> no false missing-definition blocker, but explicit member/currency rebind.
- **FIN-FK-05** purchase with `reward_request_id` and no REWARD_REQUESTS in archive -> `MISSING_CATEGORY:REWARD_REQUESTS`; missing currency -> invalid reference; present positive but dangling reward definition -> closure blocker.
- **FIN-FK-06** prior group ledger / point ledger source_type remap, wrong key, wrong tenant, encrypted file auth and cross-db scoped native proof remain intact.

## Known outstanding gaps
- This packet does not export/replay the full reward obligation graph, purchase settlement, or reconstruct individual point/reputation ledger balances.
- No real-family reset, full restore, receipts, 30-day verified backup, privileged owner fresh auth, HTTP execute, or BETA release. QA must independently execute/record PASS/FAIL/SKIP by pinned SHA, consolidate any P0 findings and post to #15.
