# FamilyPoints Backend — rolling continuation handoff

**Last checkpoint:** 2026-11-16, evening CET. **Track:** Backend. **Repository:** o-kobylianskyi/family-points, main. **Purpose:** independently self-contained pickup after chat hard-limit or another agent handoff.

## Release/security policy
- BKL-002 selective/FULL reset/restoration is P0 but **BETA HOLD**. Never enable `reset/execute` (HTTP 423), promote synthetic `VERIFIED`, or mutate real DEV/BETA/PROD family data without architecture, QA, release and owner approval. Always test synthetic disposable PostgreSQL 18.
- Backend works independently under architect's async QA protocol `docs/coordination/requests/BKL002_BACKEND_QA_ASYNC_TEST_PIPELINE_20261116.md`; code/fast tests, QA packets on #15, continue bounded source work. Do not ask user to run every micro-batch.
- No reliable ChatGPT context percentage; use `docs/coordination/chat-capacity/BACKEND.md` and always update handoff. No invented 50/75/85% measurements.

## Latest authoritative independent CI/QA
- Pinned QA accepted 27/27 PASS, zero failures/errors/skips at commit `419efb1fdcab849480e790449c791c86ce004695` for bounded QA packets #01-03 ONLY; newer #04-11 not independently accepted at latest SHA.
- **Important P0 CI regression:** GitHub Actions run `43947907960`, commit `19adfe1b7`, 460 Maven tests / 1 failure / 1 error / 0 skipped. Android/Web/Compose steps PASS, Backend FAIL and exact-SHA full gate FAIL. Architecture issue #15 comment 6928274072.
  1. `WorkspaceRewardObligationArchiveClosurePostgresTest:189` expected one exported reward category but fixture already includes two (`Blocked media` and `Media rewards`). Changed fixture to expect 2 and query independently confirmed source family row count 2: commit `839e9f8`.
  2. `DisposableRewardCategoryCrossDatabasePostgresTest:105` NPE from Java immutable Map.of `containsValue(null)` in `DisposableRewardCategoryRestoreExecutor`. Replaced by safe stream null check; ALSO fixed expected alphabetically sorted inspector category order (`REWARD_CATEGORIES,REWARD_DEFINITION_CATEGORIES,REWARD_DEFINITIONS`) in `e1e4e2b`. Added explicit null-map denial/no-target-mutating regression `9ccfe2e`.
- **These fixes have NOT yet been independently rerun on exact current SHA.** QA must run two targeted classes, surrounding archive/FK/financial closure tests and full Java four-component CI where applicable. No false green.

## Implemented P0 backup/export changes
- V50..V52 portable encrypted archive, registry+lease+staging+retention. Scoped categories include media/bindings, task/reward definitions, requests, purchases, task execution, point/reputation/group ledger, groups, private templates, reward requirements, reward categories, reward-category composite associations, reward obligations and composite obligation blocked categories/rewards.
- Source exporter `src/main/java/com/olehkobylianskyi/familypoints/service/WorkspacePortableBackupExporter.java` and `ProtectiveBackupImportInspector.java` guard tenant family, parent FK graph, missing categories, authenticated AES, content digest, source historical polymorphic `source_id`, manual ledger history, explicit financial currency/member rebind. All production restored state remains unsafe/blocked.
- `WorkspaceFinancialRestoreClosureService` read-only inventories twelve economic categories, with always `replaySupported=false`; no `VERIFIED` promotion. Previous source/test commits chronologically in `docs/coordination/logs/backend.md`.
- Tests `DisposableCrossDatabasePortableRestorePostgresTest`, `DisposableGroupLedgerCrossDatabasePostgresTest`, `DisposableRewardCategoryCrossDatabasePostgresTest`; specialized executors are in `src/test` ONLY. Actual production full restore not implemented.

## Next action priority
1. **First check #15 and current Actions jobs** for new results of patched regressions, ensure two previously red tests PASS. If FAIL, fix; do not weaken isolation or source closure assertions.
2. **Next bounded code package:** test-only independent PostgreSQL native replay of `REWARD_REQUIREMENTS` then `REWARD_OBLIGATIONS` with explicit source→target ID maps for purchase, request, definition, task, member, currency, categories; reject unsupported historical dependency/source IDs. Include atomic rollback and cross-tenant proof; no production API.
3. Package new source+tests+SHA under `docs/coordination/requests/BKL002_QA_PACKET_...`, notify Issue #15, update `docs/coordination/logs/backend.md`, this handoff and capacity log.
4. Release must collect exact-SHA four-component non-skipped CI. QA packets #04-11 can be consolidated; user should run only operator-assisted real tests through QA.
5. Review P0 flow for source graph proof, owner fresh reauth, encrypted KMS/file archive and durable VERIFIED, protected target snapshot, 30-day retention and atomic Reset/Restore. Maintain HTTP423/BETA HOLD.

## Coordination contacts
- Issue #15: https://github.com/o-kobylianskyi/family-points/issues/15
- Architecture: docs/coordination/DISPATCH_20261116_P0_BETA_RECOVERY.md and docs/architecture/ADR-FAMILY-RESET-002.md
- QA: docs/coordination/QA_BKL002_ASYNC_CONTROL_20261116.md
- Temporary chat monitoring: docs/coordination/requests/CHAT_CAPACITY_ALL_TRACKS_20261116.md

**Track state:** backend source changes actively merged to main, last targeted fixes pending current-SHA CI proof. Safe implementation continues; no destructive endpoints enabled.
