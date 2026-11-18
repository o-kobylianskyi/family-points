# FamilyPoints — Backend C implementation journal

## 2026-11-18 — C-C-00: take ownership and segregate Reset from Restore
- Direction assigned: **Backend C — Security & Reset**.
- Inspected baseline `main` `129d3dca1542a28905934726828f7fae95c1814a`, legacy reset ADR/XTR, revised pre-BETA scope, and Backend B latest handoff.
- Created dedicated handoff `docs/coordination/handoffs/BACKEND_C_LATEST.md`.
- Security review conclusion: never equate restore authorization with reset authorization; original owner identity, fresh server reauth and durable one-use operation-bound approval are mandatory. Financial mutation plans are owned by D.
- **Evidence:** coordination only; implementation NOT STARTED, tests NOT RUN, destructive execution NOT AUTHORIZED.
- **State:** Reset HTTP 423; BETA HOLD.
- **Next:** C-C-01 repo inventory / security baseline; contract ACK with Integrator, then C-C-02 owner and reauth tests.

## 2026-11-18 — C-C-01: live production source audit (completed, no mutation)
- Inspected actual `WorkspaceDataResetPreviewController`, `WorkspaceDataResetPrepareController`, `WorkspaceResetChallengeController`, `WorkspaceResetProofService`, `ResetChallengeAttemptLimiter`, `WorkspaceSecurity`, `CurrentUserService`, `Workspace`, `WorkspaceMember`.
- Security blocker **C-SEC-01:** `canManageWorkspace` permits delegated `MANAGE_WORKSPACE`/`ADMIN_OVERRIDE` but cannot prove original owner. Current reset controllers all use that predicate; fresh reauth only proves current actor. No safe original-owner field observed in inspected workspace/member entities. Require Architecture/Integrator owner-provenance decision before adding destructive authorization.
- Challenge proof lifecycle is more advanced than initial C-C-00 assumption: existing server already has random/hashed secret, TTL, durable consume and epoch binding; avoid redundant implementation or accidental external proof exposure.
- Source audit and explicit 10-group negative-test matrix: `docs/coordination/requests/BACKEND_C_SECURITY_RESET_AUDIT_20261118.md` @ `bd291acb`.
- **Status:** audit COMPLETED, C code NOT MODIFIED, Maven/PG NOT_RUN; Reset Execute HTTP423, BETA HOLD.
- **Next:** get ownership schema/migration ACK, implement isolated `canResetWorkspace` with fail-closed tests, re-check CI exact SHA.

## 2026-11-18 — C-C-02A: original owner provenance analysis and contract
- Inspected `Workspace`, `WorkspaceMember`, `WorkspaceController`, `WorkspaceSecurity`, `CurrentUserService`, `UserAccountService` and `WorkspaceJoinService`. Existing mutable role/privilege is not reliable creator provenance; `WorkspaceController.createWorkspace` blocks direct creation in favor of registration.
- Published explicit migration and original-owner authorization request: `docs/coordination/requests/BACKEND_C_OWNER_PROVENANCE_CONTRACT_20261118.md` @ `daad2432`.
- Plan requires **verified** creator account+member+workspace provenance, no guessed legacy owner backfill; own-workspace and account lifecycle checks, cross-workspace denial and retention of HTTP423. Coordinator must assign registration/migration file ownership first.
- **State:** C-C-02 policy implementation BLOCKED ON CONTRACT ACK, no Java or SQL migrations applied; no tests executed, BETA HOLD unchanged.
- **Next:** once approved, implement owner binding transactionally in registration and standalone reset-only ACL with negative tests; do not infer current manager=original owner.

