# Backend C → QA / Architecture / Backend B — C-C-08 large synthetic security batch

**Date:** 2026-11-18. **Scope:** TEST-ONLY, **no production Flyway**, no Reset/Restore HTTP execute enabled.
**Immutable source commits:**
- `5903f595d0086aa3c9b9b73e5ad94b1f570d68f3` — `security/DisposableOwnerPermitStore.java` **in src/test only**.
- `f824738e687647eb945e58b2e9cb53e0aeefefd2` — `DisposableOwnerPermitStorePostgresTest.java`.
- `926012f00d53ce4890838f89a633ed0e2d20ba72` — `DisposableOriginalOwnerBindingPostgresTest.java`.

## Purpose

Demonstrate, on migrated disposable PostgreSQL 18, what the approved owner/permit invariant would look like **without** quietly adding a new production schema or claiming a true authorized owner.

### QA-only permit table / lifecycle

Temporary `qa_c_owner_permits` and `qa_c_owner_permit_receipts` created AFTER Flyway only in Testcontainers:
- `RESET` and `RESTORE` action-distinct unique `workspace_id + action + operation_id`, never interchangeable; SHA-256 verifier of 256-bit random secret; positive TTL <=5m; map hash mandatory only for RESTORE.
- Immutable scope binds original account/member ID, destination family, operation UUID, snapshot UUID + digest, selected scope digest, optional RESTORE member-map digest, dataEpoch.
- `SELECT data_epoch FROM families WHERE id=? FOR UPDATE` **first**. Then exact permit `FOR UPDATE`; compare stored vs expected, DB server clock TTL, hashed secret; update used-at; update family epoch; create QA receipt in ONE JDBC transaction.
- Synthetic rollback after consumption rolls back token and epoch; new helper can retry. Concurrent consumers: exactly one commits. Cross-action, other-family, wrong identity, wrong secret, stale epoch and duplicate operation fail closed.
- No claims of signing / verifying initial identity. `issueSynthetic` is **not a production owner reauth entrypoint**, and QA receipts are not actual restored domain data.

### QA-only original-owner identity table

`qa_c_original_owner_binding` requires explicit `REGISTRATION_VERIFIED` or `LEGACY_VERIFIED`; no guessed `INFERRED_ADMIN`. SQL joins enabled account, active/nondeleted family member and exact account-family-member membership, then evaluates `ResetOriginalOwnerPolicy`.
Six PG18 methods: absent legacy binding denied; verified owner vs delegated manager; disabled account/soft-deleted member; revoked link; duplicate binding/invalid provenance rejected; same global account linked to another family cannot inherit original ownership. Production registration and legacy evidence creation are **not implemented**.

## Exact test execution

```powershell
git pull --ff-only origin main
git rev-parse HEAD
./mvnw -Dtest=DisposableOwnerPermitStorePostgresTest,DisposableOriginalOwnerBindingPostgresTest,WorkspaceDestructivePermitContractTest,ResetOriginalOwnerPolicyTest,WorkspaceResetProofConcurrencyPostgresTest,WorkspaceResetProofConsumptionSecurityTest,ResetChallengeAttemptLimiterTest test
```

Use `mvn` if wrapper absent. Docker must be available. Publish exact SHA, counts (tests/failures/errors/skips), full Maven result and Surefire failure stack; never treat code review or historical green as current PASS. Full [full-ci] four-component non-skipped follow-up is required.

## Required technical approvals before production implementation

1. **Architecture/Integrator IMP** for owner-binding table+legacy enrollment and shared `RESET`/`RESTORE` permit schema; no arbitrary Flyway numbering before owners acknowledge.
2. **Backend B explicit ACK/REVISE:** action/operation/target workspace/snapshot/digest/canonical mapping hash, archive `sourceWorkspaceId`/schema version, target family -> permit row -> domain rows lock order, atomic consume and durable idempotent receipt.
3. **Registration owner:** actual signed-in account verified at initial+additional-family creation in same transaction, no guessed admin backfill.
4. **Google-only fresh reauth** independent legitimate provider or fail closed; local password path tests required.
5. **Reset/Restore release:** financial DAG, signed A portable member ancestry, D reconciliation, B mapping/replay, audit, offline epoch and QA/Release formal approval.

## CI and known independent failure

GitHub Actions [#44268695217](https://github.com/o-kobylianskyi/family-points/actions/runs/44268695217) completed Maven **572 run, 1 failure, 1 error, 0 skipped**, both Backend B synthetic `DisposableFinancialTargetRehearsalPostgresTest` duplicate-key failures. This predates this C-C-08 test batch. Later moving main may include B fix: recheck exact SHA and do not patch B test from C.

**Invariant:** Reset Execute HTTP 423; RESTORE production disabled; BETA HOLD; zero real family data operations.
