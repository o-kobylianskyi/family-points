# B-INT-02 — ONE coherent Backend B READY_FOR_QA handoff (2026-11-18)

**Producer:** Backend B, sole backend integration owner. **Status:** `READY_FOR_QA` for the bounded IMPLEMENTED B-INT-02 code and targeted acceptance; **NOT independently QA-ACCEPTED or release eligible.**

**Frozen exact implementation SHA:** `b711371d6c78d77f1550002261ff0b6ac4bd941b` on [draft PR #30](https://github.com/o-kobylianskyi/family-points/pull/30), branch `backend-b/b-int-02-integrated`. Original source integration into main base `08f26c3274e08bf62ba26bd6400f3532062b5392`. This coordination file is docs-only and is NOT part of the pinned implementation SHA. QA must explicitly freeze implementation SHA; do not accept later HEAD by assumption.

## Implemented and independently observable code paths

- Existing signed `FPB1`/AES-GCM original roster and eight-category v2 source `exportWithHistory` preserved, root/epoch/manifest offline proof after source PostgreSQL destruction. `PortableV2SourceClosurePolicy` blocks nonempty unsupported business categories; live `WorkspacePortableMemberFkClosure` inventories physical original-member FKs with explicit INCLUDE/DEPENDENCY/EXCLUDE/BLOCK policy, unknown populated FK BLOCK. Account/session/credential and owner/permit rows are excluded from portable content.
- V53 additive SQL: real new-family original creator binding registered inside same physical transaction as account/member workspace creation; immutable owner binding/trigger; no automatic legacy owner backfill; five-minute RESTORE-only durable permit, separate password failure budget, immutable operation receipt.
- Real server-side current selected owner account/member checks; BCrypt password or Google ID-token (issuer/audience + linked provider subject + fresh issue time), no client-provided reauth boolean. Separate restore password rate limiter. RESTORE scope uses existing C canonical binary hashes tied to verified source snapshot/root, full reviewed injective source→target member map, selected eight-category digest, target epoch and operation.
- Internal `WorkspaceNativeV2RestoreEngine.restore` executes under one PostgreSQL transaction: target-family epoch lock; owner/signed scope and one-use permit consume; preexisting target roster and **owner-reviewed point type IDs bound by code**; immutable source task/reward/instance/purchase/point-ledger ID remap into native PostgreSQL FK tables; polymorphic TASK/PURCHASE ledger source ID remap; exact member×currency financial parity; atomic epoch+1 and immutable receipt. Replays, stale epoch, cross-tenant references and bad mappings fail closed. No HTTP executable endpoint.
- Two-PG18 test `WorkspaceNativeV2RestoreSourceLossPostgresTest`: real independent source and target databases, migration/real encrypted source export, source `stop()`, offline signed inspector, BCrypt credential verification/permit, wrong-password failure, forced rollback (native rows/permit consumption/epoch/receipt), permitted retry+3 net points and duplicate replay denial. Test mocks only authenticated `CurrentUserService` boundary; signed HTTP/JWT/browser/device path NOT tested.
- Original owner PG fixture `WorkspaceRestoreOwnerFoundationPostgresTest`: real owner link/provenance, rollback uniqueness, FK inventory, separate password limiter, receipt trigger.

## Immutable CI evidence — exact implementation SHA

[GitHub Actions #4590](https://github.com/o-kobylianskyi/family-points/actions/runs/44392551759) @ `b711371d6c78d77f1550002261ff0b6ac4bd941b`:
- Java 21 / PostgreSQL Testcontainers: **770 tests, 0 failures, 2 errors, 0 skips; Maven BUILD FAILURE**. The two errors are independent **Retention-owned** `WorkspaceBackupRetentionRollbackGapPostgresTest` cases, both violating `ck_protective_backup_artifact_evidence_complete` via `UPDATE workspace_protective_backups SET storage_key=NULL WHERE snapshot_id=?`.
- B-specific: **two-DB encrypted source loss native restore E2E 1/1 PASS**, original-owner PG suite 2/2 PASS; historical QA-075 PG18 source loss 1/1 PASS, v2 privacy header 3/3 PASS. Prior Spring CGLIB, migration expected counts and testCompile failures corrected in code; no other Java failures on this SHA.
- Web: SUCCESS; Android: SUCCESS; Compose: SUCCESS; scope determination SUCCESS. **Full exact-SHA attestation FAIL** because backend job FAIL. Never present this run as all-four green or release clearance.
- Independent Retention defect routed to Issue #15 comment [6990884639](https://github.com/o-kobylianskyi/family-points/issues/15#issuecomment-6990884639), no retention service/test files edited by B.

## Test execution request — QA owner

Pinned `b711371d6c78d77f1550002261ff0b6ac4bd941b`:
```powershell
./mvnw -Dtest=WorkspaceNativeV2RestoreSourceLossPostgresTest,WorkspaceRestoreOwnerFoundationPostgresTest,WorkspacePortableAncestryV2SourceLossPostgresTest,PortableV2HistoryGraphTest,PortableV2HistorySensitiveHeadersQaTest test
```
Verify actual Docker PG18, Testcontainers, 8 authenticated archive categories, source loss, independently vetted owner account↔member↔family, password mismatch and 5-min one-use permit, parent ID remap, currency/member net sums, repeated operation, injected rollback, another-family mapping, malformed/wrong key/ciphertext, missing dependency and unsupported category denial, immutable receipt, target epoch+1, older v1 read-only inspector and retained disabled HTTP execute. Collect one consolidated defect list with exact SHA, environment, first reproducible stack, F/E/S.

**Full four-component non-skipped exact-SHA QA acceptance remains BLOCKED until Retention's separate failing tests are repaired and the agreed integrated immutable SHA is retested.** If QA observes B-owned P0 defects, send one aggregate list to B for B-INT-02-FIX. Do not ask user for new manual approvals just to run disposable synthetic QA.

## Intentionally unsupported / stop conditions

This is a **bounded** 8-category restore path, not the old v1 23-category full/selective recovery. Nonempty refund/point exchange/reputation/group/media/request/obligation/schedule/other unexported graph remains BLOCKED; no arbitrary partial restore that hides business dependencies. No automatic account recreation, legacy owner inference, recovery case enrollment, or silent identity matching by numeric ID. Existing target task/reward/transaction rows conflict rather than overwrite. Production Restore `restorable=false`, `replaySupported=false`; Reset Execute HTTP423; physical purge OFF; BETA HOLD. No DB migration, deployment, production data write, real family reset or release has been authorized/performed.

**Architecture next action:** review this single handoff, activate QA for B-INT-02 formal acceptance once its schedule permits; separately route Retention 2-error fixture fix. QA owns final all-four same-SHA proof, Architecture/Release own release gating. Backend B on next `++` can pick B-NEXT-01 strictly in nonoverlapping files unless QA returns B-INT-02 P0 issues.
