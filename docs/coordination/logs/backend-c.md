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
