# BKL002-QA-20261116-08 — Portable REWARD_REQUIREMENTS with explicit dependency closure

**Status:** READY_FOR_QA. **Producer:** Backend. **Consumer:** independent QA. **Priority:** P0. **Environment:** synthetic Testcontainers PostgreSQL only. **Developer Maven/Docker:** NOT_RUN (no JDK runner in GitHub connector). QA must pin pre-/post-run HEAD SHA; log PASS/FAIL/ERROR/SKIP. Earlier QA #01–03 27/27 PASS at 419efb1, unrelated to this packet; #04–07 remain separate gates.

## Delivered source
- Exporter e7572ad: optional V50 `REWARD_REQUIREMENTS` category scoped by source family, encrypts original requirement fields. Before export it checks referenced definition, request and optional task definition belong to the same family. Rejects foreign FK (`BACKUP_REWARD_REQUIREMENT_FOREIGN_RELATION`) or incomplete category graph (`BACKUP_REWARD_REQUIREMENT_PARENT_MISSING`). A selected reward definition/request with outstanding requirements must include `REWARD_REQUIREMENTS`, otherwise `BACKUP_UNEXPORTED_REWARD_REQUIREMENTS`. No source writes.
- Digest whitelist e094643, metadata registry whitelist d6698e1; offline inspector 9009200 checks family_id, exactly one owning definition/request and any optional task reference in corresponding archived category, emitting missing/dangling/invalid blockers. `restorable=false` remains unconditional.
- Financial closure 7ede590 now reports category `REWARD_REQUIREMENTS` (exportable) rather than `REWARD_REQUIREMENTS_UNEXPORTED` and marks native replay unsupported. `REWARD_OBLIGATIONS_UNEXPORTED` and `REWARD_CATEGORY_LINKS_UNEXPORTED` still block; no backward-compatible claims of full restore.
- PostgreSQL 18 acceptance test 687a1df extends obligation test with source reward requirement, encrypted `REWARD_DEFINITIONS+REWARD_REQUIREMENTS` export/inspect, count=1/1; separate `REWARD_REQUIREMENTS`-only export denied. Retains prior omitted-requirements, obligations and category-link negative cases.
- AES inspector fixture 91647d1 covers valid parent, omitted parent, invalid owner and cross-family owner.

## Independent QA
```powershell
git pull --ff-only origin main
git rev-parse HEAD
.\mvnw.cmd "-Dtest=WorkspaceRewardObligationArchiveClosurePostgresTest,ProtectiveBackupRewardHistoryClosureTest,WorkspacePortableArchiveForeignRelationTest,WorkspaceFinancialRestoreClosureServiceTest,WorkspaceFinancialRestoreClosurePostgresTest,WorkspacePointLedgerSourceClosurePostgresTest,WorkspaceGroupLedgerPortableExportPostgresTest,DisposableGroupLedgerCrossDatabasePostgresTest" test
git rev-parse HEAD
```

**Expected cases:**
- REQ-ARC-01: definition+requirements export AES-authenticated with exact persisted row count and source family only.
- REQ-ARC-02: requesting definition WITHOUT its requirements fails closed, while selecting requirements without owning definition/request fails closed.
- REQ-ARC-03: wrong-family requirement row, linked task, definition or request fails before usable archive.
- REQ-ARC-04: request-origin nullable owner one-of check: only one of reward_definition_id/reward_request_id; invalid/missing/dangling selected archive graph blocked.
- REQ-ARC-05: other obligations, blocked categories, reward-category link and financial native replay gaps still block VERIFIED; source purchases, balances and other families remain unchanged.
- REQ-ARC-06: report exact run SHA/test counters and negative details to issue #15, distinguish source regressions from env errors; accept only supported bounded scope.

## Production gate
No native requirement/reward obligation replay, point-ledger or owner reauth proof, no independent restored artifact VERIFIED, no FULL/SELECTIVE execute. HTTP 423, BETA HOLD. No writes to existing family data.