## 2026-11-18 — C-C-02B: owner identity predicate and negative tests (source committed)
- Added isolated, side-effect-free `security/ResetOriginalOwnerPolicy.java` @ `867018a0`. Owner eligibility requires exact requested workspace, current authenticated enabled account, active/non-deleted member, current account-member link and explicit `REGISTRATION_VERIFIED` / `LEGACY_VERIFIED` binding of account+member+workspace. Unknown/null provenance fails closed. Role/ADMIN_OVERRIDE never substitutes provenance.
- Added `security/ResetOriginalOwnerPolicyTest.java` @ `8dd944ad` with seven JUnit test methods and multiple assertions: positive trusted binding, unknown/missing binding, cross-workspace shared account, delegated manager, member mismatch, disabled/deleted/revoked/unauthed, missing IDs, binding mismatch.
- **Not wired into production controllers**: canonical DB binding and signup/backfill migration have no Architecture ACK. A pure policy is a necessary identity predicate, **not** credential proof, owner-resolution service, approved endpoint auth, migration, or enablement.
- **Evidence:** new source and tests pushed to GitHub main. Tests are **NOT_RUN** (source-only connector; no Maven baseline); do not claim PASS or exact-SHA CI.
- **Safety:** `execute` remains HTTP423, no existing controller/DB/Flyway changed, BETA HOLD.
- **Next:** architecture approves provenance schema; C adds transactional resolver + protected `canResetWorkspace`, wires Reset endpoints and executes unit/PG CI. Meanwhile, C can improve isolated proof abuse/replay negative coverage without enabling execution.

## 2026-11-18 — C-C-03: one-use Reset proof negative test package
- Inspected `WorkspaceResetProofService`, `WorkspaceEpochTransactionGuard` and existing challenge controller test; found present 5-minute proof/epoch, credential fingerprint, account/member and conditional consumed-at protections.
- Committed `WorkspaceResetProofConsumptionSecurityTest` (7 JUnit cases, synthetic Mockito) @ `39a1ece4`: successful consumption only; consumed reuse, expiry, prior epoch, rotated password, wrong actor/member/secret/digest, other family/challenge and lost conditional update. No production business logic changed.
- QA request `docs/coordination/requests/BACKEND_C_QA_PACKET_20261118_PROOF_CONSUMPTION.md` @ `f7723020`; exact-SHA `mvn -Dtest=WorkspaceResetProofConsumptionSecurityTest test` acceptance pending.
- **Evidence:** source commits only, Maven NOT_RUN, PostgreSQL NOT_RUN, no CI green claimed; ownership provenance migration still unapproved.
- **Release:** HTTP 423, BETA HOLD; do not infer full concurrent DB race protection from Mockito tests.
- **Next:** verify QA execution and add isolated PostgreSQL physical concurrency tests after schema/owner provenance sign-off. Can audit rate-limit behavior in parallel.

## 2026-11-18 — C-C-04: persisted proof verifier integrity and reauth budget coverage
- Inspected `WorkspaceResetProofService`, `WorkspaceEpochTransactionGuard`, `ResetChallengeAttemptLimiter` and their current unit tests.
- Production hardening @ `1534d864`: a durable challenge with missing/wrong-typed actor/binding or non-hex/absent 64-char verifier hash now rejects `409 RESET_FRESH_CHALLENGE_REQUIRED` rather than risking NullPointerException/corrupt-state 500. Correct proof path and HTTP423 Execute unmodified.
- Extended synthetic proof tests @ `fbbae497`: absent/malformed verifier, missing account/epoch/member/digest/credential fingerprint. Extended rate limiter tests @ `e133438c`: null count fail closed, SQL error propagates, separate workspace/account keys.
- QA target and exact source SHA instructions: `docs/coordination/requests/BACKEND_C_QA_PACKET_20261118_PROOF_HARDENING.md` @ `5b4336ae`.
- **Evidence:** SOURCE_COMMITTED, Maven/JUnit/PG **NOT_RUN**; no green CI claimed. Backend C ownership/provenance migration still awaiting Architecture sign-off. No real Reset or Restore enabled.
- **Next:** inspect QA output; build reproducible PG18 concurrency test for token consumption and reset reauth throttling on disposable DB. Keep test-only until Integrator approves shared migration/owner contract.
