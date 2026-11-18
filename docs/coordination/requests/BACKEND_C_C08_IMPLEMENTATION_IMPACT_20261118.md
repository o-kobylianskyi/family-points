# IMP proposal — Backend C original owner + action-scoped permit production integration

**Date:** 2026-11-18. **Status:** PROPOSED / NOT MERGED INTO PRODUCTION. **Policy:** ADR-BKL002-ABCD-INTEGRATION-003 ACCEPTED. **Implementers:** C security, B RESTORE consumer, Architect schema/registration/Release coordination, QA PG tests.

## Existing work does NOT suffice
- `ResetOriginalOwnerPolicy` (pure) cannot determine true owner without authenticated immutable persisted provenance.
- `WorkspaceDestructivePermitContract.Scope` is a validated descriptor, not an authorization token, durable grant, operator identity proof or receipt.
- `WorkspaceResetProofService` is existing RESET password proof; its challenge secret is deliberately not available through HTTP, and the challenge cannot be safely reused as RESTORE permission.
- C-C-08 `DisposableOwnerPermitStore` + `qa_c_original_owner_binding` are strictly **src/test** and PG18 synthetic, not deployable owner enrollment.

## Schema proposal for Architecture/Backend B review, not Flyway yet

### 1. Verified original owner
- Durable per-workspace original creator account and member binding, provenance `REGISTRATION_VERIFIED` or audited `LEGACY_VERIFIED`, evidence reference, creation timestamp. Unknown legacy workspaces MUST remain unverified with Reset/Restore denied.
- Original provenance immutable. If later owner transfer is supported, retain initial creator identity and add separately audited active-owner authorization; never silently overwrite origin.
- DB constraints and exact joins account ↔ workspace member ↔ family; current account enabled, member active/not deleted, linked membership.
- Registration path must save binding atomically with new family and authenticated account link. Legacy enrollment from independent verified audit evidence only.

### 2. Action-scoped one-time grant
- Cryptographically strong 256-bit random bearer secret, only hash/keyed verifier in durable store; one-use constraint and expiry <=5m.
- Target family, action RESET|RESTORE, operation UUID, owner account/member, sealed snapshot UUID/hash, exact selected scope digest, RESTORE-only canonical reviewed mapping digest, epoch, issued/expires/consumed/audit; include source archive schemaVersion + sourceWorkspaceId + sealed manifest if B ACK.
- FK/uniqueness: target `family` + `action` + `operation`, bound immutable payload and no action transposition; prevent attempts for unverified owner.
- Single transaction consume + B domain replay or C reset + epoch + receipt; target family lock first. Expired/consumed/stale/cross-family/cross-action denied; replay GET result uses existing receipt, not replaying writes.

### 3. API / security lifecycle
- Owner verification endpoint must reauth with real account/password or verified recent provider challenge; an archive signature or JWT/role is not fresh reauth.
- Explicit typed confirmation distinct from credential, exact counts/digest and snapshot version.
- Never display ready/execute if backup is unverified or any financial/category dependency incomplete.
- No production endpoint or Flyway schema from the test-only C-C-08 batch.

## Required ACKs and handoff

- **Architecture / Integrator:** assign concrete Flyway version, owner enrollment source file ownership and audit/transfer rules; register cross-consumer IMP.
- **Backend B:** approve/revise `docs/coordination/requests/BACKEND_C_TO_B_DURABLE_PERMIT_CONTRACT_20261118.md` and specify exact canonical digest fields, lock+receipt semantics. B does not mint authentic C owner permits.
- **Backend A:** authenticated archive source snapshot/version/hash inputs needed by C permit.
- **Backend D:** accepted financial closure version/count hash and Reset reconciliation constraints.
- **QA:** run C-C-08 two-PG18 new tests and the earlier C proof+policy tests exact SHA; verify concurrency, rollback, stale token, shared account and deleted member; no real family write.
- **Release:** BETA HOLD, Reset Execute HTTP 423 until multi-component non-skipped CI and real permitted disposable acceptance.

## Safety

This IMP is a review request, NOT an approval to run DB migration, alter any existing family, issue real owner proof, or permit Reset/Restore. Synthetic PG tests cannot stand in for authenticated source-loss disaster recovery.
