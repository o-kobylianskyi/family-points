# FamilyPoints — FINAL Backend C → Backend B Handoff

**Date:** 2026-11-18  
**Status:** **CLOSED / HANDOFF COMPLETE**. This is a read-only ownership transfer and coordination record, not a product release, a deployed DB migration, or permission to execute destructive operations.  
**Decision authority:** project-owner directive [Issue #15 comment 6974473668](https://github.com/o-kobylianskyi/family-points/issues/15#issuecomment-6974473668) supersedes the earlier four-parallel-backend workstream split. **Backend B is the sole primary implementer/integrator**, owning all pending C security/Reset and D financial integration work. Backend C must not receive new runtime/tasks/commits without a new explicit project-owner directive.

## 1. Delivered source and test inventory (preserve; do not rewrite just to reassign ownership)

### Existing RESET security runtime / guards
- `src/main/java/com/olehkobylianskyi/familypoints/service/WorkspaceResetProofService.java`: RESET challenge issuance and mandatory-transaction one-use consumption; hashed verifier, short-lived binding of actor/workspace/operation/digest/data epoch/current credential fingerprint. C hardened fail-closed for corrupt/missing persisted proof metadata in `1534d864`.
- `src/main/java/com/olehkobylianskyi/familypoints/service/ResetChallengeAttemptLimiter.java`: durable per-workspace/account challenge attempt throttling; reject database/limit failures rather than bypassing.
- Existing `WorkspaceDataResetPreviewController`, `WorkspaceDataResetPrepareController`, `WorkspaceResetChallengeController` have historically used permissive `canManageWorkspace` which can include delegated manager/admin. **Neither existing preview/challenge actor auth nor password reauth proves verified original ownership.** All destructive Execute endpoints must stay HTTP 423.
- `ResetOriginalOwnerPolicy` (pure, not wired to actual ownership source): negative policy tests; no currently-authoritative persisted original owner verified by production path.

### C-owned canonical security contracts (pure, no production grant issuance)
- `src/main/java/com/olehkobylianskyi/familypoints/security/WorkspaceDestructivePermitContract.java` — RESET vs RESTORE scopes/action separation, actor/target/operation/snapshot/selection/mapping/epoch/TTL <=5m; overflow rejection.
- `src/main/java/com/olehkobylianskyi/familypoints/security/WorkspaceRestorePermitScopeV2.java` — **authoritative C binary V2 candidate** mapping/scope canonical SHA-256: sourceWorkspaceId, source schemaVersion, signed snapshotId/sourceSnapshotEpoch/snapshotDigest/manifestRootDigest, target family/owner/operation/targetExpectedEpoch/TTL and selection/memberMap digests.
- `src/main/java/com/olehkobylianskyi/familypoints/security/WorkspaceRestoreSelectionClosureV2.java` — typed sorted category RESTORE/PRESERVE/BLOCK, source row count, category content/dependency/conflict SHA-256, source/target tenant IDs and archive schema version.
- `src/main/java/com/olehkobylianskyi/familypoints/security/OriginalOwnerEnrollmentEvidence.java` — immutable **review-only** candidate of authenticated new-family creator event vs independently audited legacy account-recovery evidence; exact account/workspace/member, active linked membership and freshness; **does not authenticate evidence or write owner binding**.
- Golden-vector tests `WorkspaceRestorePermitCanonicalVectorsTest` and pure security tests `WorkspaceRestorePermitScopeV2Test`, `WorkspaceRestoreSelectionClosureV2Test`, `WorkspaceDestructivePermitContractTest`, `OriginalOwnerEnrollmentEvidenceTest`.
- **Never treat a validated client-supplied descriptor, archive membership, admin role or typed text as owner proof.**

### Disposable PostgreSQL 18 test-only security components
- `src/test/java/com/olehkobylianskyi/familypoints/security/DisposableOwnerPermitStore.java` — QA-only `qa_c_owner_permits` / `qa_c_owner_permit_receipts`; 256-bit secret hashed, action-specific 5min grant, lock `families FOR UPDATE` then permit, consume + epoch bump + receipt atomically in test DB. Issuer is **synthetic and NOT authenticated**.
- `DisposableOwnerPermitStorePostgresTest` — six cases: one-use, rollback/retry, wrong action/tenant, owner/secret/epoch, duplicate+expiry, two-thread concurrency.
- `DisposableOriginalOwnerBindingPostgresTest` — QA-only provenance table; originally six cases, later two for malformed cross-family binding and competing enrollment race. **No production Flyway migration or true evidence verification**.
- `WorkspaceResetProofConcurrencyPostgresTest` — six physical PG18 cases: double use exactly once, rollback/retry, wrong workspace, stale epoch, expiry, password rotation.
- `WorkspaceResetProofConsumptionSecurityTest` and `ResetChallengeAttemptLimiterTest` — synthetic negative/unit coverage, including corrupt proof metadata and limiter DB failure.

## 2. Consumer compatibility — Backend B already adopting C contract

- B's review `docs/coordination/requests/BACKEND_B_TO_C_C10_BINARY_V2_ACK_20261118.md` is **CONDITIONAL field-level ACK** of C binary V2 as the sole proposed common RESTORE format; it rejects B-B-07's older pipe-delimited `FP-RESTORE-*-V2` text hashes for C grants.
- B's `DisposableRestoreScopeV2Consumer` calls C's actual canonical mapping/selection/scope methods; B's `DisposableRestoreScopeV2ConsumerTest` pins C's three fixed golden vectors (commit `590040d2`). B-B-08 source graph/consumer tests exist, but inputs from A are still caller-supplied rather than authenticated sealed archive proof.
- **Do not run two competing digest implementations** or silently convert old B test-only strings. Keep exact typed binary encoding documented in `docs/coordination/requests/BACKEND_C_C09_CONTRACT_V2_B_ACK_20261118.md` and `BACKEND_C_C10_V2_INTEROPERABILITY_GATE_20261118.md`.
- `VNext-2` in synthetic vector is a **test marker**, not an approved production archive schema version. Final source manifest + D financial closure + reviewed pair evidence + receipt semantics remain a single Architecture IMP responsibility now executed by B.

## 3. Remaining production blockers — all transferred to Backend B

| Priority | Gap / required deliverable | Owner after handoff |
|---|---|---|
| P0 | Authoritative **immutable original owner** binding; transactionally record authenticated creator + exact family-member/account link on new-family registration. Legacy families remain OWNER_NOT_ESTABLISHED until independently audited evidence; **never infer owner** from admin, oldest member, email/name or source archive. Composite cross-tenant constraints, immutable provenance and explicit audited transfer protocol. | **Backend B** |
| P0 | Genuine fresh owner credential verification, including provider-only account flow or explicit fail-closed denial; hardened persistent failed-attempt controls. Avoid JWT age, role or password field as substitute. | **Backend B** |
| P0 | Production durable **RESET vs RESTORE** grant storage, authenticated issuance, hashed 256-bit token/nonce, exact source/target/action/operation/owner/reviewed mapping/selection/snapshot/epoch scope, <=5min server-clock expiry, replay denial, immutable audit and atomic consume. **No production service or Flyway yet.** | **Backend B** |
| P0 | Adopt **one C binary V2** SHA contract; final authenticated A signed manifest + source schemaVersion, source tenant/epoch, historical FAMILY_MEMBERS/TaskInstance/ledger parents and category/closure digests; actual verified owner-reviewed injective participant mapping (hash alone does not prove review). | **Backend B**, consumes A minimal producer |
| P0 | Physical PostgreSQL order target family/epoch row lock → permit/operation lock → approved target category rows → epoch bump + receipt, **same transaction**. Injected rollback restores grant and all financial/FK data. Same-op retry after lost response uses **GET immutable receipt**, not a new execution. | **Backend B** |
| P0 | Integration into protected Reset/Restore service and proper authorization boundary (not delegated manager), Reset financial dependency/D invariants and stale offline epoch checks. **No execute unlock before complete QA and release approval.** | **Backend B** |
| P0 | One reviewed shared Architecture **IMP** for Flyway/DTO/crypto/epoch/lock/wire changes; identify real owner registration writer and independently verified legacy enrollment workflow. No guessed migration/backfill. | Architecture coordinates; **B implements** |

**Do not create new C stream or a second test-only framework.** Integrate the tested C pieces in one vertical slice and close actual production gaps.

## 4. Immutable QA/CI evidence, not implied current HEAD PASS

- **Fully verified earlier four-component run:** [#44275375375](https://github.com/o-kobylianskyi/family-points/actions/runs/44275375375) at immutable SHA `ab77611b866f4757ab579df5778e738e1a2bc685`, Java Maven/PostgreSQL **702/702 PASS, 0 failures, 0 errors, 0 skipped**; Web/Android/Compose and full exact-SHA attestation/outcome PASS. Actual test logs show C owner permit 6/6, Reset proof PG 6/6, B V2 consumer 5/5. This does **not** certify subsequent C-C-11 enrollment code/tests, B-B-08 or moving HEAD.
- Earlier [#44274406405](https://github.com/o-kobylianskyi/family-points/actions/runs/44274406405): C permit PG18 6/6, owner-binding PG18 6/6, proof PG18 6/6, C V2 scope 7/7, selection closure 5/5 — **30/30 class-level PASS**; full Maven 684 tests with 1 unrelated A/QA fixture error, so full gate FAILED on that SHA. Later QA fixed that JDBC placeholder and confirmed PG race suite PASS in #44275375375.
- **Additional C-C-11** `OriginalOwnerEnrollmentEvidenceTest` 8 methods and owner-binding PG18 two new methods were committed **after** the 702 baseline; exact-new-SHA independent result has not been verified here.
- QA/Release must pin a **new single immutable SHA** and run non-skipped Maven/JDK21/PG18, Web, Android, Compose, Release safety + attestation, then authorized *disposable* archive/source-loss/owner/E2E and V49 browser/device. Green scoped jobs with heavy steps skipped do **not** qualify.

## 5. Canonical references and safety

- Owner directive: [Issue #15 comment 6974473668](https://github.com/o-kobylianskyi/family-points/issues/15#issuecomment-6974473668).
- Architecture policy (product accepted, historic owner assignments superseded by directive): `docs/architecture/ADR-BKL002-ABCD-INTEGRATION-003.md`.
- C provenance contract: `docs/coordination/requests/BACKEND_C_OWNER_PROVENANCE_CONTRACT_20261118.md`.
- C owner/permit production IMP draft: `docs/coordination/requests/BACKEND_C_C08_IMPLEMENTATION_IMPACT_20261118.md`.
- C→B v2 contract + vectors: `docs/coordination/requests/BACKEND_C_C09_CONTRACT_V2_B_ACK_20261118.md`, `BACKEND_C_C10_V2_INTEROPERABILITY_GATE_20261118.md`.
- B conditional ACK: `docs/coordination/requests/BACKEND_B_TO_C_C10_BINARY_V2_ACK_20261118.md`.
- Last C QA/owner evidence: `docs/coordination/requests/BACKEND_C_C11_OWNER_ENROLLMENT_QA_AND_B_V2_ACK_20261118.md`.
- Full C journal: `docs/coordination/logs/backend-c.md`.
- **Runtime stays locked:** production `restorable=false`, `replaySupported=false`, RESET Execute **HTTP 423**, **BETA HOLD**. No actual family Reset, Restore, source deletion, grant release, migration or deployment is authorized.

**Final C disposition:** all C-C-01…C-C-11 source, test and design artifacts remain in GitHub; C is administratively CLOSED. Backend B now owns implementation, defects and integration. No further C commits, tickets or independent ACKs are required; future work and approvals route through Backend B, Architecture, QA and Release.
