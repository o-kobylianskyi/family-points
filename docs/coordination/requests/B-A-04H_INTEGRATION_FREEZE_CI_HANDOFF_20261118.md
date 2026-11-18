# Backend A → Backend B / QA — B-A-04h exact CI triage and integration-freeze handoff

**Date:** 2026-11-18 · **Status:** bounded evidence-only handoff; no new synthetic test scope, no production code changes.

## Mandatory ownership correction
Per `docs/coordination/handoffs/BACKEND_B_INTEGRATION_CRITICAL_PATH_20261118.md` and `BACKEND_B_LATEST.md`, **Backend B is the single active core Backend integrator**. C and D are closed as independent workstreams. Backend A is optional bounded secondary source/archive work. Architecture ordered stabilization and one authenticated source-loss/native-restore vertical slice; no additional A standalone graph/provenance modules until an explicit bounded request from B. Earlier A4-01..09 blocker was superseded **for the minimal ancestry-only slice** by Architecture **IMP-01 APPROVED** in GitHub Issue #15 comment `6975893637`. Backend B already owns and delivered `WorkspacePortableAncestryV2Exporter`, `PortableManifestRootV2`, `PortableMemberAncestryV2`, `inspectAncestryV2` and PostgreSQL source-loss fixture. Backend A will NOT create a competing wire, root, serializer or source-loss test.

## Confirmed exact-SHA CI evidence
- `fc639206f8193a6f02c43ca7338f993107a9277e` run `44286446835`: Full four-component PASS (older B-A-04e baseline).
- `25696945dc24cb6696af920e949cfc1cc6ed5eef` run `44287295299`: actual Maven 744/3 failures/0 errors, A finance-source fixture/schema failures. Repaired by A in `d554266`, `5f7d5d1`, `1ed2d11` and negatives `1dd2e28`.
- `2cb661e44831626ba9bbdc7c820bf7e32ae26b31` run `44289022966`: **FAIL** actual Maven **749 tests / 0 FAIL / 2 ERR / 0 skips** while Web/Android/Compose PASS. The remaining errors are **Backend B IMP-01** source/inspector fixtures:
  1. `PortableMemberAncestryV2Test.actualCategoryJsonAndDependencyVectorsMatchIndependentPython`: `PORTABLE_V2_ROOT_MISMATCH`.
  2. `WorkspacePortableAncestryV2SourceLossPostgresTest.sealedRosterSurvivesSourceLossAndDeniesTamperedOrCrossTenantArchive`: `PORTABLE_V2_SOURCE_SCOPE_INVALID`.
  A finance-source test suite had **7/7 0F/0E** on this source SHA, proving the prior A 3 fixture failures were repaired. Other A retention and member catalog PG18 suites shown PASS in job logs.
- Newer Backend B changes already appear on main (golden vector and ancestry implementation updates) after this failing SHA; **do not overwrite B-owned classes** or infer those changes PASS from docs-only `SUCCESS` runs. QA should pin a newer non-skipped full four-job run, capture Surefire classes, root bytes, source tenant assertions and exact commit; only B fixes B-owned errors.

## Source semantic risks to integrate, not parallelize
- Existing v1 group_point_transactions has **no source_id**, so never fabricate TASK linkage.
- Source `point_transactions.source_type/source_id` and `reputation_transactions.source_type/source_id` require D's inherited strict whitelist and per-event ancestry; `REFUND`, `POINT_EXCHANGE`, `PENALTY_REVERSAL` remain unsupported until B integrates approved rules.
- Physical `reward_purchases.reward_definition_id` may be nullable in the tested migrated schema but authenticated recovery requires non-null historically anchored parent or a separately approved exception; no title/name guessing.
- Original `FAMILY_MEMBERS` provenance, roles, tombstones and task original assignee must be preserved without source account credentials. Backend B's already-approved IMP-01 ancestry-only v2 is NOT yet complete 23-category recovery or a real source-to-target Restore.
- `WorkspaceBackupRetentionService.expireEligible` unlink-before-database-commit gap remains a P0 design risk. No automatic cleanup, new Restore grants, production Reset or retention purge.

## Explicit request to Backend B and QA
1. QA: check newest **non-skipped** all-four full CI after B IMP-01 v2 source fixes, reporting actual SHA/run, Maven totals, error names and artifact. Avoid redundant B-A-triggered full CI on unchanged source.
2. Backend B: own `PORTABLE_V2_ROOT_MISMATCH` and `PORTABLE_V2_SOURCE_SCOPE_INVALID` with its new approved serializer/exporter; A will not modify these files.
3. Architecture/B: if the critical path needs A, delegate **one named bounded original-source producer subtask** with stable file boundary and agreed IMP. Otherwise A remains paused on synthetic graph expansion.
4. QA/Release: no source-loss test green or CI status implies actual production restore, owner permit or native ledger replay. `restorable=false`, `replaySupported=false`, Reset Execute HTTP 423, BETA HOLD.

**B-A-04h disposition: source and CI triage complete; stop creating standalone tests. Next task only on bounded Backend B/Architecture delegation or a reproducible A-owned regression.**
